package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2018, github.com/satr, MIT License

import com.amazonaws.auth.profile.internal.BasicProfile;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.model.Runtime;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import io.github.satr.common.*;
import io.github.satr.idea.plugin.connector.la.entities.*;
import io.github.satr.idea.plugin.connector.la.models.ConnectorModel;
import io.github.satr.idea.plugin.connector.la.models.ConnectorSettings;
import io.github.satr.idea.plugin.connector.la.models.ProjectModel;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.http.util.TextUtils.isEmpty;

public class ConnectorPresenterImpl extends AbstractConnectorPresenter implements ConnectorPresenter {
    private final Regions DEFAULT_REGION = Regions.US_EAST_1;
    private ConnectorSettings connectorSettings = ConnectorSettings.getInstance();
    private ConnectorView view;
    private List<TestFunctionInputEntity> testFunctionInputRecentEntityList = new ArrayList<>();

    @Override
    public void setView(ConnectorView view) {
        this.view = view;
    }

    @Override
    public void refreshFunctionList() {
        view.logDebug("Refresh function list.");
        ArrayList<String> functionNames = new ArrayList<>();
        OperationValueResult<List<FunctionEntity>> functionListResult = getConnectorModel().getFunctions();
        view.log(functionListResult);
        refreshRolesList();

        String lastSelectedFunctionName = connectorSettings.getLastSelectedFunctionName();
        FunctionEntity selectedFunctionEntity = null;
        int functionCount = 0;
        List<FunctionEntity> functionList = functionListResult.getValue();
        for (FunctionEntity entity : functionList) {
            if (!entity.getRuntime().equals(Runtime.Java8)) {
                continue;
            }
            functionNames.add(entity.getFunctionName());
            if(entity.getFunctionName().equals(lastSelectedFunctionName)){
                selectedFunctionEntity = entity;
            }
            functionCount++;
        }
        view.logDebug("Found %d Java-8 functions.", functionCount);
        connectorSettings.setFunctionNames(functionNames);
        view.setFunctionList(functionList, selectedFunctionEntity);
        FunctionEntity functionEntity = view.getSelectedFunctionEntity();
        if(functionEntity == null) {
            view.logInfo("Function is not selected.");
        } else {
            view.logInfo("Selected function: \"%s\"", functionEntity.getFunctionName());
        }
        view.setFunctionConfiguration(functionEntity);
        refreshStatus();
    }

    @Override
    public void updateFunction(Project project) {
        view.logDebug("Update function.");
        FunctionEntity functionEntity = view.getSelectedFunctionEntityWithUpdateConfiguration();
        if(functionEntity.isChanged()){
            OperationResult updateFunctionConfigurationResult = getConnectorModel().updateConfiguration(functionEntity);
            if(updateFunctionConfigurationResult.failed()){
                showError(project, "Lambda function configuration update failed: \"%s\"",
                        updateFunctionConfigurationResult.getErrorAsString());
                return;
            }
        }
        updateFunctionCode(project, functionEntity);
    }

    private void updateFunctionCode(Project project, FunctionEntity functionEntity) {
        ArtifactEntity artifactEntity = view.getSelectedArtifactEntity();
        OperationResult validationResult = validateToUpdate(functionEntity, artifactEntity);
        if(validationResult.failed()){
            showError(project, validationResult.getErrorAsString());
            return;
        }
        String functionName = functionEntity.getFunctionName();
        String artifactFilePath = artifactEntity.getOutputFilePath();
        final OperationValueResult<FunctionEntity> result = getConnectorModel().updateWithJar(functionEntity, artifactFilePath);
        if (!result.success()) {
            showError(project, result.getErrorAsString());
            return;
        }
        connectorSettings.setLastSelectedFunctionName(functionName);
        connectorSettings.setLastSelectedJarArtifactName(artifactEntity.getName());
        FunctionEntity updatedFunctionEntity = result.getValue();
        view.updateFunctionEntity(updatedFunctionEntity);
        setFunction(updatedFunctionEntity);
        showInfo(project, "Lambda function \"%s\" has been updated with the JAR-artifact \"%s\".",
                            updatedFunctionEntity.getFunctionName(), artifactEntity.getName());
    }

    private void showError(Project project, String format, Object... args) {
        MessageHelper.showError(project, format, args);
        view.logError(format, args);
    }

    private void showInfo(Project project, String format, Object... args) {
        MessageHelper.showInfo(project, format, args);
        view.logInfo(format, args);
    }

    private OperationResult validateToUpdate(FunctionEntity functionEntity, ArtifactEntity artifactEntity) {
        OperationResultImpl result = new OperationResultImpl();
        if(functionEntity == null){
            result.addError("Function is not selected.");
        }
        if(artifactEntity == null){
            result.addError("JAR-artifact is not selected.");
        }
        if(result.failed()) {
            return result;
        }
        if(!new File(artifactEntity.getOutputFilePath()).exists()){
            result.addError("JAR-artifact file does not exist.");
        }
        return result;
    }

    @Override
    public void shutdown() {
        view.logDebug("Shutdown.");
        shutdownConnectorModel();
    }

    @Override
    public void refreshRegionList(Project project) {
        view.logDebug("Refresh region list.");
        view.setRegionList(getConnectorModel().getRegions(), getLastSelectedRegion());
    }

    @Override
    public void refreshTracingModeList() {
        view.logDebug("Refresh trace mode list.");
        view.setTracingModeList(TracingModeEntity.values());
    }

    @Override
    public void refreshCredentialProfilesList(Project project) {
        view.logDebug("Refresh credential profile list.");
        OperationValueResult<List<CredentialProfileEntity>> credentialProfilesResult = getConnectorModel().getCredentialProfiles();
        List<CredentialProfileEntity> credentialProfiles = credentialProfilesResult.getValue();
        view.setCredentialProfilesList(credentialProfiles, getLastSelectedCredentialProfile());
        view.log(credentialProfilesResult);
        refreshStatus();
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
        String proxyDetails = getConnectorModel().getProxyDetails();
        view.refreshStatus(function, artifact, region, regionDescription, credentialProfile, proxyDetails);
    }

    @Override
    public void refreshAll(Project project) {
        view.logDebug("Refresh all.");
        refreshJarArtifactList(project);
        refreshRegionList(project);
        refreshCredentialProfilesList(project);
        refreshFunctionList();
        refreshStatus();
    }

    @Override
    public void refreshJarArtifactList(Project project) {
        if(project == null) {
            return;
        }
        view.logDebug("Refresh JAR-artifact list.");
        ProjectModel projectModel = ServiceManager.getService(ProjectModel.class);
        String lastSelectedJarArtifactName = connectorSettings.getLastSelectedJarArtifactName();
        ArtifactEntity selectedArtifactEntity = null;
        Collection<? extends ArtifactEntity> jarArtifacts = projectModel.getJarArtifacts(project);
        for(ArtifactEntity entity : jarArtifacts){
            if(entity.getName().equals(lastSelectedJarArtifactName)){
                selectedArtifactEntity = entity;
                break;
            }
        }
        view.setArtifactList(jarArtifacts, selectedArtifactEntity);

        if(jarArtifacts.size() == 0) {
            view.logInfo("No JAR-artifacts found.");
            refreshStatus();
            return;
        }

        ArtifactEntity artifactEntity = view.getSelectedArtifactEntity();
        if(artifactEntity == null) {
            view.logInfo("JAR-artifact is not selected.");
            refreshStatus();
            return;
        }

        view.logInfo("Selected JAR-artifact: \"%s\"", artifactEntity.getName());
        String outputFilePath = artifactEntity.getOutputFilePath();
        if(!new File(outputFilePath).exists()){
            view.logError("JAR-artifact file does not exist with the path:\n%s", outputFilePath);
        }
        refreshStatus();
    }

    @Override
    public void setRegion(RegionEntity regionEntity) {
        Regions region = tryGetRegionBy(regionEntity.getName());
        if(region == null) {
            return;
        }
        setRegionAndProfile(region, connectorSettings.getLastSelectedCredentialProfile());
    }

    private void setRegionAndProfile(Regions region, String credentialProfile) {
        view.logInfo("Region is set to: %s", region.toString());
        view.logInfo("Profile is set to: %s", credentialProfile);
        reCreateConnectorModel(region, credentialProfile);
        connectorSettings.setLastSelectedRegionName(region.getName());
        refreshFunctionList();
    }

    private void refreshRolesList() {
        view.logDebug("Refresh role list.");
        ArrayList<String> roleNames = new ArrayList<>();
        List<RoleEntity> roles = getConnectorModel().getRoles();
        int count = 0;
        for (RoleEntity entity : roles) {
            //Check if applicable to Lambda?
            roleNames.add(entity.getName());
            count++;
        }
        view.logDebug("Found %d roles.", count);
        view.setRoleList(roles);
    }

    @Override
    public void setCredentialProfile(CredentialProfileEntity credentialProfileEntity) {
        BasicProfile basicProfile = credentialProfileEntity.getBasicProfile();
        Regions lastSelectedRegion = getLastSelectedRegion();
        Regions region = lastSelectedRegion;
        if(!isEmpty(basicProfile.getRegion())) {
            region = tryGetRegionBy(basicProfile.getRegion());
        }
        if(region == null){
            region = lastSelectedRegion;
        }
        String credentialProfile = credentialProfileEntity.getName();
        connectorSettings.setLastSelectedCredentialProfile(credentialProfile);
        setRegionAndProfile(region, credentialProfile);
        if(!lastSelectedRegion.getName().equals(region.getName())){
            view.setRegion(region);
        }
        CredentialProfileEntity profileEntity = view.getSelectedCredentialProfileEntity();
        if(profileEntity == null) {
            view.logInfo("Credential profile is not selected.");
        } else {
            view.logInfo("Selected Credential profile: \"%s\"", profileEntity.getName());
        }
        refreshStatus();
    }

    @Override
    public void setFunction(FunctionEntity functionEntity) {
        if(functionEntity != null) {
            String functionName = functionEntity.getFunctionName();
            connectorSettings.setLastSelectedFunctionName(functionName);
            view.logDebug("Set function %s.", functionName);
        } else {
            view.logDebug("Function not set.");
        }
        view.setFunctionConfiguration(functionEntity);
        refreshStatus();
    }

    @Override
    public void setJarArtifact(ArtifactEntity artifactEntity) {
        view.logDebug("Set JAR-artifact.");
        connectorSettings.setLastSelectedFunctionName(artifactEntity.getName());
        refreshStatus();
    }

    @Override
    public void runFunctionTest(Project project, String inputText) {
        FunctionEntity functionEntity = view.getSelectedFunctionEntity();
        if(functionEntity == null) {
            view.logError("Cannot run function - function is not selected.");
            return;
        }
        if(isEmpty(inputText.trim())){
            view.logError("Cannot run function \"%s\" - input is empty.", functionEntity.getFunctionName());
            return;
        }
        view.logDebug("Run function \"%s\".", functionEntity.getFunctionName());
        OperationValueResult<String> result = getConnectorModel().invokeFunction(functionEntity.getFunctionName(), inputText);
        if(result.hasInfo()){
            view.logInfo(result.getInfoAsString());
        }
        if(result.failed()) {
            view.logError("Run function test failed:\n%s", result.getErrorAsString());
        }
        view.setFunctionTestOutput(result.getValue());
    }

    @Override
    public void openTestFunctionInputFile(File file) {
        try {
            String filePath = file.getCanonicalPath();
            view.logDebug("Read function test input from file: %s", filePath);
            byte[] buffer = Files.readAllBytes(file.toPath());
            String inputText = new String(buffer);
            view.setTestFunctionInput(inputText);
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
            view.logError(e);
        }
    }

    @Override
    public String getLastSelectedTestFunctionInputFilePath() {
        String filePath = connectorSettings.getLastSelectedTestFunctionInputFilePath();
        return isEmpty(filePath) ? "" : filePath;
    }

    @Override
    public void setLastSelectedTestFunctionInputFilePath(String path) {
        connectorSettings.setLastSelectedTestFunctionInputFilePath(path);
    }

    @Override
    public void setSetTestFunctionInputFromRecent(TestFunctionInputEntity entity) {
        view.setTestFunctionInput(entity.getInputText());
    }

    @Override
    public void setProxySettings() {
        ConnectorModel model = this.getConnectorModel();
        reCreateConnectorModel(model.getRegion(), model.getCredentialProfileName());
        refreshStatus();
    }

    @Override
    public void refreshFunctionConfiguration(Project project) {
        view.logDebug("Update function configuration.");
        FunctionEntity functionEntity = view.getSelectedFunctionEntity();
        if(functionEntity == null) {
            showError(project, "No function selected to refresh its configuration.");
            return;
        }
        final OperationValueResult<FunctionEntity> result = getConnectorModel().getFunctionBy(functionEntity.getFunctionName());
        if (!result.success()) {
            showError(project, result.getErrorAsString());
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
        String lastSelectedRegionName = connectorSettings.getLastSelectedRegionName();
        Regions region = lastSelectedRegionName == null
                            ? DEFAULT_REGION
                            : tryGetRegionBy(lastSelectedRegionName);
        return region != null ? region : DEFAULT_REGION;
    }

    @Override
    @NotNull
    protected String getLastSelectedCredentialProfile() {
        String lastSelectedCredentialProfile = connectorSettings.getLastSelectedCredentialProfile();
        String credentialProfile = lastSelectedCredentialProfile == null
                            ? Constant.CredentialProfile.DEFAULT
                            : lastSelectedCredentialProfile;
        return credentialProfile != null ? credentialProfile : Constant.CredentialProfile.DEFAULT;
    }
}
