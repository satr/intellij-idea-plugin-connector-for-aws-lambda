package io.github.satr.common;

public interface Logger {
    void logOperationResult(OperationResult operationResult);
    void logDebug(String format, Object... args);
    void logWarning(String format, Object... args);
    void logInfo(String format, Object... args);
    void logError(String format, Object... args);
    void logError(Throwable throwable);
}
