package io.github.satr.common;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class JsonHelper {
    private final ScriptEngineManager manager = new ScriptEngineManager();
    private final ScriptEngine scriptEngine = manager.getEngineByName("JavaScript");
    private final String SCRIPT_PARAMS = "result = JSON.stringify(JSON.parse(jsonString), null, 2)";
    private final String VALUE_FORMAT = "jsonString";
    private final String RESULT_KEY = "result";

    public OperationValueResult<String> Reformat(String jsonText) {
        OperationValueResultImpl<String> result = new OperationValueResultImpl<>();
        scriptEngine.put(VALUE_FORMAT, jsonText);
        try {
            scriptEngine.eval(SCRIPT_PARAMS);
            result.setValue((String)scriptEngine.get(RESULT_KEY));
        } catch (ScriptException e) {
            e.printStackTrace();
            result.addError(e.getMessage());
        }
        return result;
    }
}
