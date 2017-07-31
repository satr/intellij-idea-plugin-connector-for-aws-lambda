package io.github.satr.idea.plugin.connector.la.actions;
// Copyright © 2017, github.com/satr, MIT License

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.packaging.artifacts.Artifact;
import io.github.satr.common.OperationValueResult;
import io.github.satr.idea.plugin.connector.la.entities.ArtifactEntry;
import io.github.satr.idea.plugin.connector.la.entities.FunctionEntry;
import io.github.satr.idea.plugin.connector.la.models.ProjectModel;

import java.util.Collection;

import static io.github.satr.common.MessageHelper.showError;
import static io.github.satr.common.MessageHelper.showInfo;
import static org.apache.http.util.TextUtils.isEmpty;

public class UpdateFunctionAction extends AbstractFunctionAction {
    private final ProjectModel projectModel = ServiceManager.getService(ProjectModel.class);

    //TODO: enter credentials - for now it is used default credentials, setup by the aws cli
    @Override
    public void actionPerformed(AnActionEvent event) {
        final Project project = event.getData(PlatformDataKeys.PROJECT);
        ArtifactEntry artifactEntry = getPreviouslySelectedArtifact(project);
        if(artifactEntry == null){
            showError(project, "JAR-artifact not found.");
            return;
        }

        String functionName = connectorSettings.getLastSelectedFunctionName();
        functionName = Messages.showInputDialog(project, "Name", "Name of the function powered by AWS Lambda",
                                                Messages.getQuestionIcon(), functionName, null);
        if(functionName != null)
            functionName = functionName.trim();
        if(isEmpty(functionName)) {
            showError(project, "Function name cannot be empty");
            return;
        }
        if(!connectorSettings.getFunctionNames().contains(functionName)){
            showError(project, "Function \"%s\" does not exist.\nTry to update the list.", functionName);
            return;
        }

        final OperationValueResult<FunctionEntry> result = getConnectorModel().updateWithJar(functionName, artifactEntry.getOutputFilePath());

        if (!result.success()) {
            showError(project, result.getErrorAsString());
            return;
        }
        connectorSettings.setLastSelectedFunctionName(functionName);
        connectorSettings.setLastSelectedJarArtifactName(artifactEntry.getName());
        showInfo(project, "Function \"%s\" has been updated with the artifact \"%s\".", result.getValue().getFunctionName(), artifactEntry.getName());
    }

    private ArtifactEntry getPreviouslySelectedArtifact(Project project) {
        Collection<? extends ArtifactEntry> jarArtifacts = projectModel.getJarArtifacts(project);
        if(jarArtifacts.isEmpty())
            return null;

        String lastSelectedJarArtifactName = connectorSettings.getLastSelectedJarArtifactName();
        ArtifactEntry artifactEntry = (ArtifactEntry) jarArtifacts.toArray()[0];
        for(ArtifactEntry art : jarArtifacts) {
            if (!art.getName().equals(lastSelectedJarArtifactName))
                continue;
            return art;
        }
        return artifactEntry;
    }
}
