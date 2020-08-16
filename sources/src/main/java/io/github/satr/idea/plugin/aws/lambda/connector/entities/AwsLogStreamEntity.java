package io.github.satr.idea.plugin.aws.lambda.connector.entities;

import com.amazonaws.services.logs.model.LogStream;
import io.github.satr.common.DateTimeHelper;

import java.time.LocalDateTime;

public class AwsLogStreamEntity {

    private String logGroupName;
    private final String logStreamName;
    private final LocalDateTime lastEventTimestamp;
    private final LocalDateTime lastEventTime;

    public AwsLogStreamEntity(String logGroupName, LogStream logStream) {
        this.logGroupName = logGroupName;
        logStreamName = logStream.getLogStreamName();
        lastEventTimestamp = DateTimeHelper.toLocalDateTime(logStream.getLastEventTimestamp());
        lastEventTime = DateTimeHelper.toLocalDateTime(logStream.getLastEventTimestamp());
    }

    public String getLogStreamName() {
        return logStreamName;
    }

    public String getLogGroupName() {
        return logGroupName;
    }

    public LocalDateTime getLastEventTime() {
        return lastEventTime;
    }

    @Override
    public String toString() {
        return String.format("%s - %s : \"%s\"", DateTimeHelper.toFormattedString(lastEventTime),
                DateTimeHelper.toFormattedString(lastEventTimestamp),
                logStreamName);
    }
}
