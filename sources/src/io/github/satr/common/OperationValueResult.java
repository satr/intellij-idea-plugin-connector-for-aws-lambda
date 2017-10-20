package io.github.satr.common;
// Copyright © 2017, github.com/satr, MIT License

public interface OperationValueResult <T> extends OperationResult {
    void setValue(T value);
    T getValue();
}