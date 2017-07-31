package io.github.satr.idea.plugin.connector.la.models;
// Copyright © 2017, github.com/satr, MIT License

import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;

public interface ProjectModel {
    Artifact getArtifact(Project project);
}
