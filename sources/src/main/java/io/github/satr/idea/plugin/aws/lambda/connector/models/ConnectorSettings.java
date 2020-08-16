package io.github.satr.idea.plugin.aws.lambda.connector.models;
// Copyright © 2020, github.com/satr, MIT License

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.apache.http.annotation.Obsolete;
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
    @Obsolete
    //use: lastSelectedArtifactName
    private String lastSelectedJarArtifactName;
    private String lastSelectedArtifactName;
    private String lastSelectedRegionName;
    private String lastSelectedCredentialProfile;
    private String lastSelectedTestFunctionInputFilePath;
    @Obsolete
    //use: lastSelectedArtifactPerFunction
    private Map<String, String> lastSelectedJarArtifactPerFunction = new HashMap<>();
    private Map<String, String> lastSelectedArtifactPerFunction = new HashMap<>();

    @Obsolete
    //use: getLastSelectedArtifactName(); left for backward compatibility transition
    public String getLastSelectedJarArtifactName() {
        return lastSelectedJarArtifactName;
    }

    public String getLastSelectedArtifactName() {
        return lastSelectedArtifactName;
    }

    public void setLastSelectedArtifactName(String lastSelectedArtifactName) {
        this.lastSelectedJarArtifactName = null; //obsolete - left for backward compatibility transition
        this.lastSelectedArtifactName = lastSelectedArtifactName;

        if (!isEmpty(lastSelectedFunctionName)) {
            if (!isEmpty(lastSelectedArtifactName)) {
                lastSelectedJarArtifactPerFunction.put(lastSelectedFunctionName, lastSelectedArtifactName);
            } else {
                lastSelectedJarArtifactPerFunction.remove(lastSelectedFunctionName);
            }
        }
    }
    @Obsolete
    //use: getLastSelectedArtifactNameForFunction()
    public @Nullable String getLastSelectedJarArtifactNameForFunction(@NotNull String functionName) {
        return lastSelectedJarArtifactPerFunction.getOrDefault(functionName, null);
    }

    @Obsolete
    //use: getLastSelectedArtifactPerFunction()
    public Map<String, String> getLastSelectedJarArtifactPerFunction() {
        return lastSelectedJarArtifactPerFunction;
    }

    @Obsolete
    //use: setLastSelectedArtifactPerFunction()
    public void setLastSelectedJarArtifactPerFunction(Map<String, String> lastSelectedJarArtifactPerFunction) {
        this.lastSelectedJarArtifactPerFunction = lastSelectedJarArtifactPerFunction;
    }

    public @Nullable String getLastSelectedArtifactNameForFunction(@NotNull String functionName) {
        return lastSelectedArtifactPerFunction.getOrDefault(functionName, null);
    }

    public Map<String, String> getLastSelectedArtifactPerFunction() {
        return lastSelectedArtifactPerFunction;
    }

    public void setLastSelectedArtifactPerFunction(Map<String, String> lastSelectedArtifactPerFunction) {
        this.lastSelectedArtifactPerFunction = lastSelectedArtifactPerFunction;
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
