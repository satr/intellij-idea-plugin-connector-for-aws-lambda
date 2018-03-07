package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2018, github.com/satr, MIT License

import io.github.satr.common.Logger;
import io.github.satr.idea.plugin.connector.la.entities.*;
import io.github.satr.idea.plugin.connector.la.models.ProjectModel;

import java.io.File;

public interface ConnectorPresenter {
    void setView(ConnectorView view);
    void refreshFunctionList();
    void updateFunction();
    void shutdown();
    void refreshRegionList();
    void refreshTracingModeList();
    void refreshCredentialProfilesList();
    void refreshJarArtifactList();
    void refreshStatus();
    void refreshAll();
    void setRegion(RegionEntity regionEntity);
    void setCredentialProfile(CredentialProfileEntity credentialProfileEntity);
    void setFunction(FunctionEntity functionEntity);
    void setJarArtifact(ArtifactEntity artifactEntity);
    void runFunctionTest(String text);
    void openTestFunctionInputFile(File filename);
    String getLastSelectedTestFunctionInputFilePath();
    void setLastSelectedTestFunctionInputFilePath(String path);
    void setSetTestFunctionInputFromRecent(TestFunctionInputEntity entity);
    void setProxySettings();
    void refreshFunctionConfiguration();
    void addLogger(Logger logger);
    void setProjectModel(ProjectModel projectModel);
}
