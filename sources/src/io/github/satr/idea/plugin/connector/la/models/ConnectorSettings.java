package io.github.satr.idea.plugin.connector.la.models;
// Copyright © 2018, github.com/satr, MIT License

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.http.util.TextUtils.isEmpty;

@State(name = "ConnectorSettings", storages = @Storage("connector-settings.xml"))
public class ConnectorSettings  implements PersistentStateComponent<ConnectorSettings> {

    private List<String> functionNames = new ArrayList<>();
    private String lastSelectedFunctionName;
    private String lastSelectedJarArtifactName;
    private String lastSelectedRegionName;
    private String lastSelectedCredentialProfile;
    private String lastSelectedTestFunctionInputFilePath;

    private Map<String, String> lastSelectedJarArtifactPerFunction = new HashMap<>();

    public String getLastSelectedJarArtifactName() {
        return lastSelectedJarArtifactName;
    }

    public void setLastSelectedJarArtifactName(String lastSelectedJarArtifactName) {
        this.lastSelectedJarArtifactName = lastSelectedJarArtifactName;

        if (!isEmpty(lastSelectedFunctionName)) {
            if (!isEmpty(lastSelectedJarArtifactName)) {
                lastSelectedJarArtifactPerFunction.put(lastSelectedFunctionName, lastSelectedJarArtifactName);
            } else {
                lastSelectedJarArtifactPerFunction.remove(lastSelectedFunctionName);
            }
        }
    }

    public @Nullable String getLastSelectedJarArtifactNameForFunction(@NotNull String functionName) {
        return lastSelectedJarArtifactPerFunction.getOrDefault(functionName, null);
    }

    public Map<String, String> getLastSelectedJarArtifactPerFunction() {
        return lastSelectedJarArtifactPerFunction;
    }

    public void setLastSelectedJarArtifactPerFunction(Map<String, String> lastSelectedJarArtifactPerFunction) {
        this.lastSelectedJarArtifactPerFunction = lastSelectedJarArtifactPerFunction;
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

    public void setLastSelectedRegionName(String lastSelectedRegionName) {
        clearLastSelectedFunctionOnChangedRegion(lastSelectedRegionName);
        this.lastSelectedRegionName = lastSelectedRegionName;
    }

    private void clearLastSelectedFunctionOnChangedRegion(String lastSelectedRegionName) {
        if((isEmpty(lastSelectedRegionName) && !isEmpty(this.lastSelectedRegionName))
                || (!isEmpty(lastSelectedRegionName) && !lastSelectedRegionName.equals(this.lastSelectedRegionName)
                        && !isEmpty(this.lastSelectedRegionName))){
            setLastSelectedFunctionName("");
        }
    }

    public String getLastSelectedRegionName() {
        return lastSelectedRegionName;
    }

    public String getLastSelectedCredentialProfile() {
        return lastSelectedCredentialProfile;
    }

    public void setLastSelectedCredentialProfile(String lastSelectedCredentialProfile) {
        this.lastSelectedCredentialProfile = lastSelectedCredentialProfile;
    }

    public String getLastSelectedTestFunctionInputFilePath() {
        return this.lastSelectedTestFunctionInputFilePath;
    }

    public void setLastSelectedTestFunctionInputFilePath(String path) {
        this.lastSelectedTestFunctionInputFilePath = path;
    }
}
