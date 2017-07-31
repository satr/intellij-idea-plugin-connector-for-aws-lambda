package io.github.satr.common;// Copyright © 2017, github.com/satr, MIT License

public interface OperationResult {
    void addError(String format, Object... args);
    boolean failed();
    boolean success();
    String getErrorAsString();
}
