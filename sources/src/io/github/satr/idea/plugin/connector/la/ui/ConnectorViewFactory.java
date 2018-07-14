package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2018, github.com/satr, MIT License

import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.model.AWSLambdaException;
import com.amazonaws.services.lambda.model.Runtime;
import com.amazonaws.services.logs.model.InvalidOperationException;
import com.intellij.codeInspection.ui.RegExFormatter;
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
import io.github.satr.idea.plugin.connector.la.ui.components.RegexFormatter;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.spi.LoggingEvent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

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
    private JTabbedPane tabMainContent;
    private JTextArea localLogText;
    private JPanel tabPanSettings;
    private JTextPane txtStatus;
    private JButton refreshAllButton;
    private JPanel pnlToolBar;
    private JPanel pnlSettings;
    private JPanel pnlTabs;
    private JComboBox logLevelList;
    private JButton clearLocalLogButton;
    private JScrollPane localLogScroll;
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
    private JLabel functionSizeLimitsLink;
    private JLabel functionParameterConstraintsLink;
    private JScrollPane functionTestOutputTextScroll;
    private JCheckBox functionTestOutputWrapCheckBox;
    private JButton clearFunctionTestOutputButton;
    private JButton clearFunctionTestInputButton;
    private JCheckBox functionTestInputWrapCheckBox;
    private JButton deleteAwsLogStreamsButton;
    private JScrollPane functionTestInputTextScroll;
    private JCheckBox localLogWrapCheckBox;
    private JButton reformatJsonFunctionTestInputButton;
    private JButton reformatJsonFunctionTestOutputButton;
    private JButton functionConfigurationCollapseExpandButton;
    private JPanel functionConfigurationDetailsPanel;
    private JSplitPane mainSplitPanel;
    private JCheckBox autoFormatJsonFunctionTestOutputCheckBox;
    private JTextArea awsLogStreamEventMessageText;
    private JLabel awsLogStreamEventTimestamp;
    private JCheckBox awsLogStreamEventMessageWrapCheckBox;
    private JScrollPane awsLogStreamEventMessageScroll;
    private JTextField textProxyHost;
    private JTextField textProxyPort;
    private JCheckBox cbUseProxy;
    private boolean operationInProgress = false;
    private boolean setRegionOperationInProgress;
    private boolean runFunctionTestOperationInProgress;
    private final List<JButton> buttons = new ArrayList<>();
    private Project project;
    private final DefaultComboBoxModel<JComboBoxToolTipProvider<RoleEntity>> roleEntityListModel =  new DefaultComboBoxModel<>();
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
        prepareEditableFields();
        prepareHyperLinks();
        prepareUiLogger();
        prepareRolesList();
        subscribeToIdeaBusNotifications();

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
        clearLocalLogButton.addActionListener(e -> {
            clearLocalLog();
        });
        clearFunctionTestInputButton.addActionListener(e -> {
            clearFunctionTestInput();
        });
        clearFunctionTestOutputButton.addActionListener(e -> {
            clearFunctionTestOutput();
        });
        deleteAwsLogStreamsButton.addActionListener(e -> {
            runDeleteAwsLogStreams();
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
            refreshAwsLogStreamEvents(((JList) e.getSource()));
        });

        awsLogStreamEventList.addListSelectionListener(e -> {
            showAwsLogStreamEvent(((JList) e.getSource()));
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

        functionTestInputWrapCheckBox.addChangeListener(e -> {
            boolean wrap = ((JCheckBox) e.getSource()).isSelected();
            setWrapForFunctionTestInput(wrap);
        });
        functionTestOutputWrapCheckBox.addChangeListener(e -> {
            boolean wrap = ((JCheckBox) e.getSource()).isSelected();
            setWrapForFunctionTestOutput(wrap);
        });
        localLogWrapCheckBox.addChangeListener(e -> {
            boolean wrap = ((JCheckBox) e.getSource()).isSelected();
            setWrapForLocalLog(wrap);
        });
        awsLogStreamEventMessageWrapCheckBox.addChangeListener(e -> {
            boolean wrap = ((JCheckBox) e.getSource()).isSelected();
            setWrapForAwsLogStreamEventMessage(wrap);
        });
        reformatJsonFunctionTestInputButton.addActionListener(e -> {
            runReformatJsonFunctionTestInput();
        });
        reformatJsonFunctionTestOutputButton.addActionListener(e -> {
            runReformatJsonFunctionTestOutput();
        });
        collapseFunctionConfigurationDetails(true);
        functionConfigurationCollapseExpandButton.addActionListener(e -> {
            collapseFunctionConfigurationDetails(functionConfigurationDetailsPanel.isVisible());
        });
        autoFormatJsonFunctionTestOutputCheckBox.addChangeListener(e -> {
            boolean autoFormat = ((JCheckBox) e.getSource()).isSelected();
            if(autoFormat) {
                runReformatJsonFunctionTestOutput();
            }
        });
        setFunctionTestInput("{\"name\":\"value\"}");
    }

    private void collapseFunctionConfigurationDetails(boolean collapse) {
        functionConfigurationDetailsPanel.setVisible(!collapse);
        functionSizeLimitsLink.setVisible(!collapse);
        functionParameterConstraintsLink.setVisible(!collapse);
        refreshFunctionConfiguration.setVisible(!collapse);
        setupButton(functionConfigurationCollapseExpandButton, collapse ? IconLoader.getIcon("/icons/iconExpand.png")
                                                                : IconLoader.getIcon("/icons/iconCollapse.png"));
        mainSplitPanel.getUI().resetToPreferredSizes(mainSplitPanel);
    }


    public void setWrapForFunctionTestInput(boolean wrap) {
        setWrapForScrollableTextArea(wrap, functionTestInputText, functionTestInputTextScroll);
    }

    public void setWrapForFunctionTestOutput(boolean wrap) {
        setWrapForScrollableTextArea(wrap, functionTestOutputText, functionTestOutputTextScroll);
    }

    public void setWrapForLocalLog(boolean wrap) {
        setWrapForScrollableTextArea(wrap, localLogText, localLogScroll);
    }

    public void setWrapForAwsLogStreamEventMessage(boolean wrap) {
        setWrapForScrollableTextArea(wrap, awsLogStreamEventMessageText, awsLogStreamEventMessageScroll);
    }

    public void setWrapForScrollableTextArea(boolean wrap, JTextArea textArea, JScrollPane scrollPane) {
        scrollPane.setHorizontalScrollBarPolicy(wrap ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        textArea.setLineWrap(wrap);
        textArea.setWrapStyleWord(wrap);
    }

    public void prepareHyperLinks() {
        initHyperlinkLabel(functionSizeLimitsLink,
                "https://docs.aws.amazon.com/lambda/latest/dg/limits.html",
                " Size limits ", "Size limits for a code file and space, allocated for a function");
        initHyperlinkLabel(functionParameterConstraintsLink,
                "https://docs.aws.amazon.com/lambda/latest/dg/API_UpdateFunctionConfiguration.html",
                " Constraints ", "Constraints of parameters");
    }

    private void prepareEditableFields() {
        //The name of the function. Note that the length constraint applies only to the ARN. If you specify only the function name, it is limited to 64 characters in length.
//fix cleared previous value, when invalid new value was entered
//        functionHandler.setFormatterFactory(getRegexFormatterFactory("[^\\s]{1,128}"));
//        functionDescription.setFormatterFactory(getRegexFormatterFactory("\\w{0,256}"));
        functionHandler.setToolTipText("1 to 128 characters, spaces are not allowed");
        functionDescription.setToolTipText("Optional. 0 to 256 characters");
        functionTimeout.setFormatterFactory(getIntegerFormatterFactoryFor(1, 30000));
        functionTimeout.setToolTipText("Timeout: 1 to 30000 s (default: 3 s)\nDefault Max: 300 s - it might depend on account's constraints");
        functionMemorySize.setFormatterFactory(getIntegerFormatterFactoryFor(128, 3008));
        functionMemorySize.setToolTipText("Memory Size: 128 to 3008 MB (default: 64 MB)");
    }

    @NotNull
    private DefaultFormatterFactory getIntegerFormatterFactoryFor(int minValue, int maxValue) {
        NumberFormatter integerFormat = new NumberFormatter(NumberFormat.getIntegerInstance());
        integerFormat.setMinimum(new Integer(minValue));
        integerFormat.setMaximum(new Integer(maxValue));
        return new DefaultFormatterFactory(integerFormat, integerFormat, integerFormat);
    }

    private JFormattedTextField.AbstractFormatterFactory getRegexFormatterFactory(String pattern) {
        RegExFormatter formatter = new RegExFormatter();
        formatter.setAllowsInvalid(false);
        return new DefaultFormatterFactory(new RegexFormatter(pattern));
    }

    public void subscribeToIdeaBusNotifications() {
        MessageBusConnection messageBusConnector = getMessageBusConnector();
        messageBusConnector.subscribe(UISettingsListener.TOPIC, (uiSettingsChanged) -> fixButtonsAfterPotentiallyChangedTheme());
        messageBusConnector.subscribe(BuildManagerListener.TOPIC, new BuildManagerListener() {
            @Override
            public void buildFinished(Project prj, UUID uuid, boolean b) {
                performAfterBuildActivity();
            }
        });
    }

    public void prepareRolesList() {
        functionRoles.setModel(roleEntityListModel);
        functionRoles.setRenderer(new JComboBoxItemToolTipRenderer(functionRoles));
        functionRoles.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                if(!presenter.roleListLoaded()) {
                    runInitializeFunctionRoleList();
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });
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
        setupButton(clearLocalLogButton, IconLoader.getIcon("/icons/iconClearLog.png"));
        setupButton(clearFunctionTestInputButton, IconLoader.getIcon("/icons/iconClearLog.png"));
        setupButton(clearFunctionTestOutputButton, IconLoader.getIcon("/icons/iconClearLog.png"));
        setupButton(deleteAwsLogStreamsButton, IconLoader.getIcon("/icons/iconClearLog.png"));
        setupButton(refreshFuncListButton, IconLoader.getIcon("/icons/iconRefresh.png"));
        setupButton(refreshJarArtifactsButton, IconLoader.getIcon("/icons/iconRefresh.png"));
        setupButton(refreshRegionsButton, IconLoader.getIcon("/icons/iconRefresh.png"));
        setupButton(refreshCredentialProfiles, IconLoader.getIcon("/icons/iconRefresh.png"));
        setupButton(refreshFunctionConfiguration, IconLoader.getIcon("/icons/iconRefresh.png"));
        setupButton(refreshAwsLogStreamList, IconLoader.getIcon("/icons/iconRefresh.png"));
        setupButton(reformatJsonFunctionTestInputButton, IconLoader.getIcon("/icons/iconReformatJson.png"));
        setupButton(reformatJsonFunctionTestOutputButton, IconLoader.getIcon("/icons/iconReformatJson.png"));
        setupButton(functionConfigurationCollapseExpandButton, IconLoader.getIcon("/icons/iconExpand.png"));
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
        if(!buttons.contains(button)) {
            buttons.add(button);
        }
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

    private void initHyperlinkLabel(JLabel label, final String url, String text) {
        initHyperlinkLabel(label, url, text, null);
    }

    private void initHyperlinkLabel(JLabel label, final String url, String text, String toolTip) {
        label.setText("<html><a href=\"\">"+text+"</a></html>");
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.setToolTipText(isEmpty(toolTip) ? url : toolTip);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(new URI(url));
                    } catch (URISyntaxException | IOException ex) {
                    }
                }
            }
        });
    }
    private void prepareUiLogger() {
        uiLogger.addAppender(new AsyncAppender(){
            @Override
            public void append(LoggingEvent event) {
                super.append(event);
                localLogText.append(String.format("\n%s: %s", event.getLevel(), event.getMessage()));
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

    private void refreshAwsLogStreamEvents(JList source) {
        if(operationInProgress || setRegionOperationInProgress) {
            return;
        }
        int selectedIndex = source.getSelectedIndex();
        if(awsLogStreamListModel.size() < selectedIndex + 1) {
            throw new InvalidOperationException(String.format("awsLogStreamListModel has less elements than selected index %d", selectedIndex));
        }
        AwsLogStreamEntity entity = awsLogStreamListModel.get(selectedIndex);
        runOperation(() -> presenter.setAwsLogStreamEventList(entity), "Refresh AWS Log Stream events: " + entity);
    }

    private void showAwsLogStreamEvent(JList source) {
        if(operationInProgress) {
            return;
        }
        int selectedIndex = source.getSelectedIndex();
        if(awsLogStreamEventListModel.size() < selectedIndex + 1) {
            throw new InvalidOperationException(String.format("awsLogStreamEventListModel has less elements than selected index %d", selectedIndex));
        }
        AwsLogStreamEventEntity entity = awsLogStreamEventListModel.get(selectedIndex);
        runOperation(() -> presenter.setAwsLogStreamEvent(entity), "Set AWS Log Stream event.");
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

    private void runReformatJsonFunctionTestInput() {
        if(operationInProgress || runFunctionTestOperationInProgress) {
            return;
        }
        runOperation(() -> {
            presenter.reformatJsonFunctionTestInput(functionTestInputText.getText());
        }, "Reformat JSON function test input");
    }

    private void runReformatJsonFunctionTestOutput() {
        if(operationInProgress || runFunctionTestOperationInProgress) {
            return;
        }
        runOperation(() -> {
            presenter.reformatJsonFunctionTestOutput(functionTestOutputText.getText());
        }, "Reformat JSON function test output");
    }

    private void runFunctionTest() {
        if(operationInProgress || runFunctionTestOperationInProgress) {
            return;
        }
        runOperation(() -> {
            runFunctionTestOperationInProgress = true;
            try {
                presenter.runFunctionTest(functionTestInputText.getText(), isAutoFormatFunctionTestOutput());
            } finally {
                runFunctionTestOperationInProgress = false;
            }
        }, "Run test of the function");
    }

    private boolean isAutoFormatFunctionTestOutput() {
        return autoFormatJsonFunctionTestOutputCheckBox.isSelected();
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

    private void clearLocalLog() {
        localLogText.setText("");
    }

    private void clearFunctionTestInput() {
        functionTestInputText.setText("");
    }

    private void clearFunctionTestOutput() {
        functionTestOutputText.setText("");
    }

    private void runDeleteAwsLogStreams() {
        runOperation(() -> presenter.deleteAwsLogStreams(), "Delete AWS log streams.");
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

    private void runInitializeFunctionRoleList() {
        runOperation(() -> {
            if(presenter.initializeFunctionRoleList()) {
                JPopupMenu popupMenu = functionRoles.getComponentPopupMenu();
                if(popupMenu != null) {
                    popupMenu.invalidate();
                }
                functionRoles.hidePopup();
            }
        }, "Initialize function role list");
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
        functionEntity.setRoleArn(getFunctionRole().getArn());
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
    public void setFunctionTestInput(String inputText) {
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
        functionHandler.setText(functionEntity.getHandler());
        functionArn.setText(functionEntity.getArn());
        functionArn.setToolTipText(functionEntity.getArn());
        LocalDateTime lastModified = functionEntity.getLastModified();
        String format = DateTimeHelper.toFormattedString(lastModified);
        functionLastModified.setText(format);
        selectRoleInList(functionEntity.getRoleArn());
        functionRoles.setToolTipText(functionEntity.getRoleArn());
        functionRuntime.setText(functionEntity.getRuntime().name());
        functionMemorySize.setValue(new Integer(functionEntity.getMemorySize()));
        functionTimeout.setValue(new Integer(functionEntity.getTimeout().toString()));
        functionTracingConfigModes.setSelectedItem(functionEntity.getTracingModeEntity());
    }

    private void selectRoleInList(String roleArn) {
        for (int i = 0; i < roleEntityListModel.getSize(); i++) {
            JComboBoxToolTipProvider<RoleEntity> item = roleEntityListModel.getElementAt(i);
            if (!roleArn.equals(item.getEntity().getArn())) {
                continue;
            }
            roleEntityListModel.setSelectedItem(item);
            functionRoles.setToolTipText(item.getEntity().toString());
            return;
        }
    }

    private void clearFunctionConfiguration() {
        functionDescription.setText("");
        functionHandler.setText("");
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
    public void clearRoleList() {
        roleEntityListModel.removeAllElements();
    }

    @Override
    public void setAwsLogStreamEvent(String timestamp, String message) {
        awsLogStreamEventTimestamp.setText(timestamp);
        awsLogStreamEventMessageText.setText(message);
    }

    @Override
    public void setRoleList(List<RoleEntity> roles, RoleEntity selectedRoleEntity) {
        roleEntityListModel.removeAllElements();
        JComboBoxToolTipProvider<RoleEntity> itemToSelect = null;
        for(RoleEntity roleEntity : roles) {
            JComboBoxToolTipProvider<RoleEntity> item = createJComboBoxToolTipProviderFor(roleEntity);
            if(item.getEntity().equals(selectedRoleEntity)){
                itemToSelect = item;
            }
            roleEntityListModel.addElement(item);
        }
        if(itemToSelect != null) {
            roleEntityListModel.setSelectedItem(itemToSelect);
        }
    }

    @NotNull
    public JComboBoxToolTipProvider<RoleEntity> createJComboBoxToolTipProviderFor(RoleEntity roleEntity) {
        return new JComboBoxToolTipProviderImpl<>(roleEntity.getName(), roleEntity.toString()).withEntity(roleEntity);
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
        setAwsLogStreamEvent("", "");
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
