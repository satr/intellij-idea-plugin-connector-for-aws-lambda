package io.github.satr.common;
// Copyright © 2017, github.com/satr, MIT License

import java.util.ArrayList;
import java.util.List;

public class OperationResultImpl implements OperationResult {
    private final List<String> errorMessages = new ArrayList<>();

    @Override
    public void addError(String format, Object... args) {
        errorMessages.add(String.format(format, args));
    }

    @Override
    public boolean failed() {
        return !success();
    }

    @Override
    public boolean success() {
        return errorMessages.size() == 0;
    }

    @Override
    public String getErrorAsString() {
        StringBuilder builder = new StringBuilder();
        for(String message : errorMessages)
            builder.append(message + "\n");
        return builder.toString();
    }
}
