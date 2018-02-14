package io.github.satr.idea.plugin.connector.la.entities;
// Copyright © 2018, github.com/satr, MIT License

import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.Runtime;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult;

public class FunctionEntry {
    private final String functionName;
    private final Runtime runtime;
    private String handler;

    public FunctionEntry(FunctionConfiguration configuration) {
        functionName = configuration.getFunctionName();
        runtime = Runtime.fromValue(configuration.getRuntime());
        handler = configuration.getHandler();
    }

    public FunctionEntry(UpdateFunctionCodeResult updateFunctionCodeResult) {
        functionName = updateFunctionCodeResult.getFunctionName();
        runtime = Runtime.fromValue(updateFunctionCodeResult.getRuntime());
        handler = updateFunctionCodeResult.getHandler();
    }

    public String getFunctionName() {
        return functionName;
    }

    public Runtime getRuntime() {
        return runtime;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", getFunctionName(), getRuntime().toString());
    }
}
