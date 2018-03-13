package io.github.satr.common;

import java.util.ArrayList;
import java.util.List;

public class CompositeLogger implements Logger {
    private final List<Logger> loggers = new ArrayList<>();
    private final List<org.apache.log4j.Logger> log4jLoggers = new ArrayList<>();

    @Override
    public void logOperationResult(OperationResult operationResult) {
        if(operationResult.hasInfo()) {
            logInfo(operationResult.getInfoAsString());
        }
        if(operationResult.hasWarnings()) {
            logWarning(operationResult.getWarningsAsString());
        }
        if(operationResult.hasErrors()) {
            logInfo(operationResult.getErrorAsString());
        }
    }

    @Override
    public void logDebug(String format, Object... args) {
        for (Logger logger : loggers) {
            logger.logDebug(String.format(format, args));
        }
    }

    @Override
    public void logInfo(String format, Object... args) {
        String message = String.format(format, args);
        for (Logger logger : loggers) {
            logger.logInfo(message);
        }
        for (org.apache.log4j.Logger logger : log4jLoggers) {
            logger.info(message);
        }
    }

    @Override
    public void logWarning(String format, Object... args) {
        String message = String.format(format, args);
        for (Logger logger : loggers) {
            logger.logWarning(message);
        }
        for (org.apache.log4j.Logger logger : log4jLoggers) {
            logger.warn(message);
        }
    }

    @Override
    public void logError(String format, Object... args) {
        String message = String.format(format, args);
        for (Logger logger : loggers) {
            logger.logError(message);
        }
        for (org.apache.log4j.Logger logger : log4jLoggers) {
            logger.error(message);
        }
    }

    @Override
    public void logError(Throwable throwable) {
        logError(throwable.getMessage());
    }


    public void addLogger(Logger logger) {
        loggers.add(logger);
    }

    public void addLogger(org.apache.log4j.Logger logger) {
        log4jLoggers.add(logger);
    }
}
