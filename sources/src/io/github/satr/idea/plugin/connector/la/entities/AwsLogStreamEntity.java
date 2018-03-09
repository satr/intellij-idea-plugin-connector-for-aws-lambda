package io.github.satr.idea.plugin.connector.la.entities;

import com.amazonaws.services.logs.model.LogStream;
import io.github.satr.common.DateTimeHelper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;

public class AwsLogStreamEntity {

    private String logGroupName;
    private final String logStreamName;
    private final LocalDateTime lastEventTimestamp;
    private final LocalDateTime creationTime;

    public AwsLogStreamEntity(String logGroupName, LogStream logStream) {
        this.logGroupName = logGroupName;
        logStreamName = logStream.getLogStreamName();
        lastEventTimestamp = DateTimeHelper.toLocalDateTime(logStream.getLastEventTimestamp());
        creationTime = DateTimeHelper.toLocalDateTime(logStream.getCreationTime());
    }

    public String getLogStreamName() {
        return logStreamName;
    }

    public String getLogGroupName() {
        return logGroupName;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    @Override
    public String toString() {
        return String.format("%s - %s : \"%s\"", DateTimeHelper.toFormattedString(creationTime),
                DateTimeHelper.toFormattedString(lastEventTimestamp),
                logStreamName);
    }
}
