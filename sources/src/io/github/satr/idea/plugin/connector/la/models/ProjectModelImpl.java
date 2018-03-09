package io.github.satr.idea.plugin.connector.la.models;
// Copyright © 2018, github.com/satr, MIT License

import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactType;
import io.github.satr.idea.plugin.connector.la.entities.ArtifactEntity;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ProjectModelImpl implements ProjectModel {
    private final String JAR_ARTIFACT_TYPE = "jar";
    private final Project project;
    private final MavenProjectsManager mavenProjectsManager;

    public ProjectModelImpl(Project project) {
        this.project = project;
        mavenProjectsManager = MavenProjectsManager.getInstance(project);
    }

    @Override
    public Collection<? extends ArtifactEntity> getJarArtifacts() {
        if(project == null) {
            return Collections.emptyList();
        }
        final ArrayList<ArtifactEntity> artifactEntries = new ArrayList<>();
        addProjectArtifacts(artifactEntries);
        addMavenProjectArtifacts(artifactEntries);
        return artifactEntries;
    }

    private void addMavenProjectArtifacts(ArrayList<ArtifactEntity> artifactEntries) {
        if (mavenProjectsManager == null || !mavenProjectsManager.isMavenizedProject()) {
            return;
        }
        for(MavenProject mavenProject : mavenProjectsManager.getProjects()) {
            if(!JAR_ARTIFACT_TYPE.equals(mavenProject.getPackaging())){
                continue;
            }
            String artifactName = String.format("%s.%s", mavenProject.getFinalName(), mavenProject.getPackaging());
            artifactEntries.add(new ArtifactEntity(artifactName, mavenProject.getBuildDirectory(), artifactName, true));
        }
    }

    private void addProjectArtifacts(ArrayList<ArtifactEntity> artifactEntries) {
        final ArtifactManager artifactManager = ArtifactManager.getInstance(this.project);
        final Collection<? extends Artifact> jarArtifacts = artifactManager.getArtifactsByType(ArtifactType.findById(JAR_ARTIFACT_TYPE));
        for(Artifact artifact : jarArtifacts) {
            artifactEntries.add(new ArtifactEntity(artifact));
        }
    }
}
