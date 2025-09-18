package io.github.satr.common;// Copyright © 2018, github.com/satr, MIT License

public interface OperationResult {
    void addInfo(String format, Object... args);
    void addWarning(String format, Object... arg);
    void addError(String format, Object... args);
    void addDebug(String format, Object... args);
    boolean failed();
    boolean success();
    boolean hasInfo();
    boolean hasWarnings();
    boolean hasErrors();
    String getDebugAsString();
    String getErrorAsString();
    String getWarningsAsString();
    String getInfoAsString();
}
