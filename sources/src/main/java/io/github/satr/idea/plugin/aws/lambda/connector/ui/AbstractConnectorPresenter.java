package io.github.satr.idea.plugin.aws.lambda.connector.ui;

import com.amazonaws.regions.Regions;
import io.github.satr.common.CompositeLogger;
import io.github.satr.common.Logger;
import io.github.satr.idea.plugin.aws.lambda.connector.models.ConnectorSettings;
import io.github.satr.idea.plugin.aws.lambda.connector.models.FunctionConnectorModel;
import io.github.satr.idea.plugin.aws.lambda.connector.models.ProjectModel;
import io.github.satr.idea.plugin.aws.lambda.connector.models.RoleConnectorModel;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractConnectorPresenter {
    private ConnectorSettings connectorSettings = ConnectorSettings.getInstance();
    protected ProjectModel projectModel;
    private FunctionConnectorModel functionConnectorModel;
    private RoleConnectorModel roleConnectorModel;
    private final CompositeLogger logger = new CompositeLogger();

    protected ConnectorSettings getConnectorSettings() {
        return connectorSettings;
    }

    public Logger getLogger() {
        return logger;
    }

    protected void shutdownConnectorModel() {
        if(functionConnectorModel != null) {
            getLogger().logDebug("Shutdown function connector");
            functionConnectorModel.shutdown();
        }
        functionConnectorModel = null;

        if(roleConnectorModel != null) {
            getLogger().logDebug("Shutdown role connector");
            roleConnectorModel.shutdown();
        }
        roleConnectorModel = null;
    }

    @NotNull
    private FunctionConnectorModel createFunctionConnectorModel(String profileName, String regionName) {
        return new FunctionConnectorModel(regionName, profileName, getLogger());
    }

    @NotNull
    private RoleConnectorModel createRoleConnectorModel(String profileName, String regionName) {
        return new RoleConnectorModel(regionName, profileName, getLogger());
    }

    protected FunctionConnectorModel getFunctionConnectorModel() {
        if (functionConnectorModel != null) {
            return functionConnectorModel;
        }
        String lastSelectedCredentialProfileName = getLastSelectedCredentialProfileName();
        functionConnectorModel = createFunctionConnectorModel(lastSelectedCredentialProfileName, getLastSelectedRegion().getName());
        getConnectorSettings().setLastSelectedCredentialProfile(functionConnectorModel.getCredentialProfileName());
        return functionConnectorModel;
    }

    protected RoleConnectorModel getRoleConnectorModel() {
        if (roleConnectorModel != null) {
            return roleConnectorModel;
        }
        return roleConnectorModel = createRoleConnectorModel(getLastSelectedCredentialProfileName(), getLastSelectedRegion().getName());
    }

    protected void reCreateModels(String credentialProfile, String regionName) {
        shutdownConnectorModel();
        functionConnectorModel = createFunctionConnectorModel(credentialProfile, regionName);
        roleConnectorModel = createRoleConnectorModel(credentialProfile, regionName);
    }

    public void addLogger(Logger logger) {
        this.logger.addLogger(logger);
    }

    public void setProjectModel(ProjectModel projectModel) {
        this.projectModel = projectModel;
    }

    @NotNull
    protected abstract Regions getLastSelectedRegion();

    @NotNull
    protected abstract String getLastSelectedCredentialProfileName();

    public boolean roleListLoaded() {
        return roleConnectorModel.isLoaded();
    }
}
