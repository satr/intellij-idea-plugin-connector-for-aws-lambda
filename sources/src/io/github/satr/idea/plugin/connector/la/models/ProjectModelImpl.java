package io.github.satr.idea.plugin.connector.la.models;
// Copyright © 2017, github.com/satr, MIT License

import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactType;

import java.util.Collection;

public class ProjectModelImpl implements ProjectModel {
    private final String JAR_ARTIFACT_TYPE = "jar";
    protected ConnectorSettings connectorSettings = ConnectorSettings.getInstance();

    @Override
    public Artifact getArtifact(Project project) {
        ArtifactManager artifactManager = ArtifactManager.getInstance(project);
        final Collection<? extends Artifact> jarArtifacts = artifactManager.getArtifactsByType(ArtifactType.findById(JAR_ARTIFACT_TYPE));
        if(jarArtifacts.isEmpty())
            return null;

        Artifact artifact = null;
        String lastSelectedJarArtifactName = connectorSettings.getLastSelectedJarArtifactName();
        artifact = (Artifact) jarArtifacts.toArray()[0];
        for(Artifact art : jarArtifacts) {
            if (!art.getName().equals(lastSelectedJarArtifactName))
                continue;
            return art;
        }
        return artifact;
    }
}
