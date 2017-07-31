package io.github.satr.idea.plugin.connector.la.models;
// Copyright © 2017, github.com/satr, MIT License

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(name = "ConnectorSettings", storages = @Storage("connector-settings.xml"))
public class ConnectorSettings  implements PersistentStateComponent<ConnectorSettings> {

    private List<String> functionNames = new ArrayList<>();
    private String lastSelectedFunctionName;
    private String lastSelectedJarArtifactName;

    public String getLastSelectedJarArtifactName() {
        return lastSelectedJarArtifactName;
    }

    public void setLastSelectedJarArtifactName(String lastSelectedJarArtifactName) {
        this.lastSelectedJarArtifactName = lastSelectedJarArtifactName;
    }

    public String getLastSelectedFunctionName() {
        return lastSelectedFunctionName;
    }

    public void setLastSelectedFunctionName(String lastSelectedFunctionName) {
        this.lastSelectedFunctionName = lastSelectedFunctionName;
    }

    public List<String> getFunctionNames() {
        return functionNames;
    }

    public void setFunctionNames(List<String> functionNames) {
        this.functionNames = functionNames;
    }

    @Nullable
    @Override
    public ConnectorSettings getState() {
        return this;
    }

    @Override
    public void loadState(ConnectorSettings connectorSettings) {
        XmlSerializerUtil.copyBean(connectorSettings, this);
    }

    public static ConnectorSettings getInstance() {
        return ServiceManager.getService(ConnectorSettings.class);
    }

}
