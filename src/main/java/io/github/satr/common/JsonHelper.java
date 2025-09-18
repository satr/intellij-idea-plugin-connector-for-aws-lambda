package io.github.satr.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.core.JsonProcessingException;

public class JsonHelper {
    private static final ObjectWriter PRETTY_WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();

    public OperationValueResult<String> Reformat(String jsonText) {
        OperationValueResultImpl<String> result = new OperationValueResultImpl<>();
        try {
            Object json = new ObjectMapper().readValue(jsonText, Object.class);
            result.setValue(PRETTY_WRITER.writeValueAsString(json));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            result.addError(e.getMessage());
        }
        return result;
    }
}
