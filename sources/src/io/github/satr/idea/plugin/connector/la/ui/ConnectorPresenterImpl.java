package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2017, github.com/satr, MIT License

import com.amazonaws.auth.profile.internal.BasicProfile;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.model.Runtime;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import io.github.satr.common.Constant;
import io.github.satr.common.OperationResult;
import io.github.satr.common.OperationResultImpl;
import io.github.satr.common.OperationValueResult;
import io.github.satr.idea.plugin.connector.la.entities.ArtifactEntry;
import io.github.satr.idea.plugin.connector.la.entities.CredentialProfileEntry;
import io.github.satr.idea.plugin.connector.la.entities.FunctionEntry;
import io.github.satr.idea.plugin.connector.la.entities.RegionEntry;
import io.github.satr.idea.plugin.connector.la.models.ConnectorSettings;
import io.github.satr.idea.plugin.connector.la.models.ProjectModel;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.github.satr.common.MessageHelper.showError;
import static io.github.satr.common.MessageHelper.showInfo;
import static io.github.satr.common.StringUtil.getNotEmptyString;
import static org.apache.http.util.TextUtils.isEmpty;

public class ConnectorPresenterImpl extends AbstractConnectorPresenter implements ConnectorPresenter {
    private final Regions DEFAULT_REGION = Regions.US_EAST_1;
    private ConnectorSettings connectorSettings = ConnectorSettings.getInstance();
    private ConnectorView view;

    @Override
    public void setView(ConnectorView view) {
        this.view = view;
    }

    @Override
    public void refreshFunctionList() {
        ArrayList<String> functionNames = new ArrayList<>();
        List<FunctionEntry> functions = getConnectorModel().getFunctions();
        String lastSelectedFunctionName = connectorSettings.getLastSelectedFunctionName();
        FunctionEntry selectedFunctionEntry = null;
        for (FunctionEntry entry : functions) {
            if (!entry.getRuntime().equals(Runtime.Java8)) {
                continue;
            }
            functionNames.add(entry.getFunctionName());
            if(entry.getFunctionName().equals(lastSelectedFunctionName)){
                selectedFunctionEntry = entry;
            }
        }
        connectorSettings.setFunctionNames(functionNames);
        view.setFunctionList(functions, selectedFunctionEntry);
        FunctionEntry functionEntry = view.getSelectedFunctionEntry();
        refreshStatus();
    }

    @Override
    public void updateFunction(Project project) {
        FunctionEntry functionEntry = view.getSelectedFunctionEntry();
        ArtifactEntry artifactEntry = view.getSelectedArtifactEntry();
        OperationResult validationResult = validateToUpdate(functionEntry, artifactEntry);
        if(validationResult.failed()){
            showError(project, validationResult.getErrorAsString());
            return;
        }
        String functionName = functionEntry.getFunctionName();
        String artifactFilePath = artifactEntry.getOutputFilePath();
        final OperationValueResult<FunctionEntry> result = getConnectorModel().updateWithJar(functionName, artifactFilePath);
        if (!result.success()) {
            showError(project, result.getErrorAsString());
            return;
        }
        connectorSettings.setLastSelectedFunctionName(functionName);
        connectorSettings.setLastSelectedJarArtifactName(artifactEntry.getName());
        showInfo(project, "Lambda function \"%s\" has been updated with the artifact \"%s\".",
                            result.getValue().getFunctionName(), artifactEntry.getName());
    }

    private OperationResult validateToUpdate(FunctionEntry functionEntry, ArtifactEntry artifactEntry) {
        OperationResultImpl result = new OperationResultImpl();
        if(functionEntry == null){
            result.addError("Function is not selected.");
        }
        if(artifactEntry == null){
            result.addError("JAR-artifact is not selected.");
        }
        if(result.failed()) {
            return result;
        }
        if(!new File(artifactEntry.getOutputFilePath()).exists()){
            result.addError("JAR-artifact file does not exist.");
        }
        return result;
    }

    @Override
    public void shutdown() {
        shutdownConnectorModel();
    }

    @Override
    public void refreshRegionList(Project project) {
        view.setRegionList(getConnectorModel().getRegions(), getLastSelectedRegion());
        refreshStatus();
    }

    @Override
    public void refreshCredentialProfilesList(Project project) {
        view.setCredentialProfilesList(getConnectorModel().getCredentialProfiles(), getLastSelectedCredentialProfile());
        refreshStatus();
    }

    @Override
    public void refreshStatus() {
        CredentialProfileEntry credentialProfileEntry = view.getSelectedCredentialProfileEntry();
        String credentialProfile = credentialProfileEntry != null ? credentialProfileEntry.toString() : null;
        RegionEntry regionEntry = view.getSelectedRegionEntry();
        String region = regionEntry != null ? regionEntry.getName() : null;
        String regionDescription = regionEntry != null ? regionEntry.toString() : null;
        FunctionEntry functionEntry = view.getSelectedFunctionEntry();
        String function = functionEntry != null ? functionEntry.toString() : null;
        ArtifactEntry artifactEntry = view.getSelectedArtifactEntry();
        String artifact = artifactEntry != null ? artifactEntry.toString() : null;
        view.refreshStatus(function, artifact, region, regionDescription, credentialProfile);
    }

    @Override
    public void refreshAllLists(Project project) {
        refreshJarArtifactList(project);
        refreshRegionList(project);
        refreshCredentialProfilesList(project);
        refreshFunctionList();
    }

    @Override
    public void refreshJarArtifactList(Project project) {
        ProjectModel projectModel = ServiceManager.getService(ProjectModel.class);
        String lastSelectedJarArtifactName = connectorSettings.getLastSelectedJarArtifactName();
        ArtifactEntry selectedArtifactEntry = null;
        Collection<? extends ArtifactEntry> jarArtifacts = projectModel.getJarArtifacts(project);
        for(ArtifactEntry entry : jarArtifacts){
            if(entry.getName().equals(lastSelectedJarArtifactName)){
                selectedArtifactEntry = entry;
                break;
            }
        }
        view.setArtifactList(jarArtifacts, selectedArtifactEntry);
        refreshStatus();
    }

    @Override
    public void setRegion(RegionEntry regionEntry) {
        Regions region = tryGetRegionBy(regionEntry.getName());
        if(region == null)
            return;
        setRegionAndProfile(region, connectorSettings.getLastSelectedCredentialProfile());
    }

    private void setRegionAndProfile(Regions region, String credentialProfile) {
        reCreateConnectorModel(region, credentialProfile);
        connectorSettings.setLastSelectedRegionName(region.getName());
        refreshFunctionList();
    }

    @Override
    public void setCredentialProfile(CredentialProfileEntry credentialProfileEntry) {
        BasicProfile basicProfile = credentialProfileEntry.getBasicProfile();
        Regions lastSelectedRegion = getLastSelectedRegion();
        Regions region = lastSelectedRegion;
        if(!isEmpty(basicProfile.getRegion())) {
            region = tryGetRegionBy(basicProfile.getRegion());
        }
        if(region == null){
            region = lastSelectedRegion;
        }
        String credentialProfile = credentialProfileEntry.getName();
        connectorSettings.setLastSelectedCredentialProfile(credentialProfile);
        setRegionAndProfile(region, credentialProfile);
        if(!lastSelectedRegion.getName().equals(region.getName())){
            view.setRegion(region);
        }
        refreshStatus();
    }

    @Override
    public void setFunction(FunctionEntry functionEntry) {
        connectorSettings.setLastSelectedFunctionName(functionEntry.getFunctionName());
        refreshStatus();
    }

    @Override
    public void setJarArtifact(ArtifactEntry artifactEntry) {
        connectorSettings.setLastSelectedFunctionName(artifactEntry.getName());
        refreshStatus();
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

    @NotNull
    protected String getLastSelectedFunction() {
        return getNotEmptyString(connectorSettings.getLastSelectedFunctionName());
    }

    @NotNull
    protected String getLastSelectedJarArtifact() {
        return getNotEmptyString(connectorSettings.getLastSelectedJarArtifactName());
    }
}
