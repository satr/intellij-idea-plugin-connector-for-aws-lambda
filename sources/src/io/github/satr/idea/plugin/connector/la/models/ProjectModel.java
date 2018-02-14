package io.github.satr.idea.plugin.connector.la.models;
// Copyright © 2018, github.com/satr, MIT License

import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import io.github.satr.idea.plugin.connector.la.entities.ArtifactEntry;

import java.util.Collection;

public interface ProjectModel {
    Collection<? extends ArtifactEntry> getJarArtifacts(Project project);
}
