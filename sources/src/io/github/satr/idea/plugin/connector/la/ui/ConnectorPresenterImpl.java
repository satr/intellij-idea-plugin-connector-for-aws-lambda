package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2017, github.com/satr, MIT License

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.model.Runtime;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import io.github.satr.common.OperationValueResult;
import io.github.satr.idea.plugin.connector.la.entities.ArtifactEntry;
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
    public void refreshJarArtifactList(Project project) {
        ProjectModel projectModel = ServiceManager.getService(ProjectModel.class);
        view.setArtifactList(projectModel.getJarArtifacts(project));

    }

    @Override
    public void setRegion(RegionEntry regionEntry) {
        Regions region = Regions.fromName(regionEntry.getName());
        if(region == null)
            return;
        if(connectorModel != null)
            connectorModel.shutdown();
        connectorModel = new ConnectorModel(region);
        connectorSettings.setLastSelectedRegionName(regionEntry.getName());
        refreshFunctionList();
    }

    private ConnectorModel getConnectorModel() {
        if (connectorModel != null)
            return connectorModel;
        Regions region = getLastSelectedRegion();
        return connectorModel = new ConnectorModel(region);
    }

    @NotNull
    private Regions getLastSelectedRegion() {
        String lastSelectedRegionName = connectorSettings.getLastSelectedRegionName();
        Regions region = lastSelectedRegionName == null
                            ? DEFAULT_REGION
                            : Regions.fromName(lastSelectedRegionName);
        return region != null ? region : DEFAULT_REGION;
    }
}
