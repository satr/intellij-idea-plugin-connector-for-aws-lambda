package io.github.satr.idea.plugin.connector.la.entities;
// Copyright © 2018, github.com/satr, MIT License

import com.intellij.packaging.artifacts.Artifact;

import java.nio.file.Path;
import java.nio.file.Paths;

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

    public ArtifactEntity(String artifactName, String artifactPath, String artifactPackage, boolean isMavenized) {
        this(artifactName, Paths.get(artifactPath, artifactName).toAbsolutePath().toString(), isMavenized);
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
