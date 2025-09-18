package io.github.satr.common;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;

public final class DateTimeHelper {
    public static String toFormattedString(LocalDateTime localDateTime) {
        return localDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
    }

    public static LocalDateTime toLocalDateTime(Long timestamp) {
        return LocalDateTime.ofInstant(new Date(timestamp).toInstant(), ZoneId.systemDefault());
    }
}
