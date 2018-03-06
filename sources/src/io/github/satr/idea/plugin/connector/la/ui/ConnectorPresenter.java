package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2018, github.com/satr, MIT License

import com.intellij.openapi.project.Project;
import io.github.satr.idea.plugin.connector.la.entities.*;

import java.io.File;

public interface ConnectorPresenter {
    void setView(ConnectorView view);
    void refreshFunctionList();
    void updateFunction(Project project);
    void shutdown();
    void refreshRegionList(Project project);
    void refreshTracingModeList();
    void refreshCredentialProfilesList(Project project);
    void refreshJarArtifactList(Project project);
    void refreshStatus();
    void refreshAll(Project project);
    void setRegion(RegionEntity regionEntity);
    void setCredentialProfile(CredentialProfileEntity credentialProfileEntity);
    void setFunction(FunctionEntity functionEntity);
    void setJarArtifact(ArtifactEntity artifactEntity);
    void runFunctionTest(Project project, String text);
    void openTestFunctionInputFile(File filename);
    String getLastSelectedTestFunctionInputFilePath();
    void setLastSelectedTestFunctionInputFilePath(String path);
    void setSetTestFunctionInputFromRecent(TestFunctionInputEntity entity);
    void setProxySettings();
    void refreshFunctionConfiguration(Project project);
}
