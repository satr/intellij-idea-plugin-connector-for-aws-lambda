package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2017, github.com/satr, MIT License

import com.amazonaws.services.lambda.model.Runtime;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import io.github.satr.idea.plugin.connector.la.entities.FunctionEntry;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class ConnectorViewFactory implements ToolWindowFactory, ConnectorView {
    private final ConnectorPresenter presenter;
    private JComboBox cbFunctions;
    private JButton refreshButton;
    private JButton updateButton;
    private JPanel toolPanel;
    private Project project;

    public ConnectorViewFactory() {
        this(ServiceManager.getService(ConnectorPresenter.class));
    }

    public ConnectorViewFactory(ConnectorPresenter presenter) {
        this.presenter = presenter;
        presenter.setView(this);
        refreshButton.addActionListener(e -> presenter.refreshFunctionList());
        updateButton.addActionListener(e -> {
            if(cbFunctions.getSelectedObjects().length > 0)
                presenter.updateFunction((FunctionEntry)cbFunctions.getSelectedItem(), getProject());
        });
    }

    public Project getProject() {
        return project;
    }

    @Override
    protected void finalize() throws Throwable {
        if (presenter != null)
            presenter.shutdown();

        super.finalize();
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(toolPanel, "", false);
        toolWindow.getContentManager().addContent(content);
        this.project = project;
    }

    @Override
    public void setFunctionList(List<FunctionEntry> functions) {
        cbFunctions.removeAllItems();
        for (FunctionEntry entry : functions) {
            if (entry.getRuntime().equals(Runtime.Java8))
                cbFunctions.addItem(entry);
        }
    }
}
