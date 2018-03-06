package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2018, github.com/satr, MIT License

import com.amazonaws.regions.Regions;
import io.github.satr.common.OperationResult;
import io.github.satr.idea.plugin.connector.la.entities.*;

import java.util.Collection;
import java.util.List;

public interface ConnectorView {
    void setFunctionList(List<FunctionEntry> functions, FunctionEntry selectedFunctionEntry);
    void setArtifactList(Collection<? extends ArtifactEntry> artifacts, ArtifactEntry selectedArtifactEntry);
    void setRegionList(List<RegionEntry> regions, Regions selectedRegion);
    void setTracingModeList(Collection<TracingModeEntity> tracingModeEntities);
    void setCredentialProfilesList(List<CredentialProfileEntry> credentialProfiles, String selectedCredentialsProfile);
    void setRegion(Regions region);
    void refreshStatus(String function, String artifact, String region, String regionDescription, String credentialProfile, String proxyDetails);
    FunctionEntry getSelectedFunctionEntry();

    FunctionEntry getSelectedFunctionEntryWithUpdateConfiguration();

    ArtifactEntry getSelectedArtifactEntry();
    RegionEntry getSelectedRegionEntry();
    CredentialProfileEntry getSelectedCredentialProfileEntry();
    void log(OperationResult operationResult);
    void logDebug(String format, Object... args);
    void logWarning(String format, Object... args);
    void logInfo(String format, Object... args);
    void logError(String format, Object... args);
    void logError(Throwable throwable);
    void setFunctionTestOutput(String outputText);
    void setTestFunctionInput(String inputText);
    void setTestFunctionInputRecentEntryList(List<TestFunctionInputEntry> filePathList);
    void setFunctionConfiguration(FunctionEntry functionEntry);
    void setRoleList(List<RoleEntity> roles);
    void updateFunctionEntry(FunctionEntry functionEntry);
}
