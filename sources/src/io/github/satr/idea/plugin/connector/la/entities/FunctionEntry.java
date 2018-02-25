package io.github.satr.idea.plugin.connector.la.entities;
// Copyright © 2018, github.com/satr, MIT License

import com.amazonaws.services.lambda.model.*;
import com.amazonaws.services.lambda.model.Runtime;

public class FunctionEntry {
    private final String functionName;
    private final Runtime runtime;
    private String handler;
    private String description;
    private String arn;
    private String lastModified;
    private RoleEntity roleEntity;
    private Integer timeout;
    private Integer memorySize;
    private TracingModeEntity tracingModeEntity;

    public FunctionEntry(UpdateFunctionCodeResult updateFunctionCodeResult) {
        functionName = updateFunctionCodeResult.getFunctionName();
        runtime = Runtime.fromValue(updateFunctionCodeResult.getRuntime());
        handler = updateFunctionCodeResult.getHandler();
    }

    public FunctionEntry(FunctionConfiguration functionConfiguration, RoleEntity roleEntity) {
        functionName = functionConfiguration.getFunctionName();
        runtime = Runtime.fromValue(functionConfiguration.getRuntime());
        handler = functionConfiguration.getHandler();
        description = functionConfiguration.getDescription();
        arn = functionConfiguration.getFunctionArn();
        lastModified = functionConfiguration.getLastModified();
        this.roleEntity = roleEntity;
        timeout = functionConfiguration.getTimeout();
        memorySize = functionConfiguration.getMemorySize();
        tracingModeEntity = TracingModeEntity.fromValue(functionConfiguration.getTracingConfig().getMode());
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

    public String getArn() {
        return arn;
    }

    public void setArn(String arn) {
        this.arn = arn;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public RoleEntity getRoleEntity() {
        return roleEntity;
    }

    public void setRole(RoleEntity role) {
        this.roleEntity = role;
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

    public TracingModeEntity getTracingModeEntity() {
        return tracingModeEntity;
    }

    public void setTracingModeEntity(TracingModeEntity tracingModeEntity) {
        this.tracingModeEntity = tracingModeEntity;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", getFunctionName(), getRuntime().toString());
    }
}
