package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2018, github.com/satr, MIT License

import com.amazonaws.regions.Regions;
import io.github.satr.common.OperationResult;
import io.github.satr.idea.plugin.connector.la.entities.*;

import java.util.Collection;
import java.util.List;

public interface ConnectorView {
    void setFunctionList(List<FunctionEntity> functions, FunctionEntity selectedFunctionEntity);
    void setArtifactList(Collection<? extends ArtifactEntity> artifacts, ArtifactEntity selectedArtifactEntity);
    void setRegionList(List<RegionEntity> regions, Regions selectedRegion);
    void setTracingModeList(Collection<TracingModeEntity> tracingModeEntities);
    void setCredentialProfilesList(List<CredentialProfileEntity> credentialProfiles, String selectedCredentialsProfile);
    void setRegion(Regions region);
    void refreshStatus(String function, String artifact, String region, String regionDescription, String credentialProfile, String proxyDetails);
    FunctionEntity getSelectedFunctionEntity();

    FunctionEntity getSelectedFunctionEntityWithUpdateConfiguration();

    ArtifactEntity getSelectedArtifactEntity();
    RegionEntity getSelectedRegionEntity();
    CredentialProfileEntity getSelectedCredentialProfileEntity();
    void log(OperationResult operationResult);
    void logDebug(String format, Object... args);
    void logWarning(String format, Object... args);
    void logInfo(String format, Object... args);
    void logError(String format, Object... args);
    void logError(Throwable throwable);
    void setFunctionTestOutput(String outputText);
    void setTestFunctionInput(String inputText);
    void setTestFunctionInputRecentEntityList(List<TestFunctionInputEntity> filePathList);
    void setFunctionConfiguration(FunctionEntity functionEntity);
    void setRoleList(List<RoleEntity> roles);
    void updateFunctionEntity(FunctionEntity functionEntity);
}
