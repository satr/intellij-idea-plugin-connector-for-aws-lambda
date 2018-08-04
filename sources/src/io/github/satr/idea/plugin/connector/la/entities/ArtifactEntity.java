package io.github.satr.idea.plugin.connector.la.entities;
// Copyright © 2018, github.com/satr, MIT License

import com.intellij.packaging.artifacts.Artifact;

public class ArtifactEntity {

    private final String name;
    private final String outputFilePath;
    private boolean isMavenized;

    public ArtifactEntity(Artifact artifact) {
        this(artifact.getName(), artifact.getOutputFilePath(), false);
    }

    public ArtifactEntity(String artifactName, String artifactFilePath, boolean isMavenized) {
        name = artifactName;
        this.outputFilePath = artifactFilePath;
        this.isMavenized = isMavenized;
    }

    public String getName() {
        return name;
    }
    public String getOutputFilePath() {
        return outputFilePath;
    }

    @Override
    public String toString() {
        return String.format("%s%s", getName(), getTypeDescription());
    }

    public String getTypeDescription() {
        return isMavenized ? " (Maven)" : "";
    }
}
