package io.swagger.v3.parser;

import tools.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class ObjectMapperTest {
    @Test
    public void testCreateJsonMapper() {
        ObjectMapper mapper = ObjectMapperFactory.createJson();
        Assert.assertNotNull("ObjectMapper should be created", mapper);
    }
}
