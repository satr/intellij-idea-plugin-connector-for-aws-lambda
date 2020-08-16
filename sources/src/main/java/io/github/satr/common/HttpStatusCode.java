package io.github.satr.common;

public enum HttpStatusCode {
    OK(200, "OK");

    private final int code;
    private final String codeDescription;

    HttpStatusCode(int code, String codeDescription) {

        this.code = code;
        this.codeDescription = codeDescription;
    }

    public int getCode() {
        return code;
    }

    public String getCodeDescription() {
        return codeDescription;
    }
}
