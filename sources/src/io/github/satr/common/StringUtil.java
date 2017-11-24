package io.github.satr.common;

import org.jetbrains.annotations.NotNull;

import static org.apache.http.util.TextUtils.isEmpty;

public final class StringUtil {
    @NotNull
    public static String getNotEmptyString(String lastSelectedItem) {
        return getNotEmptyString(lastSelectedItem, "");
    }

    @NotNull
    public static String getNotEmptyString(String value, String defaultValue) {
        return isEmpty(value) ? defaultValue : value;
    }
}
