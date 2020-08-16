package io.github.satr.idea.plugin.aws.lambda.connector.ui;
// Copyright © 2020, github.com/satr, MIT License

import io.github.satr.common.Logger;
import io.github.satr.idea.plugin.aws.lambda.connector.entities.*;
import io.github.satr.idea.plugin.aws.lambda.connector.models.ProjectModel;

import java.io.File;

public interface ConnectorPresenter {
    void setView(ConnectorView view);
    void refreshFunctionList();
    void updateFunction();
    void shutdown();
    void refreshRegionList();
    void refreshTracingModeList();
    void refreshCredentialProfilesList();
    void refreshArtifactList();
    void refreshStatus();
    void refreshAll();
    void setRegion(RegionEntity regionEntity);
    void setCredentialProfile(CredentialProfileEntity credentialProfileEntity);
    void setFunction(FunctionEntity functionEntity);
    void setArtifact(ArtifactEntity artifactEntity);
    void runFunctionTest(String inputText, boolean autoFormatOutput);
    void openTestFunctionInputFile(File filename);
    String getLastSelectedTestFunctionInputFilePath();
    void setLastSelectedTestFunctionInputFilePath(String path);
    void setSetTestFunctionInputFromRecent(TestFunctionInputEntity entity);
    void setProxySettings();
    void refreshFunctionConfiguration();
    void addLogger(Logger logger);
    void setProjectModel(ProjectModel projectModel);
    void setAwsLogStreamEventList(AwsLogStreamEntity entity);
    void setAutoRefreshAwsLog(boolean autoRefresh);
    void refreshAwsLogStreams();
    void runGetNextAwsLogStreamSet();
    boolean roleListLoaded();
    boolean initializeFunctionRoleList();
    void deleteAwsLogStreams();
    void reformatJsonFunctionTestInput(String jsonText);
    void reformatJsonFunctionTestOutput(String jsonText);
    void setAwsLogStreamEvent(AwsLogStreamEventEntity entity);
}
