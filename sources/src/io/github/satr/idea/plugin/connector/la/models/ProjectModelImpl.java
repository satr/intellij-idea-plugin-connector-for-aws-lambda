package io.github.satr.idea.plugin.connector.la.models;
// Copyright © 2018, github.com/satr, MIT License

import com.intellij.history.core.Paths;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.impl.elements.ArchivePackagingElement;
import com.intellij.packaging.impl.elements.PackagingElementFactoryImpl;
import io.github.satr.idea.plugin.connector.la.entities.ArtifactEntity;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ProjectModelImpl implements ProjectModel {
    private final String JAR_ARTIFACT_TYPE = "jar";
    private final String PLAIN_ARTIFACT_TYPE = "plain";
    private final Project project;
    private final MavenProjectsManager mavenProjectsManager;

    public ProjectModelImpl(Project project) {
        this.project = project;
        mavenProjectsManager = MavenProjectsManager.getInstance(project);
    }

    @Override
    public List<? extends ArtifactEntity> getArtifacts() {
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
            artifactEntries.add(new ArtifactEntity(artifactName, mavenProject.getBuildDirectory(), true));
        }
    }

    private void addProjectArtifacts(ArrayList<ArtifactEntity> artifactEntries) {
        final ArtifactManager artifactManager = ArtifactManager.getInstance(this.project);
        collectJarArtifacts(artifactEntries, artifactManager);
        collectArchivesFromPlainArtifacts(artifactEntries, artifactManager);
    }

    private void collectArchivesFromPlainArtifacts(ArrayList<ArtifactEntity> artifactEntries, ArtifactManager artifactManager) {
        Collection<? extends Artifact> artifactsByType = artifactManager.getArtifactsByType(ArtifactType.findById(PLAIN_ARTIFACT_TYPE));
        artifactsByType.stream().forEach(artifact -> addArtifactEntity(artifactEntries, artifact));
    }

    private boolean addArtifactEntity(ArrayList<ArtifactEntity> artifactEntries, Artifact artifact) {
        CompositePackagingElement<?> rootElement = artifact.getRootElement();
        if(rootElement.getType() == PackagingElementFactoryImpl.ARCHIVE_ELEMENT_TYPE) {
            artifactEntries.add(new ArtifactEntity(artifact));            return true;
        }
        for(PackagingElement<?> packagingElement : rootElement.getChildren()) {
            if (packagingElement.getType() != PackagingElementFactoryImpl.ARCHIVE_ELEMENT_TYPE) {
                continue;
            }
            ArchivePackagingElement archivePackagingElement = (ArchivePackagingElement) packagingElement;
            if (archivePackagingElement == null) {
                continue;
            }

            String filePath = Paths.appended(artifact.getOutputFilePath(), archivePackagingElement.getArchiveFileName());
            artifactEntries.add(new ArtifactEntity(packagingElement.toString(), filePath, false));
            return true;
        }
        return true;
    }

    private void collectJarArtifacts(ArrayList<ArtifactEntity> artifactEntries, ArtifactManager artifactManager) {
        artifactManager.getArtifactsByType(ArtifactType.findById(JAR_ARTIFACT_TYPE))
                .stream()
                .forEach(artifact -> artifactEntries.add(new ArtifactEntity(artifact)));
    }
}
