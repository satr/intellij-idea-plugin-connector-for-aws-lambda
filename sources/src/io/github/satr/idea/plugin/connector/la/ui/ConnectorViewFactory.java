package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2018, github.com/satr, MIT License

import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.model.AWSLambdaException;
import com.amazonaws.services.lambda.model.Runtime;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.messages.MessageBusConnection;
import io.github.satr.common.MessageHelper;
import io.github.satr.common.OperationResult;
import io.github.satr.idea.plugin.connector.la.entities.*;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.github.satr.common.StringUtil.getNotEmptyString;
import static org.apache.http.util.TextUtils.isEmpty;

public class ConnectorViewFactory implements ToolWindowFactory, ConnectorView {
    static final Logger logger = LogManager.getLogger(ConnectorViewFactory.class);
    private final ConnectorPresenter presenter;
    private final ProgressManager progressManager = ProgressManager.getInstance();
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
    private JButton openFunctionInputFileButton;
    private JButton runFunctionTestButton;
    private JComboBox testFunctionInputRecentFileList;
    private JPanel logPan;
    private JPanel logSettingsPan;
    private JButton updateProxySettingsButton;
    private JFormattedTextField functionArn;
    private JFormattedTextField functionLastModified;
    private JComboBox functionRoles;
    private JFormattedTextField functionTimeout;
    private JFormattedTextField functionMemorySize;
    private JFormattedTextField functionHandler;
    private JFormattedTextField functionDescription;
    private JLabel functionRuntime;
    private JComboBox functionTracingConfigModes;
    private JButton refreshFunctionProperties;
    private JTextField textProxyHost;
    private JTextField textProxyPort;
    private JCheckBox cbUseProxy;
    private Project project;
    private boolean operationInProgress = false;
    private boolean setRegionOperationInProgress;
    private boolean runFunctionTestOperationInProgress;
    final private List<JButton> buttons = new ArrayList<>();

    public ConnectorViewFactory() {
        this(ServiceManager.getService(ConnectorPresenter.class));
    }

    public ConnectorViewFactory(final ConnectorPresenter presenter) {
        prepareButtons();
        prepareUiLogger();
        this.presenter = presenter;
        this.presenter.setView(this);

        refreshAllButton.addActionListener(e -> {
            runRefreshAll(presenter);
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
        runFunctionTestButton.addActionListener(e -> {
            runFunctionTest(presenter);
        });
        openFunctionInputFileButton.addActionListener(e -> {
            openFunctionTestInputFile(presenter);
        });
        testFunctionInputRecentFileList.addItemListener(e -> {
            runSetTestFunctionInputFromRecent(presenter, e);
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
        updateProxySettingsButton.addActionListener(e -> {
            updateProxySetting(presenter);
        });
        presenter.refreshTracingModeList();
        presenter.refreshJarArtifactList(project);
        runRefreshAll(presenter);
    }

    @NotNull
    private MessageBusConnection getMessageBusConnector() {
        return ApplicationManager.getApplication().getMessageBus().connect();
    }

    private void prepareButtons() {
        setupButton(updateFunctionButton, IconLoader.getIcon("/icons/iconUpdateFunction.png"));
        setupButton(refreshAllButton, IconLoader.getIcon("/icons/iconRefresh.png"));
        setupButton(runFunctionTestButton, IconLoader.getIcon("/icons/iconRunFunctionTest.png"));
        setupButton(openFunctionInputFileButton, IconLoader.getIcon("/icons/iconOpenFunctionInputFile.png"));
        setupButton(updateProxySettingsButton, IconLoader.getIcon("/icons/iconUpdateProxySettings.png"));
        setupButton(clearLogButton, IconLoader.getIcon("/icons/iconClearLog.png"));
        setupButton(refreshFuncListButton, IconLoader.getIcon("/icons/iconRefresh.png"));
        setupButton(refreshJarArtifactsButton, IconLoader.getIcon("/icons/iconRefresh.png"));
        setupButton(refreshRegionsButton, IconLoader.getIcon("/icons/iconRefresh.png"));
        setupButton(refreshCredentialProfiles, IconLoader.getIcon("/icons/iconRefresh.png"));

        getMessageBusConnector().subscribe(UISettingsListener.TOPIC, this::uiSettingsChanged);
    }

    private void uiSettingsChanged(UISettings uiSettings) {
        fixButtonsAfterPotentiallyChangedTheme();
    }

    private void fixButtonsAfterPotentiallyChangedTheme() {
        for(JButton button: buttons) {
            removeButtonBorder(button);
        }
    }

    private void setupButton(JButton button, Icon icon) {
        button.setIcon(icon);
        removeButtonBorder(button);
        buttons.add(button);
    }

    private static void removeButtonBorder(JButton button) {
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setBorder(null);
    }

    private void openFunctionTestInputFile(ConnectorPresenter presenter) {
        if(runFunctionTestOperationInProgress) {
            return;
        }

        String path = presenter.getLastSelectedTestFunctionInputFilePath();
        File file = new File(path);
        if(!file.isDirectory() || !file.exists()){
            file = new File("");
        }
        JFileChooser fileChooser = new JFileChooser(file);
        fileChooser.setDialogTitle("Test Function Input");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON", "json");
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setFileFilter(filter);
        fileChooser.setMultiSelectionEnabled(false);
        if(fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION){
            return;
        }
        try {
            File selectedFile = fileChooser.getSelectedFile();
            presenter.setLastSelectedTestFunctionInputFilePath(fileChooser.getCurrentDirectory().getCanonicalPath());
            runOperation(() -> presenter.openTestFunctionInputFile(selectedFile), "Read test function input file \"%s\".", selectedFile.getName());
        } catch (Exception e) {
            e.printStackTrace();
            logError(e.getMessage());
        }
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
        logLevelList.addItem(Level.WARN);
        logLevelList.addItem(Level.ERROR);
        logger.setLevel(Level.INFO);
        logLevelList.setSelectedItem(Level.INFO);
        logLevelList.addItemListener(e -> {
            logger.setLevel((Level) e.getItem());
        });
    }

    private void runSetRegion(ConnectorPresenter presenter, ItemEvent e) {
        if(operationInProgress || setRegionOperationInProgress
                || e.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        RegionEntry entry = (RegionEntry)e.getItem();
        if(entry == null)
            return;
        runOperation(() -> presenter.setRegion(entry), "Select region: " + entry.toString());
    }

    private void runSetFunction(ConnectorPresenter presenter, ItemEvent e) {
        if(operationInProgress || setRegionOperationInProgress
                || e.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        FunctionEntry entry = (FunctionEntry)e.getItem();
        runOperation(() -> presenter.setFunction(entry), "Select function: " + entry.toString());
    }

    private void runSetTestFunctionInputFromRecent(ConnectorPresenter presenter, ItemEvent e) {
        if(operationInProgress
                || e.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        TestFunctionInputEntry entry = (TestFunctionInputEntry)e.getItem();
        if(entry == null)
            return;
        runOperation(() -> presenter.setSetTestFunctionInputFromRecent(entry), "Select test function input from file: ", entry.getFileName());
    }

    private void runFunctionTest(ConnectorPresenter presenter) {
        if(operationInProgress || runFunctionTestOperationInProgress) {
            return;
        }
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
        if(entry == null) {
            return;
        }
        runOperation(() -> presenter.setCredentialProfile(entry), "Select credential profile: " + entry.toString());
    }


    private void updateProxySetting(ConnectorPresenter presenter) {
        runOperation(() -> presenter.setProxySettings(), "Update proxy settings from IDEA settings.");
    }

    private void runSetJarArtifact(ConnectorPresenter presenter, ItemEvent e) {
        if(e.getStateChange() != ItemEvent.SELECTED)
            return;
        ArtifactEntry entry = (ArtifactEntry) e.getItem();
        if(entry == null) {
            return;
        }
        runOperation(() -> presenter.setJarArtifact(entry), "Select JAR-artifact: " + entry.toString());
    }

    private void runUpdateFunction(ConnectorPresenter presenter) {
        runOperation(() -> presenter.updateFunction(getProject()),
                    "Update selected AWS Lambda function with JAR-artifact");
    }

    private void clearLog() {
        logTextBox.setText("");
    }

    private void runRefreshAll(ConnectorPresenter presenter) {
        runOperation(() -> presenter.refreshAll(project), "Refresh all");
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

    private void runRefreshCredentialProfilesList(ConnectorPresenter presenter) {
        runOperation(() -> presenter.refreshCredentialProfilesList(project), "Refresh list of credential profiles");
    }

    private void runOperation(Runnable runnable, final String format, Object... args) {
        if(operationInProgress) {
            return;
        }
        try {
            operationInProgress = true;
            setControlsEnabled(false);
            final String title = String.format(format, args);
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
        runFunctionTestButton.setEnabled(enabled);
        openFunctionInputFileButton.setEnabled(enabled);
        functionList.setEnabled(enabled);
        jarArtifactList.setEnabled(enabled);
        regionList.setEnabled(enabled);
        credentialProfileList.setEnabled(enabled);
        testFunctionInputRecentFileList.setEnabled(enabled);
    }

    public Project getProject() {
        return project;
    }

    @Override
    protected void finalize() throws Throwable {
        if (presenter != null) {
            presenter.shutdown();
        }
        super.finalize();
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        toolWindow.getContentManager().addContent(ContentFactory.SERVICE.getInstance().createContent(toolPanel, "", false));
        this.project = project;
//TODO        this.presenter.init(this.project);
    }

    @Override
    public void setFunctionList(List<FunctionEntry> functions, FunctionEntry selectedFunctionEntry) {
        functionList.removeAllItems();
        for (FunctionEntry entry : functions) {
            if (entry.getRuntime().equals(Runtime.Java8)) {
                functionList.addItem(entry);
            }
        }
        presenter.setFunction(selectedFunctionEntry);
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
    public void refreshStatus(String function, String artifact, String region, String regionDescription,
                              String credentialProfile, String proxyDetails) {
        txtStatus.setText(String.format("Func: \"%s\"; Jar: \"%s\"; Region: \"%s\"; Profile:\"%s\"; Proxy:\"%s\"",
                getNotEmptyString(function, "?"),
                getNotEmptyString(artifact, "?"),
                getNotEmptyString(region, "?"),
                getNotEmptyString(credentialProfile, "?"),
                getNotEmptyString(proxyDetails, "?")
        ));
        txtStatus.setToolTipText(String.format("Func: %s\nJar: %s\nRegion: %s\nProfile: %s; \nProxy: %s",
                getNotEmptyString(function, "?"),
                getNotEmptyString(artifact, "?"),
                getNotEmptyString(regionDescription, "?"),
                getNotEmptyString(credentialProfile, "?"),
                getNotEmptyString(proxyDetails, "?")
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
    public void log(OperationResult operationResult) {
        if(operationResult.hasInfo()) {
            logInfo(operationResult.getInfoAsString());
        }
        if(operationResult.hasWarnings()) {
            logWarning(operationResult.getWarningsAsString());
        }
        if(operationResult.hasErrors()) {
            logInfo(operationResult.getErrorAsString());
        }
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
    public void logWarning(String format, Object... args) {
        logger.warn(String.format(format, args));
    }

    @Override
    public void logError(String format, Object... args) {
        logger.error(String.format(format, args));
    }

    @Override
    public void logError(Throwable throwable) {
        logError(throwable.getMessage());
    }

    @Override
    public void setFunctionTestOutput(String outputText) {
        functionTestOutputText.setText(outputText);
    }

    @Override
    public void setTestFunctionInput(String inputText) {
        functionTestInputText.setText(inputText);
    }

    @Override
    public void setTestFunctionInputRecentEntryList(List<TestFunctionInputEntry> entries) {
        testFunctionInputRecentFileList.removeAllItems();
        for(TestFunctionInputEntry entry : entries){
            testFunctionInputRecentFileList.addItem(entry);
        }
        if(entries.size() > 0) {
            testFunctionInputRecentFileList.setSelectedIndex(entries.size() - 1);//select last added entry
        }
    }

    @Override
    public void setFunctionProperties(FunctionEntry functionEntry) {
        if (functionEntry == null) {
            clearFunctionProperties();
            return;
        }
        functionDescription.setText(functionEntry.getDescription());
        functionHandler.setText(functionEntry.getHandler());
        functionArn.setText(functionEntry.getArn());
        functionLastModified.setText(functionEntry.getLastModified());
        functionRoles.setSelectedItem(functionEntry.getRoleEntity());
        functionRuntime.setText(functionEntry.getRuntime().name());
        functionMemorySize.setText(functionEntry.getMemorySize().toString());
        functionTimeout.setText(functionEntry.getTimeout().toString());
        functionTracingConfigModes.setSelectedItem(functionEntry.getTracingModeEntity());
    }

    private void clearFunctionProperties() {
        functionDescription.setText("");
        functionHandler.setText("");
        functionArn.setText("");
        functionLastModified.setText("");
        if(functionRoles.getItemCount() > 0) {
            functionRoles.setSelectedIndex(0);
        }
        functionRuntime.setText("");
        functionMemorySize.setText("");
        functionTimeout.setText("");
        functionTracingConfigModes.setSelectedIndex(0);
    }

    @Override
    public void setRoleList(List<RoleEntity> roles) {
        functionRoles.removeAllItems();
        for(RoleEntity entity : roles) {
            functionRoles.addItem(entity);
        }
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

    @Override
    public void setTracingModeList(Collection<TracingModeEntity> tracingModeEntities) {
        functionTracingConfigModes.removeAllItems();
        for(TracingModeEntity entity : tracingModeEntities) {
            functionTracingConfigModes.addItem(entity);
        }
    }
}
