package io.github.satr.idea.plugin.aws.lambda.connector.entities;

import com.amazonaws.services.logs.model.OutputLogEvent;
import io.github.satr.common.DateTimeHelper;

import java.time.LocalDateTime;

public class AwsLogStreamEventEntity {

    private final LocalDateTime timeStamp;
    private final String message;

    public AwsLogStreamEventEntity(OutputLogEvent event) {
        timeStamp = DateTimeHelper.toLocalDateTime(event.getTimestamp());
        message = event.getMessage();
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format("%s : %s", DateTimeHelper.toFormattedString(timeStamp), message);
    }
}
