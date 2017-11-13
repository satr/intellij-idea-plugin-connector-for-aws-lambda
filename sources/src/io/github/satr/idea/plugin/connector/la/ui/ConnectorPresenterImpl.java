package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2017, github.com/satr, MIT License

import com.amazonaws.auth.profile.internal.BasicProfile;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.model.Runtime;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import io.github.satr.common.Constant;
import io.github.satr.common.OperationValueResult;
import io.github.satr.idea.plugin.connector.la.entities.ArtifactEntry;
import io.github.satr.idea.plugin.connector.la.entities.CredentialProfileEntry;
import io.github.satr.idea.plugin.connector.la.entities.FunctionEntry;
import io.github.satr.idea.plugin.connector.la.entities.RegionEntry;
import io.github.satr.idea.plugin.connector.la.models.ConnectorModel;
import io.github.satr.idea.plugin.connector.la.models.ConnectorSettings;
import io.github.satr.idea.plugin.connector.la.models.ProjectModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static io.github.satr.common.MessageHelper.showError;
import static io.github.satr.common.MessageHelper.showInfo;
import static org.apache.http.util.TextUtils.isEmpty;

public class ConnectorPresenterImpl implements ConnectorPresenter {
    private final Regions DEFAULT_REGION = Regions.US_EAST_1;
    private ConnectorModel connectorModel;
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
        for (FunctionEntry entry : functions) {
            if (!entry.getRuntime().equals(Runtime.Java8))
                continue;
            functionNames.add(entry.getFunctionName());
        }
        connectorSettings.setFunctionNames(functionNames);
        view.setFunctionList(functions);
    }

    @Override
    public void updateFunction(FunctionEntry functionEntry, ArtifactEntry artifactEntry, Project project) {
        ProjectModel projectModel = ServiceManager.getService(ProjectModel.class);
        String functionName = functionEntry.getFunctionName();
        final OperationValueResult<FunctionEntry> result = getConnectorModel().updateWithJar(functionName, artifactEntry.getOutputFilePath());

        if (!result.success()) {
            showError(project, result.getErrorAsString());
            return;
        }
        connectorSettings.setLastSelectedFunctionName(functionName);
        connectorSettings.setLastSelectedJarArtifactName(artifactEntry.getName());
        showInfo(project, "Lambda function \"%s\" has been updated with the artifact \"%s\".", result.getValue().getFunctionName(), artifactEntry.getName());

    }

    @Override
    public void shutdown() {
        if (connectorModel != null)
            connectorModel.shutdown();
    }

    @Override
    public void refreshRegionList(Project project) {
        view.setRegionList(getConnectorModel().getRegions(), getLastSelectedRegion());
    }

    @Override
    public void refreshCredentialProfilesList(Project project) {
        view.setCredentialProfilesList(getConnectorModel().getCredentialProfiles(), getLastSelectedCredentialProfile());
    }

    @Override
    public void refreshJarArtifactList(Project project) {
        ProjectModel projectModel = ServiceManager.getService(ProjectModel.class);
        view.setArtifactList(projectModel.getJarArtifacts(project));

    }

    @Override
    public void setRegion(RegionEntry regionEntry) {
        Regions region = tryGetRegionBy(regionEntry.getName());
        if(region == null)
            return;
        setRegionAndProfile(region, connectorSettings.getLastSelectedCredentialProfile());
    }

    private void setRegionAndProfile(Regions region, String credentialProfile) {
        if(connectorModel != null)
            connectorModel.shutdown();
        connectorModel = new ConnectorModel(region, credentialProfile);
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
    }

    private Regions tryGetRegionBy(String regionName) {
        for (Regions region : Regions.values()){
            if(region.getName().equals(regionName)){
                return region;
            }
        }
        return null;
    }

    private ConnectorModel getConnectorModel() {
        if (connectorModel != null)
            return connectorModel;
        Regions region = getLastSelectedRegion();
        return connectorModel = new ConnectorModel(region, Constant.CredentialProfile.DEFAULT);
    }

    @NotNull
    private Regions getLastSelectedRegion() {
        String lastSelectedRegionName = connectorSettings.getLastSelectedRegionName();
        Regions region = lastSelectedRegionName == null
                            ? DEFAULT_REGION
                            : tryGetRegionBy(lastSelectedRegionName);
        return region != null ? region : DEFAULT_REGION;
    }

    @NotNull
    private String getLastSelectedCredentialProfile() {
        String lastSelectedCredentialProfile = connectorSettings.getLastSelectedCredentialProfile();
        String credentialProfile = lastSelectedCredentialProfile == null
                            ? Constant.CredentialProfile.DEFAULT
                            : lastSelectedCredentialProfile;
        return credentialProfile != null ? credentialProfile : Constant.CredentialProfile.DEFAULT;
    }
}
