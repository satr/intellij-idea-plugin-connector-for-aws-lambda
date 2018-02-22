package io.github.satr.idea.plugin.connector.la.entities;
// Copyright © 2018, github.com/satr, MIT License

import com.amazonaws.services.lambda.model.*;
import com.amazonaws.services.lambda.model.Runtime;

public class FunctionEntry {
    private final String functionName;
    private final Runtime runtime;
    private String handler;
    private String description;
    private String functionArn;
    private String lastModified;
    private String role;
    private Integer timeout;
    private Integer memorySize;
    private TracingMode tracingConfigMode;

    public FunctionEntry(FunctionConfiguration configuration) {
        functionName = configuration.getFunctionName();
        runtime = Runtime.fromValue(configuration.getRuntime());
        handler = configuration.getHandler();
        description = configuration.getDescription();
        functionArn = configuration.getFunctionArn();
        lastModified = configuration.getLastModified();
        role = configuration.getRole();
        timeout = configuration.getTimeout();
        memorySize = configuration.getMemorySize();
        tracingConfigMode = TracingMode.fromValue(configuration.getTracingConfig().getMode());
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFunctionArn() {
        return functionArn;
    }

    public void setFunctionArn(String functionArn) {
        this.functionArn = functionArn;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(Integer memorySize) {
        this.memorySize = memorySize;
    }

    public TracingMode getTracingConfigMode() {
        return tracingConfigMode;
    }

    public void setTracingConfigMode(TracingMode tracingConfigMode) {
        this.tracingConfigMode = tracingConfigMode;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", getFunctionName(), getRuntime().toString());
    }
}
