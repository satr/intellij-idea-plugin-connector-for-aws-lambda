package io.github.satr.common;
// Copyright © 2017, github.com/satr, MIT License

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;

public final class MessageHelper {
    private static final Logger log = Logger.getInstance(MessageHelper.class);
    private static final WindowManager windowManager = WindowManager.getInstance();
    private static final JBPopupFactory jbPopupFactory = JBPopupFactory.getInstance();

    public static void showInfo(final Project project, final String format, final Object... args) {
        showMessage(project, MessageType.INFO, format, args);
    }

    public static void showError(final Project project, final String format, final Object... args) {
        showMessage(project, MessageType.ERROR, format, args);
    }

    private static void showMessage(final Project project, final MessageType messageType, final String format, final Object[] args) {
        StatusBar statusBar = windowManager.getStatusBar(project);
        String message = String.format(format, args);
        jbPopupFactory.createHtmlTextBalloonBuilder(message, messageType, null)
                    .setFadeoutTime(7500)
                    .createBalloon()
                    .show(RelativePoint.getNorthEastOf(statusBar.getComponent()), Balloon.Position.atRight);

        if(messageType == MessageType.INFO)
            log.info(message);
        else if(messageType == MessageType.WARNING)
            log.warn(message);
        else if(messageType == MessageType.ERROR)
            log.error(message);
        else
            log.debug(message);
    }

    public static void showError(Project project, Throwable t) {
        showError(project, t.getMessage());
        log.error(t);
    }
}
