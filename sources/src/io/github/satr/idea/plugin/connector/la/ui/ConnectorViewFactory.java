package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2018, github.com/satr, MIT License

import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.model.AWSLambdaException;
import com.amazonaws.services.lambda.model.Runtime;
import com.amazonaws.services.logs.model.InvalidOperationException;
import com.intellij.compiler.server.BuildManagerListener;
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
import io.github.satr.common.DateTimeHelper;
import io.github.satr.common.MessageHelper;
import io.github.satr.common.OperationResult;
import io.github.satr.common.StringUtil;
import io.github.satr.idea.plugin.connector.la.entities.*;
import io.github.satr.idea.plugin.connector.la.models.ProjectModelImpl;
import io.github.satr.idea.plugin.connector.la.ui.components.JComboBoxItemToolTipRenderer;
import io.github.satr.idea.plugin.connector.la.ui.components.JComboBoxToolTipProvider;
import io.github.satr.idea.plugin.connector.la.ui.components.JComboBoxToolTipProviderImpl;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.spi.LoggingEvent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DateFormatter;
import java.awt.event.ItemEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

import static io.github.satr.common.StringUtil.getNotEmptyString;
import static org.apache.http.util.TextUtils.isEmpty;

public class ConnectorViewFactory implements ToolWindowFactory, ConnectorView, io.github.satr.common.Logger {
    static final org.apache.log4j.Logger uiLogger = LogManager.getLogger(ConnectorPresenter.class);
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
    private JPanel localLogPan;
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
    private JButton refreshFunctionConfiguration;
    private JTabbedPane tabLocalLog;
    private JPanel awsLogPan;
    private JList awsLogStreamList;
    private JList awsLogStreamEventList;
    private JCheckBox autoRefreshAwsLog;
    private JButton refreshAwsLogStreamList;
    private JTextField textProxyHost;
    private JTextField textProxyPort;
    private JCheckBox cbUseProxy;
    private boolean operationInProgress = false;
    private boolean setRegionOperationInProgress;
    private boolean runFunctionTestOperationInProgress;
    private final List<JButton> buttons = new ArrayList<>();
    private final DateFormatter dateFormatter = new DateFormatter();
    private Project project;
    private DefaultListModel<AwsLogStreamEntity> awsLogStreamListModel = new DefaultListModel<>();
    private DefaultListModel<AwsLogStreamEventEntity> awsLogStreamEventListModel = new DefaultListModel<>();

    public ConnectorViewFactory() {
        this(ServiceManager.getService(ConnectorPresenter.class));
    }

    public ConnectorViewFactory(final ConnectorPresenter presenter) {
        this.presenter = presenter;
        this.presenter.addLogger(this);
        this.presenter.setView(this);

        prepareButtons();
        prepareUiLogger();


        MessageBusConnection messageBusConnector = getMessageBusConnector();
        messageBusConnector.subscribe(UISettingsListener.TOPIC, (uiSettingsChanged) -> fixButtonsAfterPotentiallyChangedTheme());
        messageBusConnector.subscribe(BuildManagerListener.TOPIC, new BuildManagerListener() {
            @Override
            public void buildFinished(Project prj, UUID uuid, boolean b) {
                performAfterBuildActivity();
            }
        });


        functionRoles.setRenderer(new JComboBoxItemToolTipRenderer(functionRoles));

        refreshAllButton.addActionListener(e -> {
            runRefreshAll();
        });
        refreshFuncListButton.addActionListener(e -> {
            runRefreshFunctionList();
        });
        refreshFunctionConfiguration.addActionListener(e -> {
            runRefreshFunctionConfiguration();
        });
        refreshJarArtifactsButton.addActionListener(e -> {
            runRefreshJarArtifactList();
        });
        refreshRegionsButton.addActionListener(e -> {
            runRefreshRegionList();
        });
        refreshCredentialProfiles.addActionListener(e -> {
            runRefreshCredentialProfilesList();
        });
        updateFunctionButton.addActionListener(e -> {
            runUpdateFunction();
        });
        clearLogButton.addActionListener(e -> {
            clearLog();
        });
        runFunctionTestButton.addActionListener(e -> {
            runFunctionTest();
        });
        openFunctionInputFileButton.addActionListener(e -> {
            openFunctionTestInputFile();
        });
        testFunctionInputRecentFileList.addItemListener(e -> {
            runSetTestFunctionInputFromRecent(e);
        });
        functionList.addItemListener(e -> {
            runSetFunction(e);
        });
        regionList.addItemListener(e -> {
            runSetRegion(e);
        });
        credentialProfileList.addItemListener(e -> {
            runSetCredentialProfile(e);
        });
        jarArtifactList.addItemListener(e -> {
            runSetJarArtifact(e);
        });
        updateProxySettingsButton.addActionListener(e -> {
            updateProxySetting();
        });

        awsLogStreamList.addListSelectionListener(e -> {
            refreshAwsLogStreamEvents(e);
        });

        autoRefreshAwsLog.addChangeListener(e -> {
            runChangeAutoRefreshAwsLog(autoRefreshAwsLog.isSelected());
        });

        refreshAwsLogStreamList.addActionListener(e -> {
            runRefreshAwsLogStreams();
        });

        this.presenter.refreshTracingModeList();
        this.presenter.refreshJarArtifactList();
        runRefreshAll();
    }

    public void runChangeAutoRefreshAwsLog(boolean selected) {
        if(operationInProgress || setRegionOperationInProgress) {
            return;
        }
        runOperation(() -> this.presenter.setAutoRefreshAwsLog(selected), "Change Auto Refresh AWS Log Stream mode");
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
        setupButton(refreshFunctionConfiguration, IconLoader.getIcon("/icons/iconRefresh.png"));
        setupButton(refreshAwsLogStreamList, IconLoader.getIcon("/icons/iconRefresh.png"));
    }

    private void performAfterBuildActivity() {
        runRefreshJarArtifactList();
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

    private void openFunctionTestInputFile() {
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
        uiLogger.addAppender(new AsyncAppender(){
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
        uiLogger.setLevel(Level.INFO);
        logLevelList.setSelectedItem(Level.INFO);
        logLevelList.addItemListener(e -> {
            uiLogger.setLevel((Level) e.getItem());
        });
    }

    private void runRefreshAwsLogStreams() {
        if(operationInProgress || setRegionOperationInProgress) {
            return;
        }
        runOperation(() -> presenter.refreshAwsLogStreams(), "Refresh AWS Log Streams");
    }
    private void refreshAwsLogStreamEvents(ListSelectionEvent e) {
        if(operationInProgress || setRegionOperationInProgress) {
            return;
        }
        int selectedIndex = e.getFirstIndex();
        if(awsLogStreamListModel.size() < selectedIndex + 1){
            throw new InvalidOperationException(String.format("awsLogStreamListModel has less elements than selected index %d", selectedIndex));
        }
        AwsLogStreamEntity entity = awsLogStreamListModel.get(selectedIndex);
        runOperation(() -> presenter.setAwsLogStreamEventList(entity), "Refresh AWS Log Stream events: " + entity);
    }

    private void runSetRegion(ItemEvent e) {
        if(operationInProgress || setRegionOperationInProgress
                || e.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        RegionEntity entity = (RegionEntity)e.getItem();
        if(entity == null)
            return;
        runOperation(() -> presenter.setRegion(entity), "Select region: " + entity.toString());
    }

    private void runSetFunction(ItemEvent e) {
        if(operationInProgress || setRegionOperationInProgress
                || e.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        FunctionEntity entity = (FunctionEntity)e.getItem();
        runSetFunction(entity);
    }

    private void runSetFunction(FunctionEntity entity) {
        runOperation(() -> presenter.setFunction(entity), "Select function: " + entity.toString());
    }

    private void runSetTestFunctionInputFromRecent(ItemEvent e) {
        if(operationInProgress
                || e.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        TestFunctionInputEntity entity = (TestFunctionInputEntity)e.getItem();
        if(entity == null)
            return;
        runOperation(() -> presenter.setSetTestFunctionInputFromRecent(entity), "Select test function input from file: ", entity.getFileName());
    }

    private void runFunctionTest() {
        if(operationInProgress || runFunctionTestOperationInProgress) {
            return;
        }
        runOperation(() -> {
            runFunctionTestOperationInProgress = true;
            try {
                presenter.runFunctionTest(functionTestInputText.getText());
            } finally {
                runFunctionTestOperationInProgress = false;
            }
        }, "Run test of the function");
    }

    private void runSetCredentialProfile(ItemEvent e) {
        if(e.getStateChange() != ItemEvent.SELECTED)
            return;
        CredentialProfileEntity entity = (CredentialProfileEntity)e.getItem();
        if(entity == null) {
            return;
        }
        runOperation(() -> presenter.setCredentialProfile(entity), "Select credential profile: " + entity.toString());
    }


    private void updateProxySetting() {
        runOperation(() -> presenter.setProxySettings(), "Update proxy settings from IDEA settings.");
    }

    private void runSetJarArtifact(ItemEvent e) {
        if(e.getStateChange() != ItemEvent.SELECTED)
            return;
        ArtifactEntity entity = (ArtifactEntity) e.getItem();
        if(entity == null) {
            return;
        }
        runOperation(() -> presenter.setJarArtifact(entity), "Select JAR-artifact: " + entity.toString());
    }

    private void runUpdateFunction() {
        runOperation(() -> presenter.updateFunction(),
                    "Update selected AWS Lambda function with JAR-artifact");
    }

    private void clearLog() {
        logTextBox.setText("");
    }

    private void runRefreshAll() {
        runOperation(() -> presenter.refreshAll(), "Refresh all");
    }

    private void runRefreshFunctionList() {
        runOperation(() -> presenter.refreshFunctionList(), "Refresh list of AWS Lambda functions");
    }

    private void runRefreshFunctionConfiguration() {
        runOperation(() -> presenter.refreshFunctionConfiguration(), "Refresh AWS Lambda function configuration");
    }

    private void runRefreshJarArtifactList() {
        runOperation(() -> presenter.refreshJarArtifactList(), "Refresh list of JAR-artifacts in the project");
    }

    private void runRefreshRegionList() {
        runOperation(() -> presenter.refreshRegionList(), "Refresh list of AWS regions");
    }

    private void runRefreshCredentialProfilesList() {
        runOperation(() -> presenter.refreshCredentialProfilesList(), "Refresh list of credential profiles");
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
        functionDescription.setEnabled(enabled);
        functionHandler.setEnabled(enabled);
        functionRoles.setEnabled(enabled);
        functionTimeout.setEnabled(enabled);
        functionMemorySize.setEnabled(enabled);
        functionTracingConfigModes.setEnabled(enabled);
        refreshFunctionConfiguration.setEnabled(enabled);
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
        presenter.setProjectModel(new ProjectModelImpl(project));
        this.project = project;
    }

    @Override
    public void setFunctionList(List<FunctionEntity> functions, FunctionEntity selectedFunctionEntity) {
        functionList.removeAllItems();
        for (FunctionEntity entity : functions) {
            if (entity.getRuntime().equals(Runtime.Java8)) {
                functionList.addItem(entity);
            }
        }
        presenter.setFunction(selectedFunctionEntity);
    }

    @Override
    public void setArtifactList(Collection<? extends ArtifactEntity> artifacts, ArtifactEntity selectedArtifactEntity) {
        jarArtifactList.removeAllItems();
        for(ArtifactEntity artifactEntity : artifacts) {
            jarArtifactList.addItem(artifactEntity);
        }
        if(selectedArtifactEntity != null){
            jarArtifactList.setSelectedItem(selectedArtifactEntity);
        }
    }

    @Override
    public void setCredentialProfilesList(List<CredentialProfileEntity> credentialProfiles, String selectedCredentialsProfile) {
        credentialProfileList.removeAllItems();
        CredentialProfileEntity selectedCredentialsProfileEntity = null;
        for(CredentialProfileEntity entity : credentialProfiles){
            credentialProfileList.addItem(entity);
            if(!isEmpty(selectedCredentialsProfile) && entity.getName().equals(selectedCredentialsProfile))
                selectedCredentialsProfileEntity = entity;
        }
        if(selectedCredentialsProfileEntity != null) {
            credentialProfileList.setSelectedItem(selectedCredentialsProfileEntity);
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
                if(((RegionEntity) regionList.getItemAt(i)).getName().equals(region.getName())){
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
    public FunctionEntity getSelectedFunctionEntity() {
        return (FunctionEntity) functionList.getSelectedItem();
    }

    @Override
    public FunctionEntity getSelectedFunctionEntityWithUpdateConfiguration() {
        FunctionEntity functionEntity = getSelectedFunctionEntity();
        functionEntity.setDescription(getFunctionDescription());
        functionEntity.setArn(getFunctionArn());
        functionEntity.setHandler(getFunctionHandler());
        functionEntity.setRole(getFunctionRole());
        functionEntity.setTimeout(getFunctionTimeout());
        functionEntity.setMemorySize(getFunctionMemorySize());
        functionEntity.setTracingModeEntity(getFunctionTracingConfigMode());
        return functionEntity;
    }

    @Override
    public ArtifactEntity getSelectedArtifactEntity() {
        return (ArtifactEntity) jarArtifactList.getSelectedItem();
    }

    @Override
    public RegionEntity getSelectedRegionEntity() {
        return (RegionEntity) regionList.getSelectedItem();
    }

    @Override
    public CredentialProfileEntity getSelectedCredentialProfileEntity() {
        return (CredentialProfileEntity) credentialProfileList.getSelectedItem();
    }

    @Override
    public void logOperationResult(OperationResult operationResult) {
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
        uiLogger.debug(String.format(format, args));
    }

    @Override
    public void logInfo(String format, Object... args) {
        uiLogger.info(String.format(format, args));
    }

    @Override
    public void logWarning(String format, Object... args) {
        uiLogger.warn(String.format(format, args));
    }

    @Override
    public void logError(String format, Object... args) {
        uiLogger.error(String.format(format, args));
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
    public void setTestFunctionInputRecentEntityList(List<TestFunctionInputEntity> entries) {
        testFunctionInputRecentFileList.removeAllItems();
        for(TestFunctionInputEntity entity : entries){
            testFunctionInputRecentFileList.addItem(entity);
        }
        if(entries.size() > 0) {
            testFunctionInputRecentFileList.setSelectedIndex(entries.size() - 1);//select last added entity
        }
    }

    @Override
    public void setFunctionConfiguration(FunctionEntity functionEntity) {
        if (functionEntity == null) {
            clearFunctionConfiguration();
            return;
        }
        functionDescription.setText(functionEntity.getDescription());
        functionDescription.setToolTipText(functionEntity.getDescription());
        functionHandler.setText(functionEntity.getHandler());
        functionHandler.setToolTipText(functionEntity.getHandler());
        functionArn.setText(functionEntity.getArn());
        functionArn.setToolTipText(functionEntity.getArn());
        LocalDateTime lastModified = functionEntity.getLastModified();
        String format = DateTimeHelper.toFormattedString(lastModified);
        functionLastModified.setText(format);
        selectRoleInList(functionEntity.getRoleEntity());
        functionRoles.setToolTipText(functionEntity.getRoleEntity().toString());
        functionRuntime.setText(functionEntity.getRuntime().name());
        functionMemorySize.setText(functionEntity.getMemorySize().toString());
        functionTimeout.setText(functionEntity.getTimeout().toString());
        functionTracingConfigModes.setSelectedItem(functionEntity.getTracingModeEntity());
    }

    private void selectRoleInList(RoleEntity roleEntity) {
        for (int i = 0; i < functionRoles.getItemCount(); i++) {
            Object itemEntity = ((JComboBoxToolTipProvider) functionRoles.getItemAt(i)).getEntity();
            if(roleEntity.equals(itemEntity)) {
                functionRoles.setSelectedIndex(i);
                break;
            }
        }
    }

    private void clearFunctionConfiguration() {
        functionDescription.setText("");
        functionDescription.setToolTipText(null);
        functionHandler.setText("");
        functionHandler.setToolTipText(null);
        functionArn.setText("");
        functionArn.setToolTipText(null);
        functionLastModified.setText("");
        if(functionRoles.getItemCount() > 0) {
            functionRoles.setSelectedIndex(0);
        }
        functionRoles.setToolTipText(null);
        functionRuntime.setText("");
        functionMemorySize.setText("");
        functionTimeout.setText("");
        functionTracingConfigModes.setSelectedIndex(0);
    }

    @Override
    public void setRoleList(List<RoleEntity> roles) {
        functionRoles.removeAllItems();
        for(RoleEntity entity : roles) {
            functionRoles.addItem(new JComboBoxToolTipProviderImpl(entity.getName(), entity.toString()).withEntity(entity));
        }
    }

    @Override
    public void updateFunctionEntity(FunctionEntity functionEntity) {
        boolean itemSetToList = false;
        for (int i = 0; i < functionList.getItemCount(); i++) {
            FunctionEntity functionEntityItem = (FunctionEntity) functionList.getItemAt(i);
            if (!functionEntityItem.getFunctionName().equals(functionEntity.getFunctionName())) {
                continue;
            }
            Object selectedItem = functionList.getSelectedItem();
            functionList.removeItem(functionEntityItem);
            functionList.insertItemAt(functionEntity, i);
            if(functionEntityItem.equals(selectedItem)) {
                functionList.setSelectedItem(functionEntity);
            }
            itemSetToList = true;
            break;
        }
        if(!itemSetToList && functionEntity.getRuntime().equals(Runtime.Java8)) {
            functionList.addItem(functionEntity);
            itemSetToList = true;
        }
        if(!itemSetToList){
            return;
        }
        functionList.setSelectedItem(functionEntity);
    }

    @Override
    public void showError(String format, Object... args) {
        MessageHelper.showError(project, format, args);
    }

    @Override
    public void showInfo(String format, Object... args) {
        MessageHelper.showInfo(project, format, args);
    }

    @Override
    public void clearAwsLogStreamEventList() {
        setAwsLogStreamEventList(new ArrayList<>());
    }

    @Override
    public void clearAwsLogStreamList() {
        clearAwsLogStreamEventList();
        setAwsLogStreamList(new ArrayList<>());
    }

    @Override
    public void setAwsLogStreamList(List<AwsLogStreamEntity> awsLogEventEntities) {
        awsLogStreamListModel = new DefaultListModel<>();
        for(AwsLogStreamEntity entity : awsLogEventEntities) {
            awsLogStreamListModel.addElement(entity);
        }
        awsLogStreamList.setModel(awsLogStreamListModel);
    }

    @Override
    public void setAwsLogStreamEventList(List<AwsLogStreamEventEntity> awsLogStreamEventEntities) {
        awsLogStreamEventListModel = new DefaultListModel<>();
        for(AwsLogStreamEventEntity entity : awsLogStreamEventEntities) {
            awsLogStreamEventListModel.addElement(entity);
        }
        awsLogStreamEventList.setModel(awsLogStreamEventListModel);
    }

    @Override
    public void setRegionList(List<RegionEntity> regions, Regions selectedRegion) {
        regionList.removeAllItems();
        RegionEntity selectedRegionEntity = null;
        for(RegionEntity entity : regions) {
            regionList.addItem(entity);
            if(selectedRegion != null && entity.getRegion().getName().equals(selectedRegion.getName())) {
                selectedRegionEntity = entity;
            }
        }
        if(selectedRegionEntity != null) {
            regionList.setSelectedItem(selectedRegionEntity);
        }
    }

    @Override
    public void setTracingModeList(Collection<TracingModeEntity> tracingModeEntities) {
        functionTracingConfigModes.removeAllItems();
        for(TracingModeEntity entity : tracingModeEntities) {
            functionTracingConfigModes.addItem(entity);
        }
    }

    private String getFunctionDescription() {
        return StringUtil.getNotEmptyString(functionDescription.getText());
    }

    private String getFunctionArn() {
        return StringUtil.getNotEmptyString(functionArn.getText());
    }

    private String getFunctionHandler() {
        return StringUtil.getNotEmptyString(functionHandler.getText());
    }

    private int getFunctionTimeout() {
        return StringUtil.parseNotZeroInteger(functionTimeout.getText(), 15);
    }

    private int getFunctionMemorySize() {
        return StringUtil.parseNotZeroInteger(functionMemorySize.getText(), 512);
    }

    private TracingModeEntity getFunctionTracingConfigMode() {
        return (TracingModeEntity) functionTracingConfigModes.getSelectedItem();
    }

    private RoleEntity getFunctionRole() {
        Object selectedItem = functionRoles.getSelectedItem();
        return selectedItem == null ? null : (RoleEntity) ((JComboBoxToolTipProvider) selectedItem).getEntity();
    }
}
