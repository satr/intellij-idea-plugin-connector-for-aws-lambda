package io.github.satr.idea.plugin.aws.lambda.connector.ui;
// Copyright © 2020, github.com/satr, MIT License

import com.amazonaws.regions.Regions;
import io.github.satr.idea.plugin.aws.lambda.connector.entities.*;

import java.util.Collection;
import java.util.List;

public interface ConnectorView {
    void setFunctionList(List<FunctionEntity> functions, FunctionEntity selectedFunctionEntity);
    void setArtifactList(Collection<? extends ArtifactEntity> artifacts, ArtifactEntity selectedArtifactEntity);
    void setRegionList(List<RegionEntity> regions, Regions selectedRegion);
    void setTracingModeList(Collection<TracingModeEntity> tracingModeEntities);
    void setCredentialProfilesList(List<CredentialProfileEntity> credentialProfiles, String selectedProfileName);
    void setRegion(Regions region);
    void refreshStatus(String function, String artifact, String region, String regionDescription, String credentialProfile, String proxyDetails);
    FunctionEntity getSelectedFunctionEntity();
    FunctionEntity getSelectedFunctionEntityWithUpdateConfiguration();
    ArtifactEntity getSelectedArtifactEntity();
    RegionEntity getSelectedRegionEntity();
    CredentialProfileEntity getSelectedCredentialProfileEntity();
    void setFunctionTestOutput(String outputText);
    void setFunctionTestInput(String inputText);
    void setTestFunctionInputRecentEntityList(List<TestFunctionInputEntity> filePathList);
    void setFunctionConfiguration(FunctionEntity functionEntity);
    void setRoleList(List<RoleEntity> roles, RoleEntity selectedRoleEntity);
    void updateFunctionEntity(FunctionEntity functionEntity);
    void setAwsLogStreamList(List<AwsLogStreamEntity> awsLogEventEntities);
    void setAwsLogStreamEventList(List<AwsLogStreamEventEntity> awsLogStreamEventEntities);
    void showError(String format, Object... args);
    void showInfo(String format, Object... args);
    void clearAwsLogStreamEventList();
    void clearAwsLogStreamList();
    void clearRoleList();
    void setAwsLogStreamEvent(String timestamp, String message);
}
