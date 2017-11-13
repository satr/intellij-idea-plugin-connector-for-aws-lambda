package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2017, github.com/satr, MIT License

import com.intellij.openapi.project.Project;
import io.github.satr.idea.plugin.connector.la.entities.ArtifactEntry;
import io.github.satr.idea.plugin.connector.la.entities.CredentialProfileEntry;
import io.github.satr.idea.plugin.connector.la.entities.FunctionEntry;
import io.github.satr.idea.plugin.connector.la.entities.RegionEntry;

public interface ConnectorPresenter {
    void setView(ConnectorView view);
    void refreshFunctionList();
    void updateFunction(FunctionEntry functionEntry, ArtifactEntry artifactEntry, Project project);
    void shutdown();
    void refreshRegionList(Project project);
    void refreshCredentialProfilesList(Project project);
    void refreshJarArtifactList(Project project);
    void setRegion(RegionEntry regionEntry);
    void setCredentialProfile(CredentialProfileEntry credentialProfileEntry);
}
