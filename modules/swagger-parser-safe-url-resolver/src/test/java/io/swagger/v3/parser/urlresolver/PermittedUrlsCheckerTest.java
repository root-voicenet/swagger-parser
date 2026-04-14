package io.swagger.v3.parser.urlresolver;

import io.swagger.v3.parser.urlresolver.exceptions.HostDeniedException;
import io.swagger.v3.parser.urlresolver.models.ResolvedUrl;
import io.swagger.v3.parser.urlresolver.utils.NetUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PermittedUrlsCheckerTest {

    private final List<String> emptyAllowlist = Collections.emptyList();
    private final List<String> emptyDenylist = Collections.emptyList();

    private StubPermittedUrlsChecker checker;

    @BeforeMethod
    void beforeMethod() {
        this.checker = new StubPermittedUrlsChecker(emptyAllowlist, emptyDenylist);
    }

    @Test(expectedExceptions = HostDeniedException.class, expectedExceptionsMessageRegExp = ".*IP is restricted.*")
    public void shouldRejectPrivateSIITIPv4in6HostReferencesInABCDFormat() throws Exception {
        String url = "https://[0:0:0:0:0:ffff:10.1.33.147]:8000/v1/operation?theThing=something";
        checker.map(url, "10.1.33.147", "10.1.33.147", url);
        checker.verify(url);
    }

    @Test
    public void shouldAllowPublicSIITIPv4in6HostReferencesInABCDFormat() throws Exception {
        String url = "https://[0:0:0:0:0:ffff:1.2.3.4]:8000/v1/operation?theThing=something";
        String expectedUrl = "https://1.2.3.4:8000/v1/operation?theThing=something";
        String expectedHostHeader = "0:0:0:0:0:ffff:1.2.3.4";

        checker.map(url, expectedHostHeader, "1.2.3.4", expectedUrl);
        ResolvedUrl result = checker.verify(url);

        Assert.assertEquals(result.getUrl(), expectedUrl);
        Assert.assertEquals(result.getHostHeader(), expectedHostHeader);
    }

    @Test(expectedExceptions = HostDeniedException.class, expectedExceptionsMessageRegExp = ".*IP is restricted.*")
    public void shouldRejectPrivateSIITIPv4in6HostReferencesInIPv6Format() throws Exception {
        String url = "https://[0:0:0:0:0:ffff:a01:219]:8000/v1/operation?theThing=something";
        checker.map(url, "10.1.2.25", "10.1.2.25", url);
        checker.verify(url);
    }

    @Test(expectedExceptions = HostDeniedException.class, expectedExceptionsMessageRegExp = ".*IP is restricted.*")
    public void shouldRejectNAT64HostReferences() throws Exception {
        String url = "https://[64:ff9b::]:8000/v1/operation?theThing=something";
        String ip = "64:ff9b:0:0:0:0:0:0";
        checker.map(url, ip, ip, url);
        checker.markNat64(ip);
        checker.verify(url);
    }

    @Test(expectedExceptions = HostDeniedException.class, expectedExceptionsMessageRegExp = ".*IP is restricted.*")
    public void shouldRejectDecimalIPsThatResolveToLocalIPs() throws Exception {
        String url = "https://3232235778:8000/api/v3/pet/findByStatus?status=available";
        checker.map(url, "192.168.1.2", "192.168.1.2", url);
        checker.verify(url);
    }

    @Test
    public void shouldPreferAllowlistOverEverythingElse() throws Exception {
        String url = "https://localhost:3000/1";
        List<String> allowlist = Collections.singletonList("localhost");
        this.checker = new StubPermittedUrlsChecker(allowlist, emptyDenylist);
        checker.mapHost(url, "localhost");

        ResolvedUrl result = checker.verify(url);
        Assert.assertEquals(result.getUrl(), "https://localhost:3000/1");
        Assert.assertEquals(result.getHostHeader(), "localhost");
    }

    @Test
    public void shouldAllowPublicDomainsByDefault() throws Exception {
        String url = "https://smartbear.com:3000/1";
        String expectedUrl = "https://1.2.3.4:3000/1";
        checker.map(url, "smartbear.com", "1.2.3.4", expectedUrl);

        ResolvedUrl result = checker.verify(url);
        Assert.assertEquals(result.getUrl(), expectedUrl);
        Assert.assertEquals(result.getHostHeader(), "smartbear.com");
    }

    @Test
    public void shouldAllowPublicIPsByDefault() throws Exception {
        String url = "https://1.2.3.4:3000/1";
        checker.map(url, "1.2.3.4", "1.2.3.4", url);

        ResolvedUrl result = checker.verify(url);
        Assert.assertEquals(result.getUrl(), url);
        Assert.assertEquals(result.getHostHeader(), "1.2.3.4");
    }

    @Test(
            dataProvider = "shouldBlockRestrictedIPv4sByDefault",
            expectedExceptions = HostDeniedException.class,
            expectedExceptionsMessageRegExp = ".*IP is restricted.*"
    )
    public void shouldBlockIPv4Localhost(String url, String expectedIp) throws Exception {
        checker.map(url, expectedIp, expectedIp, url);
        checker.verify(url);
    }

    @DataProvider(name = "shouldBlockRestrictedIPv4sByDefault")
    private Object[][] shouldBlockRestrictedIPv4sByDefault() {
        return new Object[][]{
                {"https://localhost:3000/1", "127.0.0.1"},
                {"https://127.0.0.1/", "127.0.0.1"},
                {"https://192.168.1.2/", "192.168.1.2"},
                {"https://127.3", "127.0.0.3"}
        };
    }

    @Test(
            dataProvider = "shouldBlockRestrictedIPv6sByDefault",
            expectedExceptions = HostDeniedException.class,
            expectedExceptionsMessageRegExp = ".*IP is restricted.*"
    )
    public void shouldBlockIPv6Localhost(String url, String expectedIp) throws Exception {
        checker.map(url, expectedIp, expectedIp, url);
        checker.markUniqueLocal(expectedIp);
        checker.verify(url);
    }

    @DataProvider(name = "shouldBlockRestrictedIPv6sByDefault")
    private Object[][] shouldBlockRestrictedIPv6sByDefault() {
        return new Object[][]{
                {"https://[fc00::1]/", "fc00:0:0:0:0:0:0:1"},
                {"https://[fd00:ec2::254]/", "fd00:ec2:0:0:0:0:0:254"}
        };
    }

    @Test(expectedExceptions = HostDeniedException.class, expectedExceptionsMessageRegExp = ".*IP is restricted.*")
    public void shouldBlockDomainNamesThatResolveToPrivateIPs() throws Exception {
        String url = "https://evil.com";
        checker.map(url, "evil.com", "192.168.1.1", "https://192.168.1.1:3000/1");
        checker.verify(url);
    }

    @Test(expectedExceptions = HostDeniedException.class, expectedExceptionsMessageRegExp = ".*URL is part of the explicit denylist.*")
    public void shouldBlockSpecificallyDenylistedURLs() throws Exception {
        String url = "https://smartbear.com";
        List<String> denylist = Collections.singletonList("smartbear.com");
        this.checker = new StubPermittedUrlsChecker(emptyAllowlist, denylist);
        checker.verify(url);
    }

    @Test(expectedExceptions = HostDeniedException.class, expectedExceptionsMessageRegExp = ".*IP is part of the explicit denylist.*")
    public void shouldBlockBasedOnResolvedIP() throws Exception {
        String url = "https://smartbear.com";
        List<String> denylist = Collections.singletonList("1.2.3.4");
        this.checker = new StubPermittedUrlsChecker(emptyAllowlist, denylist);
        checker.map(url, "smartbear.com", "1.2.3.4", "https://1.2.3.4:3000/1");
        checker.verify(url);
    }

    @Test(expectedExceptions = HostDeniedException.class, expectedExceptionsMessageRegExp = ".*URL is part of the explicit denylist.*")
    public void shouldBlockURLMatchingWildcardPattern() throws Exception {
        String url = "https://foo.example.com";
        List<String> denylist = Collections.singletonList("f*.example.com");
        this.checker = new StubPermittedUrlsChecker(emptyAllowlist, denylist);
        checker.mapHost(url, "foo.example.com");
        checker.verify(url);
    }

    @Test
    public void shouldAllowURLMatchingWildcardPattern() throws Exception {
        String url = "https://foo.example.com";
        List<String> allowlist = Collections.singletonList("f*.example.com");
        this.checker = new StubPermittedUrlsChecker(allowlist, emptyDenylist);
        checker.mapHost(url, "foo.example.com");
        checker.verify(url);
    }

    private static class StubPermittedUrlsChecker extends PermittedUrlsChecker {
        private final Map<String, String> urlHosts = new HashMap<>();
        private final Map<String, InetAddress> hostToAddress = new HashMap<>();
        private final Map<String, String> rewritten = new HashMap<>();
        private final Set<String> uniqueLocalIps = new HashSet<>();
        private final Set<String> nat64Ips = new HashSet<>();

        StubPermittedUrlsChecker(List<String> allowlist, List<String> denylist) {
            super(allowlist, denylist);
        }

        void mapHost(String url, String host) {
            urlHosts.put(url, host);
        }

        void map(String url, String host, String resolvedIp, String rewrittenUrl) throws UnknownHostException {
            mapHost(url, host);
            hostToAddress.put(host, InetAddress.getByName(resolvedIp));
            rewritten.put(key(url, resolvedIp), rewrittenUrl);
        }

        void markUniqueLocal(String ip) {
            uniqueLocalIps.add(ip);
        }

        void markNat64(String ip) {
            nat64Ips.add(ip);
        }

        @Override
        protected String getHostFromUrl(String url) throws MalformedURLException {
            if (urlHosts.containsKey(url)) {
                return urlHosts.get(url);
            }
            return NetUtils.getHostFromUrl(url);
        }

        @Override
        protected InetAddress getHostByName(String hostname) throws UnknownHostException {
            if (hostToAddress.containsKey(hostname)) {
                return hostToAddress.get(hostname);
            }
            return NetUtils.getHostByName(hostname);
        }

        @Override
        protected String setHost(String url, String host) throws MalformedURLException {
            String mapped = rewritten.get(key(url, host));
            if (mapped != null) {
                return mapped;
            }
            return NetUtils.setHost(url, host);
        }

        @Override
        protected boolean isUniqueLocalAddress(InetAddress ip) {
            return uniqueLocalIps.contains(ip.getHostAddress()) || super.isUniqueLocalAddress(ip);
        }

        @Override
        protected boolean isNAT64Address(InetAddress ip) {
            return nat64Ips.contains(ip.getHostAddress()) || super.isNAT64Address(ip);
        }

        private String key(String url, String hostOrIp) {
            return url + "|" + hostOrIp;
        }
    }
}
