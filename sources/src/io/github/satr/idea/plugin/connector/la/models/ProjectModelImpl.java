package io.github.satr.idea.plugin.connector.la.models;
// Copyright © 2018, github.com/satr, MIT License

import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactType;
import io.github.satr.idea.plugin.connector.la.entities.ArtifactEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ProjectModelImpl implements ProjectModel {
    private final String JAR_ARTIFACT_TYPE = "jar";

    @Override
    public Collection<? extends ArtifactEntry> getJarArtifacts(Project project) {
        if(project == null)
            return Collections.emptyList();
        ArtifactManager artifactManager = ArtifactManager.getInstance(project);
        final Collection<? extends Artifact> jarArtifacts = artifactManager.getArtifactsByType(ArtifactType.findById(JAR_ARTIFACT_TYPE));
        ArrayList<ArtifactEntry> artifactEntries = new ArrayList<>();
        for(Artifact artifact : jarArtifacts)
            artifactEntries.add(new ArtifactEntry(artifact));
        return artifactEntries;
    }
}
