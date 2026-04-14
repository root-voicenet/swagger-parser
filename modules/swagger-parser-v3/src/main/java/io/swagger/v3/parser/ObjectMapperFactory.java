package io.swagger.v3.parser;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.dataformat.yaml.YAMLFactory;

public class ObjectMapperFactory {
    public static ObjectMapper createJson() {
        return createJson(true, true);
    }

    protected static ObjectMapper createJson(boolean includePathDeserializer, boolean includeResponseDeserializer) {
        return create(createJsonFactory(), includePathDeserializer, includeResponseDeserializer);
    }

    public static ObjectMapper createYaml() {
        return createYaml(true, true);
    }

    protected static ObjectMapper createYaml(boolean includePathDeserializer, boolean includeResponseDeserializer) {
        return create(createYamlFactory(), includePathDeserializer, includeResponseDeserializer);
    }

    private static ObjectMapper create(TokenStreamFactory tokenStreamFactory, boolean includePathDeserializer, boolean includeResponseDeserializer) {
        return new ObjectMapper(tokenStreamFactory)
          .rebuild()
          .configureForJackson2()
          .changeDefaultPropertyInclusion(ignored -> JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
          .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
          .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .build();
    }

    private static TokenStreamFactory createJsonFactory() {
        return new JsonFactoryBuilder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .build();
    }

    private static TokenStreamFactory createYamlFactory() {
        return YAMLFactory.builder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .build();
    }
}
