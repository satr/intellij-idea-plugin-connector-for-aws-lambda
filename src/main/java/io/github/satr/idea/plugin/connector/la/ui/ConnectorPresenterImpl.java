package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2018, github.com/satr, MIT License

import com.amazonaws.auth.profile.internal.BasicProfile;
import com.amazonaws.regions.Regions;
import io.github.satr.common.*;
import io.github.satr.idea.plugin.connector.la.entities.*;
import io.github.satr.idea.plugin.connector.la.models.ConnectorSettings;
import io.github.satr.idea.plugin.connector.la.models.FunctionConnectorModel;
import io.github.satr.idea.plugin.connector.la.models.RoleConnectorModel;
import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.apache.http.util.TextUtils.isEmpty;

public class ConnectorPresenterImpl extends AbstractConnectorPresenter implements ConnectorPresenter {
    private ConnectorView view;
    private List<TestFunctionInputEntity> testFunctionInputRecentEntityList = new ArrayList<>();
    private boolean autoRefreshAwsLog = false;
    private JsonHelper jsonHelper = new JsonHelper();

    @Override
    public void setView(ConnectorView view) {
        this.view = view;
    }

    @Override
    public void refreshFunctionList() {
        getLogger().logDebug("Refresh function list.");

        clearRolesList();
        List<FunctionEntity> functionList = getFunctionEntities();

        String lastSelectedFunctionName = getConnectorSettings().getLastSelectedFunctionName();
        FunctionEntity selectedFunctionEntity = null;
        int functionCount = 0;
        ArrayList<String> functionNames = new ArrayList<>();
        for (FunctionEntity entity : functionList) {
            functionNames.add(entity.getFunctionName());
            if(entity.getFunctionName().equals(lastSelectedFunctionName)){
                selectedFunctionEntity = entity;
            }
            functionCount++;
        }
        getLogger().logDebug("Found %d functions.", functionCount);

        getConnectorSettings().setFunctionNames(functionNames);
        view.setFunctionList(functionList, selectedFunctionEntity);

        FunctionEntity functionEntity = view.getSelectedFunctionEntity();
        if(functionEntity == null) {
            getLogger().logInfo("Function is not selected.");
        } else {
            getLogger().logInfo("Selected function: \"%s\"", functionEntity.getFunctionName());
        }
        view.setFunctionConfiguration(functionEntity);
        refreshStatus();
    }

    @NotNull
    public List<FunctionEntity> getFunctionEntities() {
        OperationValueResult<List<FunctionEntity>> functionListResult = getFunctionConnectorModel().getFunctions();
        getLogger().logOperationResult(functionListResult);
        List<FunctionEntity> functionList = functionListResult.getValue();
        functionList.sort(Comparator.comparing(FunctionEntity::getFunctionName));
        return functionList;
    }

    private void clearRolesList() {
        view.clearRoleList();
    }

    @Override
    public void updateFunction() {
        getLogger().logDebug("Update function.");
        FunctionEntity functionEntity = view.getSelectedFunctionEntityWithUpdateConfiguration();
        if(functionEntity.isChanged()){
            OperationResult updateFunctionConfigurationResult = getFunctionConnectorModel().updateConfiguration(functionEntity);
            if(updateFunctionConfigurationResult.failed()){
                showError("Lambda function configuration update failed: \"%s\"",
                        updateFunctionConfigurationResult.getErrorAsString());
                return;
            }
        }
        updateFunctionCode(functionEntity);
    }

    private void updateFunctionCode(FunctionEntity functionEntity) {
        ArtifactEntity artifactEntity = view.getSelectedArtifactEntity();
        OperationResult validationResult = validateToUpdate(functionEntity, artifactEntity);
        if(validationResult.failed()){
            showError(validationResult.getErrorAsString());
            return;
        }
        String functionName = functionEntity.getFunctionName();
        String artifactFilePath = artifactEntity.getOutputFilePath();
        final OperationValueResult<FunctionEntity> result = getFunctionConnectorModel().updateWithArtifact(functionEntity, artifactFilePath);
        if (!result.success()) {
            showError(result.getErrorAsString());
            return;
        }
        getConnectorSettings().setLastSelectedFunctionName(functionName);
        getConnectorSettings().setLastSelectedArtifactName(artifactEntity.getName());
        FunctionEntity updatedFunctionEntity = result.getValue();
        view.updateFunctionEntity(updatedFunctionEntity);
        setFunction(updatedFunctionEntity);
        showInfo("Lambda function \"%s\" has been updated with the artifact \"%s\".",
                            updatedFunctionEntity.getFunctionName(), artifactEntity.getName());
    }

    private void showError(String format, Object... args) {
        view.showError(format, args);
        getLogger().logError(format, args);
    }

    private void showInfo(String format, Object... args) {
        view.showInfo(format, args);
        getLogger().logInfo(format, args);
    }

    private OperationResult validateToUpdate(FunctionEntity functionEntity, ArtifactEntity artifactEntity) {
        final OperationResultImpl result = new OperationResultImpl();
        if(functionEntity == null){
            result.addError("Function is not selected.");
        }
        if(artifactEntity == null){
            result.addError("Artifact is not selected.");
        }
        if(result.failed()) {
            return result;
        }
        String outputFilePath = artifactEntity.getOutputFilePath();
        final File codeFile = new File(outputFilePath);
        if(!codeFile.exists()){
            result.addError("Artifact file does not exist.");
            return result;
        }
        final long codeFileSize = codeFile.length();
        final int _50MB = 52428800;
        if(codeFileSize > _50MB){
            result.addError("Code file sized exceeds 50MB limit - its size is %d bytes.", codeFileSize);
            return result;
        }
        final int zipContentSize = FileHelper.getZipContentSize(codeFile);
        if(zipContentSize <= 0){
            result.addError("Code file is invalid of empty.");
            return result;
        }
        final int _250MB = 262144000;
        if(zipContentSize > _250MB){
            result.addError("Code file content size (with dependencies) exceeds 250MB limit - its size is %d bytes.", zipContentSize);
            return result;
        }
        return result;
    }

    @Override
    public void shutdown() {
        getLogger().logDebug("Shutdown.");
        shutdownConnectorModel();
    }

    @Override
    public void refreshRegionList() {
        getLogger().logDebug("Refresh regionName list.");
        view.setRegionList(getFunctionConnectorModel().getRegions(), getLastSelectedRegion());
    }

    @Override
    public void refreshTracingModeList() {
        getLogger().logDebug("Refresh trace mode list.");
        view.setTracingModeList(TracingModeEntity.values());
    }

    @Override
    public void refreshCredentialProfilesList() {
        getLogger().logDebug("Refresh credential profile list.");
        FunctionConnectorModel functionConnectorModel = getFunctionConnectorModel();
        OperationValueResult<List<CredentialProfileEntity>> result = functionConnectorModel.getCredentialProfileEntities();
        if(result.success()) {
            List<CredentialProfileEntity> profileEntities = result.getValue();
            CredentialProfileEntity selectedProfileEntry = getLastSelectedProfileEntry(functionConnectorModel, profileEntities);
            if (selectedProfileEntry == null) {
                result.addError("Profile is not selected.");
            } else {
                String profileName = selectedProfileEntry.getName();
                getConnectorSettings().setLastSelectedCredentialProfile(profileName);
                view.setCredentialProfilesList(profileEntities, profileName);
                Regions region = tryGetRegionBy(functionConnectorModel.getRegionName());
                if (region != null) {
                    view.setRegion(region);
                }
            }
        }
        getLogger().logOperationResult(result);
        refreshStatus();
    }

    public CredentialProfileEntity getLastSelectedProfileEntry(FunctionConnectorModel functionConnectorModel, List<CredentialProfileEntity> credentialProfiles) {
        String lastSelectedCredentialProfileName = getLastSelectedCredentialProfileName();
        CredentialProfileEntity profileEntry = getProfileEntry(lastSelectedCredentialProfileName, credentialProfiles);
        return profileEntry != null ? profileEntry : getProfileEntry(functionConnectorModel.getCredentialProfileName(), credentialProfiles);
    }

    private CredentialProfileEntity getProfileEntry(String profileName, List<CredentialProfileEntity> credentialProfiles) {
        for(CredentialProfileEntity profileEntity: credentialProfiles) {
            if(profileEntity.getName().equals(profileName))
                return profileEntity;
        }
        return null;
    }

    @Override
    public void refreshStatus() {
        CredentialProfileEntity credentialProfileEntity = view.getSelectedCredentialProfileEntity();
        String credentialProfile = credentialProfileEntity != null ? credentialProfileEntity.toString() : null;
        RegionEntity regionEntity = view.getSelectedRegionEntity();
        String region = regionEntity != null ? regionEntity.getName() : null;
        String regionDescription = regionEntity != null ? regionEntity.toString() : null;
        FunctionEntity functionEntity = view.getSelectedFunctionEntity();
        String function = functionEntity != null ? functionEntity.toString() : null;
        ArtifactEntity artifactEntity = view.getSelectedArtifactEntity();
        String artifact = artifactEntity != null ? artifactEntity.toString() : null;
        String proxyDetails = getFunctionConnectorModel().getProxyDetails();
        view.refreshStatus(function, artifact, region, regionDescription, credentialProfile, proxyDetails);
    }

    @Override
    public void refreshAll() {
        getLogger().logDebug("Refresh all.");
        shutdownConnectorModel();
        refreshArtifactList();
        refreshRegionList();
        refreshCredentialProfilesList();
        refreshFunctionList();
        FunctionEntity functionEntity = view.getSelectedFunctionEntity();
        refreshFunctionConfiguration(functionEntity);
        if(autoRefreshAwsLog) {
            refreshAwsLogStreamList(functionEntity, FunctionConnectorModel.AwsLogRequestMode.NewRequest);
        } else {
            view.clearAwsLogStreamList();
        }
        refreshStatus();
    }

    @Override
    public void setAwsLogStreamEventList(AwsLogStreamEntity entity) {
        if(entity == null) {
            getLogger().logDebug("Clear AWS Log Stream Event list for not selected function.");
            view.clearAwsLogStreamEventList();
            return;
        }
        getLogger().logDebug("Refresh AWS Log Stream Even list.");
        OperationValueResult<List<AwsLogStreamEventEntity>> getAwsLogEventsResult = getFunctionConnectorModel()
                .getAwsLogStreamEventsFor(entity);
        if(getAwsLogEventsResult.failed()) {
            getLogger().logOperationResult(getAwsLogEventsResult);
            return;
        }
        view.setAwsLogStreamEventList(getAwsLogEventsResult.getValue());
        getLogger().logDebug("Refreshed AWS Log Stream Event list.");
    }

    @Override
    public void setAutoRefreshAwsLog(boolean autoRefresh) {
        autoRefreshAwsLog = autoRefresh;
        if(autoRefreshAwsLog) {
            refreshAwsLogStreams();
        }
    }

    @Override
    public void refreshAwsLogStreams() {
        refreshAwsLogStreamList(view.getSelectedFunctionEntity(), FunctionConnectorModel.AwsLogRequestMode.NewRequest);
    }

    @Override
    public void runGetNextAwsLogStreamSet() {
        refreshAwsLogStreamList(view.getSelectedFunctionEntity(), FunctionConnectorModel.AwsLogRequestMode.RequestNextSet);
    }

    @Override
    public boolean initializeFunctionRoleList() {
        if (roleListLoaded()) {
            return false;
        }
        OperationResult result = getRoleConnectorModel().loadRoles();
        getLogger().logOperationResult(result);
        refreshRolesList();
        return true;
    }

    @Override
    public void deleteAwsLogStreams() {
        FunctionEntity functionEntity = view.getSelectedFunctionEntity();
        if(functionEntity == null) {
            return;
        }
        view.clearAwsLogStreamList();

        OperationValueResult<List<AwsLogStreamEntity>> deleteAwsLogEventsResult = getFunctionConnectorModel()
                .deleteAwsLogStreamsFor(functionEntity.getFunctionName());
        if(deleteAwsLogEventsResult.failed()) {
            getLogger().logOperationResult(deleteAwsLogEventsResult);
            view.showError(deleteAwsLogEventsResult.getErrorAsString());
            return;
        }
        getLogger().logDebug("Deleted AWS Log Stream list.");
    }

    @Override
    public void reformatJsonFunctionTestInput(String jsonText) {
        OperationValueResult<String> result = jsonHelper.Reformat(jsonText);
        if(result.success()) {
            getLogger().logDebug("Reformat input JSON");
            view.setFunctionTestInput(result.getValue());
            return;
        }
        getLogger().logError("Reformat JSON input failed:\n%s", result.getErrorAsString());
    }

    @Override
    public void reformatJsonFunctionTestOutput(String jsonText) {
        OperationValueResult<String> result = jsonHelper.Reformat(jsonText);
        if(result.success()) {
            getLogger().logDebug("Reformatted output JSON");
            view.setFunctionTestOutput(result.getValue());
            return;
        }
        getLogger().logError("Reformat JSON output failed:\n%s", result.getErrorAsString());
    }

    @Override
    public void setAwsLogStreamEvent(AwsLogStreamEventEntity entity) {
        view.setAwsLogStreamEvent(DateTimeHelper.toFormattedString(entity.getTimeStamp()), entity.getMessage());
    }

    private void refreshAwsLogStreamList(FunctionEntity functionEntity, FunctionConnectorModel.AwsLogRequestMode awsLogRequestMode) {
        if(functionEntity == null) {
            getLogger().logDebug("Clear AWS Log Stream list for not selected function.");
            view.clearAwsLogStreamList();
            return;
        }
        getLogger().logDebug("Refresh AWS Log Stream list.");
        view.clearAwsLogStreamEventList();
        OperationValueResult<List<AwsLogStreamEntity>> getAwsLogEventsResult = getFunctionConnectorModel()
                                                                .getAwsLogStreamsFor(functionEntity.getFunctionName(),
                                                                        awsLogRequestMode);
        if(getAwsLogEventsResult.failed()) {
            getLogger().logOperationResult(getAwsLogEventsResult);
            return;
        }
        view.setAwsLogStreamList(getAwsLogEventsResult.getValue());
        getLogger().logDebug("Refreshed AWS Log Stream list.");
    }

    @Override
    public void refreshArtifactList() {
        if(projectModel == null) {
            return;
        }
        getLogger().logDebug("Refresh artifact list.");
        String lastSelectedJarArtifactName = getLastSelectedArtifactName();

        List<? extends ArtifactEntity> jarArtifacts = projectModel.getArtifacts();
        jarArtifacts.sort(Comparator.comparing(ArtifactEntity::getName));

        ArtifactEntity selectedArtifactEntity = getSelectedArtifactEntity(lastSelectedJarArtifactName, jarArtifacts);
        view.setArtifactList(jarArtifacts, selectedArtifactEntity);

        if(jarArtifacts.size() == 0) {
            getLogger().logInfo("No artifacts found.");
            refreshStatus();
            return;
        }

        ArtifactEntity artifactEntity = view.getSelectedArtifactEntity();
        if(artifactEntity == null) {
            getLogger().logInfo("Artifact is not selected.");
            refreshStatus();
            return;
        }

        getLogger().logInfo("Selected artifact: \"%s\"", artifactEntity.getName());
        String outputFilePath = artifactEntity.getOutputFilePath();
        if(!new File(outputFilePath).exists()){
            getLogger().logError("Artifact file does not exist with the path:\n%s", outputFilePath);
        }
        refreshStatus();
    }

    @Nullable
    public ArtifactEntity getSelectedArtifactEntity(String lastSelectedJarArtifactName, List<? extends ArtifactEntity> jarArtifacts) {
        ArtifactEntity selectedArtifactEntity = null;
        for(ArtifactEntity entity : jarArtifacts){
            if(entity.getName().equals(lastSelectedJarArtifactName)){
                return entity;
            }
        }
        return null;
    }

    public String getLastSelectedArtifactName() {
        ConnectorSettings connectorSettings = getConnectorSettings();
        String lastSelectedJarArtifactName = connectorSettings.getLastSelectedJarArtifactName();
        if(isEmpty(lastSelectedJarArtifactName)) {
            lastSelectedJarArtifactName = connectorSettings.getLastSelectedJarArtifactName();
        }
        FunctionEntity selectedFunctionEntity = view.getSelectedFunctionEntity();
        if (selectedFunctionEntity == null) {
            return lastSelectedJarArtifactName;
        }
        //left for backward compatibility transition
        String lastSelectedArtifactForSelectedFunction = connectorSettings.getLastSelectedJarArtifactNameForFunction(
                        selectedFunctionEntity.getFunctionName());
        if(isEmpty(lastSelectedArtifactForSelectedFunction)) {
            lastSelectedArtifactForSelectedFunction = connectorSettings.getLastSelectedArtifactNameForFunction(
                    selectedFunctionEntity.getFunctionName());
        }
        return TextUtils.isEmpty(lastSelectedArtifactForSelectedFunction)
                ? lastSelectedJarArtifactName
                : lastSelectedArtifactForSelectedFunction;
    }

    @Override
    public void setRegion(RegionEntity regionEntity) {
        Regions region = tryGetRegionBy(regionEntity.getName());
        if(region == null) {
            return;
        }
        setRegionAndProfile(getConnectorSettings().getLastSelectedCredentialProfile(), region.getName());
    }

    private void setRegionAndProfile(String credentialProfile, String regionName) {
        getLogger().logInfo("Region is set to: %s", regionName);
        getLogger().logInfo("Profile is set to: %s", credentialProfile);
        reCreateModels(credentialProfile, regionName);
        getConnectorSettings().setLastSelectedRegionName(regionName);
        refreshFunctionList();
    }

    private void refreshRolesList() {
        getLogger().logDebug("Refresh role list.");
        FunctionEntity selectedFunctionEntity = view.getSelectedFunctionEntity();
        String currentFunctionRoleArn = selectedFunctionEntity == null ? null : selectedFunctionEntity.getRoleArn();
        RoleConnectorModel roleConnectorModel = getRoleConnectorModel();
        List<RoleEntity> roleEntities = new ArrayList<>();
        int count = 0;
        RoleEntity selectedFunctionRoleEntity = null;
        for (RoleEntity entity : roleConnectorModel.getRoles()) {
            //Check if applicable to Lambda?
            roleEntities.add(entity);
            count++;
            if(entity.getArn().equals(currentFunctionRoleArn)) {
                selectedFunctionRoleEntity = entity;
            }
        }
        getLogger().logDebug("Found %d roles.", count);
        if(!isEmpty(currentFunctionRoleArn) && selectedFunctionRoleEntity == null) {
            roleEntities.add(selectedFunctionRoleEntity = roleConnectorModel.addRole(currentFunctionRoleArn));
            getLogger().logDebug("Added a role \"%s\" from a current function.", currentFunctionRoleArn);
        }
        view.setRoleList(roleEntities, selectedFunctionRoleEntity);
    }

    @Override
    public void setCredentialProfile(CredentialProfileEntity credentialProfileEntity) {
        BasicProfile basicProfile = credentialProfileEntity.getBasicProfile();
        getConnectorSettings().setLastSelectedCredentialProfile(basicProfile.getProfileName());
        shutdownConnectorModel();
        refreshCredentialProfilesList();
    }

    @Override
    public void setFunction(FunctionEntity functionEntity) {
        if(functionEntity != null) {
            String functionName = functionEntity.getFunctionName();
            getConnectorSettings().setLastSelectedFunctionName(functionName);
            getLogger().logDebug("Set function %s.", functionName);
        } else {
            getLogger().logDebug("Function not set.");
        }
        refreshRolesList();
        if (functionEntity != null) {
            view.updateFunctionEntity(functionEntity);
        }
        view.setFunctionConfiguration(functionEntity);
        if(autoRefreshAwsLog) {
            refreshAwsLogStreamList(functionEntity, FunctionConnectorModel.AwsLogRequestMode.NewRequest);
        } else {
            view.clearAwsLogStreamList();
        }
        refreshArtifactList();
        refreshStatus();
    }

    @Override
    public void setArtifact(ArtifactEntity artifactEntity) {
        getLogger().logDebug("Set JAR-artifact.");
        getConnectorSettings().setLastSelectedArtifactName(artifactEntity.getName());
        refreshStatus();
    }

    @Override
    public void runFunctionTest(String inputText, boolean autoFormatOutput) {
        FunctionEntity functionEntity = view.getSelectedFunctionEntity();
        if(functionEntity == null) {
            getLogger().logError("Cannot runChangeAutoRefreshAwsLog function - function is not selected.");
            return;
        }
        if(isEmpty(inputText.trim())){
            getLogger().logError("Cannot runChangeAutoRefreshAwsLog function \"%s\" - input is empty.", functionEntity.getFunctionName());
            return;
        }
        getLogger().logDebug("Run function \"%s\".", functionEntity.getFunctionName());
        OperationValueResult<String> result = getFunctionConnectorModel().invokeFunction(functionEntity.getFunctionName(), inputText);
        if(result.hasInfo()){
            getLogger().logInfo(result.getInfoAsString());
        }
        if(result.failed()) {
            getLogger().logError("Run function test failed:\n%s", result.getErrorAsString());
        }
        String testOutput = result.getValue();
        if(!autoFormatOutput) {
            view.setFunctionTestOutput(testOutput);
            return;
        }
        OperationValueResult<String> autoFormatResult = jsonHelper.Reformat(testOutput);
        if(autoFormatResult.success()) {
            getLogger().logDebug("JSON output has been auto-formatted.");
            view.setFunctionTestOutput(autoFormatResult.getValue());
            return;
        }
        getLogger().logError("Auto-formatting of JSON output failed:\n%s", autoFormatResult.getErrorAsString());

    }

    @Override
    public void openTestFunctionInputFile(File file) {
        try {
            String filePath = file.getCanonicalPath();
            getLogger().logDebug("Read function test input from file: %s", filePath);
            long codeFileSize = file.length();
            if(codeFileSize > 1048576){
                getLogger().logError("Code file sized exceeds 1MB limit - its size is %d bytes", codeFileSize);
                return;
            }
            byte[] buffer = Files.readAllBytes(file.toPath());
            String inputText = new String(buffer);
            view.setFunctionTestInput(inputText);
            for(TestFunctionInputEntity entity : testFunctionInputRecentEntityList){
                if(entity.getFilePath().equals(filePath)){
                    testFunctionInputRecentEntityList.remove(entity);
                    break;
                }
            }
            testFunctionInputRecentEntityList.add(new TestFunctionInputEntity(filePath, file.getName(), inputText));
            view.setTestFunctionInputRecentEntityList(testFunctionInputRecentEntityList);
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().logError("Cannot open of an input file: %s", e.getMessage());
        }
    }

    @Override
    public String getLastSelectedTestFunctionInputFilePath() {
        String filePath = getConnectorSettings().getLastSelectedTestFunctionInputFilePath();
        return isEmpty(filePath) ? "" : filePath;
    }

    @Override
    public void setLastSelectedTestFunctionInputFilePath(String path) {
        getConnectorSettings().setLastSelectedTestFunctionInputFilePath(path);
    }

    @Override
    public void setSetTestFunctionInputFromRecent(TestFunctionInputEntity entity) {
        view.setFunctionTestInput(entity.getInputText());
    }

    @Override
    public void setProxySettings() {
        FunctionConnectorModel functionConnectorModel = this.getFunctionConnectorModel();
        reCreateModels(functionConnectorModel.getCredentialProfileName(), functionConnectorModel.getRegionName());
        refreshStatus();
    }

    @Override
    public void refreshFunctionConfiguration() {
        FunctionEntity functionEntity = view.getSelectedFunctionEntity();
        refreshFunctionConfiguration(functionEntity);
    }

    private void refreshFunctionConfiguration(FunctionEntity functionEntity) {
        getLogger().logDebug("Update function configuration.");
        if(functionEntity == null) {
            showError("No function selected to refresh its configuration.");
            return;
        }
        final OperationValueResult<FunctionEntity> result = getFunctionConnectorModel().getFunctionBy(functionEntity.getFunctionName());
        if (!result.success()) {
            showError(result.getErrorAsString());
            return;
        }
        setFunction(result.getValue());
    }

    private Regions tryGetRegionBy(String regionName) {
        for (Regions region : Regions.values()){
            if(region.getName().equals(regionName)){
                return region;
            }
        }
        return null;
    }

    @Override
    @NotNull
    protected Regions getLastSelectedRegion() {
        String lastSelectedRegionName = getConnectorSettings().getLastSelectedRegionName();
        Regions region = isEmpty(lastSelectedRegionName)
                            ? Regions.DEFAULT_REGION
                            : tryGetRegionBy(lastSelectedRegionName);
        return region != null ? region : Regions.DEFAULT_REGION;
    }

    @Override
    @NotNull
    protected String getLastSelectedCredentialProfileName() {
        String lastSelectedCredentialProfile = getConnectorSettings().getLastSelectedCredentialProfile();
        String credentialProfile = lastSelectedCredentialProfile == null
                            ? Constant.CredentialProfile.DEFAULT
                            : lastSelectedCredentialProfile;
        return credentialProfile != null ? credentialProfile : Constant.CredentialProfile.DEFAULT;
    }

}
