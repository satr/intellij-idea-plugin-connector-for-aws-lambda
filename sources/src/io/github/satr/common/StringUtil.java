package io.github.satr.common;

import org.jetbrains.annotations.NotNull;

public final class StringUtil {
    @NotNull
    public static String getNotEmptyString(String lastSelectedItem) {
        return getNotEmptyString(lastSelectedItem, "");
    }

    @NotNull
    public static String getNotEmptyString(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
}
