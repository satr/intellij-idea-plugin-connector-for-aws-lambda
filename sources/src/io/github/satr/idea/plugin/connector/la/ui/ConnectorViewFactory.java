package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2017, github.com/satr, MIT License

import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.model.AWSLambdaException;
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
import io.github.satr.idea.plugin.connector.la.entities.CredentialProfileEntry;
import io.github.satr.idea.plugin.connector.la.entities.FunctionEntry;
import io.github.satr.idea.plugin.connector.la.entities.RegionEntry;
import io.github.satr.idea.plugin.connector.la.models.ProjectModel;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Collection;
import java.util.List;

import static io.github.satr.common.StringUtil.getNotEmptyString;
import static org.apache.http.util.TextUtils.isEmpty;

public class ConnectorViewFactory implements ToolWindowFactory, ConnectorView {
    static final Logger logger = LogManager.getLogger(ConnectorViewFactory.class);
    private final ConnectorPresenter presenter;
    private final ProgressManager progressManager = ProgressManager.getInstance();
    private final ProjectModel projectModel = ServiceManager.getService(ProjectModel.class);

    private JComboBox functionList;
    private JButton refreshFuncListButton;
    private JButton updateFunctionButton;
    private JPanel toolPanel;
    private JComboBox jarArtifactList;
    private JButton refreshJarArtifactsButton;
    private JComboBox regionList;
    private JButton refreshRegionsButton;
    private JComboBox credentialProfileList;
    private JButton refreshCredentialProfiles;
    private JTabbedPane tabContent;
    private JTextArea logTextBox;
    private JPanel tabPanSettings;
    private JTextPane txtStatus;
    private JButton refreshAllButton;
    private JPanel pnlToolBar;
    private JPanel pnlSettings;
    private JPanel pnlTabs;
    private JComboBox logLevelList;
    private JButton clearLogButton;
    private JScrollPane logScrollPan;
    private JPanel runFunctionTestTab;
    private JTextArea functionTestOutputText;
    private JTextArea functionTestInputText;
    private JButton openFunctionTestInputFileButton;
    private JButton runtFunctionTestButton;
    private JComboBox functionTestFavoritesList;
    private JPanel logPan;
    private JPanel logSettingsPan;
    private Project project;
    private boolean operationInProgress = false;
    private boolean setRegionOperationInProgress;
    private boolean runFunctionTestOperationInProgress;

    public ConnectorViewFactory() {
        this(ServiceManager.getService(ConnectorPresenter.class));
    }

    public ConnectorViewFactory(ConnectorPresenter presenter) {
        prepareUiLogger();
        this.presenter = presenter;
        this.presenter.setView(this);
        presenter.refreshJarArtifactList(project);
        refreshAllButton.addActionListener(e -> {
            runRefreshAllList(presenter);
        });
        refreshFuncListButton.addActionListener(e -> {
            runRefreshFunctionList(presenter);
        });
        refreshJarArtifactsButton.addActionListener(e -> {
            runRefreshJarArtifactList(presenter);
        });
        refreshRegionsButton.addActionListener(e -> {
            runRefreshRegionList(presenter);
        });
        refreshCredentialProfiles.addActionListener(e -> {
            runRefreshCredentialProfilesList(presenter);
        });
        updateFunctionButton.addActionListener(e -> {
            runUpdateFunction(presenter);
        });
        clearLogButton.addActionListener(e -> {
            clearLog();
        });
        runtFunctionTestButton.addActionListener(e -> {
            runFunctionTest(presenter);
        });
        openFunctionTestInputFileButton.addActionListener(e -> {
            openFunctionTestInputFile(presenter);
        });
        functionList.addItemListener(e -> {
            runSetFunction(presenter, e);
        });
        regionList.addItemListener(e -> {
            runSetRegion(presenter, e);
        });
        credentialProfileList.addItemListener(e -> {
            runSetCredentialProfile(presenter, e);
        });
        jarArtifactList.addItemListener(e -> {
            runSetJarArtifact(presenter, e);
        });
        runRefreshAllList(presenter);
    }

    private void openFunctionTestInputFile(ConnectorPresenter presenter) {
        if(runFunctionTestOperationInProgress){
            return;
        }
        MessageHelper.showInfo(project, "Open test");
        String filename = "filename";
        runOperation(() -> presenter.openFunctionTestInputFile(filename), "Read test function test input file." + filename);
    }

    private void prepareUiLogger() {
        logger.addAppender(new AsyncAppender(){
            @Override
            public void append(LoggingEvent event) {
                super.append(event);
                logTextBox.append(String.format("\n%s: %s", event.getLevel(), event.getMessage()));
            }
        });
        logLevelList.addItem(Level.DEBUG);
        logLevelList.addItem(Level.INFO);
        logLevelList.addItem(Level.ERROR);
        logger.setLevel(Level.INFO);
        logLevelList.setSelectedItem(Level.INFO);
        logLevelList.addItemListener(e -> {
            logger.setLevel((Level) e.getItem());
        });
    }

    private void runSetRegion(ConnectorPresenter presenter, ItemEvent e) {
        if(operationInProgress || setRegionOperationInProgress
                || e.getStateChange() != ItemEvent.SELECTED)
            return;
        RegionEntry entry = (RegionEntry)e.getItem();
        if(entry == null)
            return;
        runOperation(() -> presenter.setRegion(entry), "Select region: " + entry.toString());
    }

    private void runSetFunction(ConnectorPresenter presenter, ItemEvent e) {
        if(operationInProgress || setRegionOperationInProgress
                || e.getStateChange() != ItemEvent.SELECTED)
            return;
        FunctionEntry entry = (FunctionEntry)e.getItem();
        if(entry == null)
            return;
        runOperation(() -> presenter.setFunction(entry), "Select function: " + entry.toString());
    }

    private void runFunctionTest(ConnectorPresenter presenter) {
        if(operationInProgress || runFunctionTestOperationInProgress)
            return;
        runOperation(() -> {
            runFunctionTestOperationInProgress = true;
            try {
                presenter.runFunctionTest(project, functionTestInputText.getText());
            } finally {
                runFunctionTestOperationInProgress = false;
            }
        }, "Run test of the function");
    }

    private void runSetCredentialProfile(ConnectorPresenter presenter, ItemEvent e) {
        if(e.getStateChange() != ItemEvent.SELECTED)
            return;
        CredentialProfileEntry entry = (CredentialProfileEntry)e.getItem();
        if(entry == null)
            return;
        runOperation(() -> presenter.setCredentialProfile(entry), "Select credential profile: " + entry.toString());
    }

    private void runSetJarArtifact(ConnectorPresenter presenter, ItemEvent e) {
        if(e.getStateChange() != ItemEvent.SELECTED)
            return;
        ArtifactEntry entry = (ArtifactEntry) e.getItem();
        if(entry == null)
            return;
        runOperation(() -> presenter.setJarArtifact(entry), "Select JAR-artifact: " + entry.toString());
    }

    private void runUpdateFunction(ConnectorPresenter presenter) {
        runOperation(() -> presenter.updateFunction(getProject()),
                    "Update selected AWS Lambda function with JAR-artifact");
    }

    private void clearLog() {
        logTextBox.setText("");
    }

    private void runRefreshAllList(ConnectorPresenter presenter) {
        runOperation(() -> {
            presenter.refreshAllLists(project);
            presenter.refreshStatus();
        }, "Refresh all lists");
    }
    private void runRefreshFunctionList(ConnectorPresenter presenter) {
        runOperation(() -> {
            presenter.refreshFunctionList();
            presenter.refreshStatus();
        }, "Refresh list of AWS Lambda functions");
    }

    private void runRefreshJarArtifactList(ConnectorPresenter presenter) {
        runOperation(() -> {
            presenter.refreshJarArtifactList(project);
            presenter.refreshStatus();
        }, "Refresh list of JAR-artifacts in the project");
    }

    private void runRefreshRegionList(ConnectorPresenter presenter) {
        runOperation(() -> {
            presenter.refreshRegionList(project);
            presenter.refreshStatus();
        }, "Refresh list of AWS regions");
    }

    private void runRefreshCredentialProfilesList(ConnectorPresenter presenter) {
        runOperation(() -> {
            presenter.refreshCredentialProfilesList(project);
            presenter.refreshStatus();
        }, "Refresh list of credential profiles");
    }

    private void runOperation(Runnable runnable, final String format, Object... args) {
        String title = String.format(format, args);
        if(operationInProgress)
            return;
        try {
            operationInProgress = true;
            setControlsEnabled(false);
            progressManager.run(new Task.Backgroundable(project, title, true) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    try {
                        logDebug(title);
                        runnable.run();
                    } catch (Throwable t) {
                        Class<? extends Throwable> exceptionClass = t.getClass();
                        if(exceptionClass.equals(AWSLambdaException.class)){
                            MessageHelper.showError(project, t.getMessage());
                            logError(t.getMessage());
                        } else if(exceptionClass.equals(SdkClientException.class)){
                            MessageHelper.showError(project, t.getMessage());
                            logError(t.getMessage());
                        } else {
                            MessageHelper.showCriticalError(project, t);
                            logError(t.getMessage());
                        }
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
        refreshAllButton.setEnabled(enabled);
        refreshFuncListButton.setEnabled(enabled);
        refreshJarArtifactsButton.setEnabled(enabled);
        refreshRegionsButton.setEnabled(enabled);
        refreshCredentialProfiles.setEnabled(enabled);
        updateFunctionButton.setEnabled(enabled);
        functionList.setEnabled(enabled);
        jarArtifactList.setEnabled(enabled);
        regionList.setEnabled(enabled);
        credentialProfileList.setEnabled(enabled);
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
    public void setFunctionList(List<FunctionEntry> functions, FunctionEntry selectedFunctionEntry) {
        functionList.removeAllItems();
        for (FunctionEntry entry : functions) {
            if (entry.getRuntime().equals(Runtime.Java8)) {
                functionList.addItem(entry);
            }
        }
        if(selectedFunctionEntry != null){
            functionList.setSelectedItem(selectedFunctionEntry);
        }
    }

    @Override
    public void setArtifactList(Collection<? extends ArtifactEntry> artifacts, ArtifactEntry selectedArtifactEntry) {
        jarArtifactList.removeAllItems();
        for(ArtifactEntry artifactEntry : artifacts) {
            jarArtifactList.addItem(artifactEntry);
        }
        if(selectedArtifactEntry != null){
            jarArtifactList.setSelectedItem(selectedArtifactEntry);
        }
    }

    @Override
    public void setCredentialProfilesList(List<CredentialProfileEntry> credentialProfiles, String selectedCredentialsProfile) {
        credentialProfileList.removeAllItems();
        CredentialProfileEntry selectedCredentialsProfileEntry = null;
        for(CredentialProfileEntry entry : credentialProfiles){
            credentialProfileList.addItem(entry);
            if(!isEmpty(selectedCredentialsProfile) && entry.getName().equals(selectedCredentialsProfile))
                selectedCredentialsProfileEntry = entry;
        }
        if(selectedCredentialsProfileEntry != null) {
            credentialProfileList.setSelectedItem(selectedCredentialsProfileEntry);
        }
    }

    @Override
    public void setRegion(Regions region) {
        if(setRegionOperationInProgress){
            return;
        }
        try {
            setRegionOperationInProgress = true;
            for (int i = 0; i < regionList.getItemCount(); i++) {
                if(((RegionEntry) regionList.getItemAt(i)).getName().equals(region.getName())){
                    regionList.setSelectedIndex(i);
                    return;
                }
            }
        } finally {
            setRegionOperationInProgress = false;
        }
    }

    @Override
    public void refreshStatus(String function, String artifact, String region, String regionDescription, String credentialProfile) {
        txtStatus.setText(String.format("Func: \"%s\"; Jar: \"%s\"; Region: \"%s\"; Profile:\"%s\"",
                getNotEmptyString(function, "?"),
                getNotEmptyString(artifact, "?"),
                getNotEmptyString(region, "?"),
                getNotEmptyString(credentialProfile, "?")
        ));
        txtStatus.setToolTipText(String.format("Func: %s\nJar: %s\nRegion: %s\nProfile: %s",
                getNotEmptyString(function, "?"),
                getNotEmptyString(artifact, "?"),
                getNotEmptyString(regionDescription, "?"),
                getNotEmptyString(credentialProfile, "?")
        ));
    }

    @Override
    public FunctionEntry getSelectedFunctionEntry() {
        return (FunctionEntry) functionList.getSelectedItem();
    }

    @Override
    public ArtifactEntry getSelectedArtifactEntry() {
        return (ArtifactEntry) jarArtifactList.getSelectedItem();
    }

    @Override
    public RegionEntry getSelectedRegionEntry() {
        return (RegionEntry) regionList.getSelectedItem();
    }

    @Override
    public CredentialProfileEntry getSelectedCredentialProfileEntry() {
        return (CredentialProfileEntry) credentialProfileList.getSelectedItem();
    }

    @Override
    public void logDebug(String format, Object... args) {
        logger.debug(String.format(format, args));
    }

    @Override
    public void logInfo(String format, Object... args) {
        logger.info(String.format(format, args));
    }

    @Override
    public void logError(String format, Object... args) {
        logger.error(String.format(format, args));
    }

    @Override
    public void setFunctionTestOutput(String outputText) {
        functionTestOutputText.setText(outputText);
    }

    @Override
    public void setFunctionTestInput(String inputText) {
        functionTestInputText.setText(inputText);
    }

    @Override
    public void setRegionList(List<RegionEntry> regions, Regions selectedRegion) {
        regionList.removeAllItems();
        RegionEntry selectedRegionEntry = null;
        for(RegionEntry entry : regions) {
            regionList.addItem(entry);
            if(selectedRegion != null && entry.getRegion().getName().equals(selectedRegion.getName())) {
                selectedRegionEntry = entry;
            }
        }
        if(selectedRegionEntry != null) {
            regionList.setSelectedItem(selectedRegionEntry);
        }
    }
}
