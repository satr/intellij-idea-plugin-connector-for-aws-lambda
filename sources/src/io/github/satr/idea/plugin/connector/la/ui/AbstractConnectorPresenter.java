package io.github.satr.idea.plugin.connector.la.ui;

import com.amazonaws.regions.Regions;
import io.github.satr.common.CompositeLogger;
import io.github.satr.common.Logger;
import io.github.satr.idea.plugin.connector.la.models.ConnectorSettings;
import io.github.satr.idea.plugin.connector.la.models.FunctionConnectorModel;
import io.github.satr.idea.plugin.connector.la.models.ProjectModel;
import io.github.satr.idea.plugin.connector.la.models.RoleConnectorModel;
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
            functionConnectorModel.shutdown();
        }
        if(roleConnectorModel != null) {
            roleConnectorModel.shutdown();
        }
    }

    @NotNull
    private FunctionConnectorModel createFunctionConnectorModel(Regions region, String profileName) {
        return new FunctionConnectorModel(region, profileName);
    }

    @NotNull
    private RoleConnectorModel createRoleConnectorModel(Regions region, String profileName) {
        return new RoleConnectorModel(region, profileName);
    }

    protected FunctionConnectorModel getFunctionConnectorModel() {
        if (functionConnectorModel != null) {
            return functionConnectorModel;
        }
        return functionConnectorModel = createFunctionConnectorModel(getLastSelectedRegion(), getLastSelectedCredentialProfileName());
    }

    protected RoleConnectorModel getRoleConnectorModel() {
        if (roleConnectorModel != null) {
            return roleConnectorModel;
        }
        return roleConnectorModel = createRoleConnectorModel(getLastSelectedRegion(), getLastSelectedCredentialProfileName());
    }

    protected void reCreateModels(Regions region, String credentialProfile) {
        shutdownConnectorModel();
        functionConnectorModel = createFunctionConnectorModel(region, credentialProfile);
        roleConnectorModel = createRoleConnectorModel(region, credentialProfile);
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
