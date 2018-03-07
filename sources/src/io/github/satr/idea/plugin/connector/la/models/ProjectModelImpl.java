package io.github.satr.idea.plugin.connector.la.models;
// Copyright © 2018, github.com/satr, MIT License

import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactType;
import io.github.satr.idea.plugin.connector.la.entities.ArtifactEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ProjectModelImpl implements ProjectModel {
    private final String JAR_ARTIFACT_TYPE = "jar";
    private final Project project;

    public ProjectModelImpl(Project project) {
        this.project = project;
    }

    @Override
    public Collection<? extends ArtifactEntity> getJarArtifacts() {
        if(project == null) {
            return Collections.emptyList();
        }
        final ArtifactManager artifactManager = ArtifactManager.getInstance(this.project);
        final Collection<? extends Artifact> jarArtifacts = artifactManager.getArtifactsByType(ArtifactType.findById(JAR_ARTIFACT_TYPE));
        final ArrayList<ArtifactEntity> artifactEntries = new ArrayList<>();
        for(Artifact artifact : jarArtifacts) {
            artifactEntries.add(new ArtifactEntity(artifact));
        }
        return artifactEntries;
    }
}
