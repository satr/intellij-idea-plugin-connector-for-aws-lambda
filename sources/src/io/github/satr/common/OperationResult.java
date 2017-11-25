package io.github.satr.common;// Copyright © 2017, github.com/satr, MIT License

public interface OperationResult {
    void addInfo(String format, Object... args);
    void addError(String format, Object... args);
    boolean failed();
    boolean success();
    boolean hasInfo();
    String getErrorAsString();
    String getInfoAsString();
}
