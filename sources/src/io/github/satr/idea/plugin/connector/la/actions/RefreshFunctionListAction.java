package io.github.satr.idea.plugin.connector.la.actions;
// Copyright © 2017, github.com/satr, MIT License

import com.amazonaws.services.lambda.model.Runtime;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import io.github.satr.idea.plugin.connector.la.entities.FunctionEntry;

import java.util.ArrayList;
import java.util.List;

import static io.github.satr.common.MessageHelper.showInfo;

public class RefreshFunctionListAction extends AbstractFunctionAction {
    @Override
    public void actionPerformed(AnActionEvent event) {
        List<String> functionNames = getFunctionNamesFor(Runtime.Java8);
        connectorSettings.setFunctionNames(functionNames);

        final Project project = event.getData(PlatformDataKeys.PROJECT);
        StringBuilder builder = new StringBuilder();
        builder.append("Functions (Java 8):\n");
        for(String name : functionNames)
            builder.append(name + "\n");
        showInfo(project, builder.toString());
    }

    private List<String> getFunctionNamesFor(Runtime runtime) {
        ArrayList<String> functionNames = new ArrayList<>();
        for(FunctionEntry entry : getConnectorModel().getFunctions()){
            if(entry.getRuntime().equals(runtime))
                functionNames.add(entry.getFunctionName());
        }
        return functionNames;
    }

}
