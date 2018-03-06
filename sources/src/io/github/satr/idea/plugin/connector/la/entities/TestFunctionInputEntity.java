package io.github.satr.idea.plugin.connector.la.entities;

public class TestFunctionInputEntity {
    private String filePath;
    private final String fileName;
    private final String inputText;

    public TestFunctionInputEntity(String filePath, String fileName, String inputText) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.inputText = inputText;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", fileName, filePath);
    }

    public String getInputText() {
        return inputText;
    }

    public String getFileName() {
        return fileName;
    }
}
