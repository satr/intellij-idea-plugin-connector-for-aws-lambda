package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2017, github.com/satr, MIT License

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.model.Runtime;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import io.github.satr.common.MessageHelper;
import io.github.satr.idea.plugin.connector.la.entities.ArtifactEntry;
import io.github.satr.idea.plugin.connector.la.entities.FunctionEntry;
import io.github.satr.idea.plugin.connector.la.entities.RegionEntry;
import io.github.satr.idea.plugin.connector.la.models.ProjectModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Collection;
import java.util.List;

public class ConnectorViewFactory implements ToolWindowFactory, ConnectorView {
    private final ConnectorPresenter presenter;
    private final ProgressManager progressManager = ProgressManager.getInstance();
    private final ProjectModel projectModel = ServiceManager.getService(ProjectModel.class);

    private JComboBox cbFunctions;
    private JButton refreshFuncListButton;
    private JButton updateFunctionButton;
    private JPanel toolPanel;
    private JComboBox cbJarArtifacts;
    private JButton refreshJarArtifactsButton;
    private JComboBox cbRegions;
    private JButton refreshRegionsButton;
    private Project project;
    private boolean operationInProgress = false;

    public ConnectorViewFactory() {
        this(ServiceManager.getService(ConnectorPresenter.class));
    }

    public ConnectorViewFactory(ConnectorPresenter presenter) {
        this.presenter = presenter;
        this.presenter.setView(this);
        presenter.refreshJarArtifactList(project);
        refreshFuncListButton.addActionListener(e -> {
            runRefreshFunctionList(presenter);
        });
        refreshJarArtifactsButton.addActionListener(e -> {
            runRefreshJarArtifactList(presenter);
        });
        refreshRegionsButton.addActionListener(e -> {
            runRefreshRegionList(presenter);
        });
        updateFunctionButton.addActionListener(e -> {
            runUpdateFunction(presenter);
        });
        cbRegions.addItemListener(e -> {
            runSetRegion(presenter, e);
        });
        //runRefreshRegionList(presenter); //region list has not been initialized automatically due to it takes time
        // during loading of IDE but the plugin might not be needed in all projects
    }

    private void runSetRegion(ConnectorPresenter presenter, ItemEvent e) {
        if(e.getStateChange() != ItemEvent.SELECTED)
            return;
        RegionEntry regionEntry = (RegionEntry)e.getItem();
        if(regionEntry == null)
            return;
        runOperation(() -> presenter.setRegion(regionEntry), "Select region: " + regionEntry.toString());
    }

    private void runUpdateFunction(ConnectorPresenter presenter) {
        if (cbFunctions.getSelectedObjects().length <= 0
            || cbJarArtifacts.getSelectedObjects().length <= 0)
            return;

        runOperation(() -> presenter.updateFunction((FunctionEntry)cbFunctions.getSelectedItem(),
                                                    (ArtifactEntry)cbJarArtifacts.getSelectedItem(),
                                                    getProject()),
                    "Update selected AWS Lambda function with JAR-artefact");
    }

    private void runRefreshFunctionList(ConnectorPresenter presenter) {
        runOperation(() -> presenter.refreshFunctionList(), "Refresh list of AWS Lambda functions");
    }

    private void runRefreshJarArtifactList(ConnectorPresenter presenter) {
        runOperation(() -> presenter.refreshJarArtifactList(project), "Refresh list of JAR-artifacts in the project");
    }

    private void runRefreshRegionList(ConnectorPresenter presenter) {
        runOperation(() -> presenter.refreshRegionList(project), "Refresh list of AWS regions");
    }

    private void runOperation(Runnable runnable, final String title) {
        if(operationInProgress)
            return;
        try {
            operationInProgress = true;
            setControlsEnabled(false);
            progressManager.run(new Task.Backgroundable(project, title, true) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    try {
                        runnable.run();
                    } catch (Throwable t) {
                        MessageHelper.showError(project, t);
                    }
                    finally {
                        setControlsEnabled(true);
                        operationInProgress = false;
                    }
                }
            });
        }
        catch(Throwable t){
            MessageHelper.showError(project, t);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        refreshFuncListButton.setEnabled(enabled);
        refreshJarArtifactsButton.setEnabled(enabled);
        refreshRegionsButton.setEnabled(enabled);
        updateFunctionButton.setEnabled(enabled);
        cbFunctions.setEnabled(enabled);
        cbJarArtifacts.setEnabled(enabled);
        cbRegions.setEnabled(enabled);
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
        this.presenter.refreshJarArtifactList(this.project);
    }

    @Override
    public void setFunctionList(List<FunctionEntry> functions) {
        cbFunctions.removeAllItems();
        for (FunctionEntry entry : functions) {
            if (entry.getRuntime().equals(Runtime.Java8))
                cbFunctions.addItem(entry);
        }
    }

    @Override
    public void setArtifactList(Collection<? extends ArtifactEntry> artifacts) {
        cbJarArtifacts.removeAllItems();
        for(ArtifactEntry artifactEntry : artifacts)
            cbJarArtifacts.addItem(artifactEntry);
    }

    @Override
    public void setRegionList(List<RegionEntry> regions, Regions selectedRegion) {
        cbRegions.removeAllItems();
        RegionEntry selectedRegionEntry = null;
        for(RegionEntry regionEntry : regions) {
            cbRegions.addItem(regionEntry);
            if(selectedRegion != null && regionEntry.getRegion().getName().equals(selectedRegion.getName()))
                selectedRegionEntry = regionEntry;
        }
        if(selectedRegionEntry != null)
            cbRegions.setSelectedItem(selectedRegionEntry);
    }
}
