package io.github.satr.common;
// Copyright © 2020, github.com/satr, MIT License

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OperationResultImpl implements OperationResult {
    private final List<String> debugMessages = new ArrayList<>();
    private final List<String> errorMessages = new ArrayList<>();
    private final List<String> warningMessages = new ArrayList<>();
    private final List<String> infoMessages = new ArrayList<>();

    @Override
    public void addInfo(String format, Object... args) {
        infoMessages.add(String.format(format, args));
    }

    @Override
    public void addError(String format, Object... args) {
        errorMessages.add(String.format(format, args));
    }

    @Override
    public void addDebug(String format, Object... args) {
        debugMessages.add(String.format(format, args));
    }

    @Override
    public void addWarning(String format, Object... args) {
        warningMessages.add(String.format(format, args));
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
    public boolean hasInfo() {
        return infoMessages.size() > 0;
    }

    @Override
    public boolean hasWarnings() {
        return warningMessages.size() > 0;
    }

    @Override
    public boolean hasErrors() {
        return errorMessages.size() > 0;
    }

    @Override
    public String getErrorAsString() {
        return join(errorMessages);
    }

    @Override
    public String getDebugAsString() {
        return join(debugMessages);
    }

    @Override
    public String getWarningsAsString() {
        return join(warningMessages);
    }

    @Override
    public String getInfoAsString() {
        return join(infoMessages);
    }

    @NotNull
    private String join(List<String> messages) {
        StringBuilder builder = new StringBuilder();
        for (String message : messages) {
            builder.append(message + "\n");
        }
        return builder.toString();
    }
}
