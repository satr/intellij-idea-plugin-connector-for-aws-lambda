package io.github.satr.idea.plugin.connector.la.entities;
// Copyright © 2018, github.com/satr, MIT License

import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactType;

public class ArtifactEntry {

    private final String name;
    private final String outputFilePath;
    private final ArtifactType artifactType;

    public ArtifactEntry(Artifact artifact) {
        name = artifact.getName();
        outputFilePath = artifact.getOutputFilePath();
        artifactType = artifact.getArtifactType();
    }

    public String getName() {
        return name;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public ArtifactType getArtifactType() {
        return artifactType;
    }

    @Override
    public String toString() {
        return String.format("%s", getName(), getArtifactType().toString());
    }
}
