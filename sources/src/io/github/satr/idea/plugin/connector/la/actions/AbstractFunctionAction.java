package io.github.satr.idea.plugin.connector.la.actions;
// Copyright © 2017, github.com/satr, MIT License

import com.amazonaws.regions.Regions;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import io.github.satr.common.Constant;
import io.github.satr.idea.plugin.connector.la.models.ConnectorModel;
import io.github.satr.idea.plugin.connector.la.models.ConnectorSettings;

public abstract class AbstractFunctionAction extends AnAction {
    private ConnectorModel connectorModel;
    protected ConnectorSettings connectorSettings = ConnectorSettings.getInstance();

    @Override
    protected void finalize() throws Throwable {
        if(connectorModel != null)
            connectorModel.shutdown();

        super.finalize();
    }

    //TODO: enter credentials - for now it is used default credentials, setup by the aws cli
    @Override
    public abstract void actionPerformed(AnActionEvent event);

    public ConnectorModel getConnectorModel() {
        return connectorModel != null
                ? connectorModel
                : (connectorModel = new ConnectorModel(Regions.US_EAST_1, Constant.CredentialProfile.DEFAULT));
    }
}
