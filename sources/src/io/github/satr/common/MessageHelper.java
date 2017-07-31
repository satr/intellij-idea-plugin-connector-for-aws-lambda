package io.github.satr.common;
// Copyright © 2017, github.com/satr, MIT License

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;

public final class MessageHelper {
    public static void showInfo(final Project project, final String format, final Object... args) {
        showMessage(project, "Information", Messages.getInformationIcon(), format, args);
    }

    public static void showError(final Project project, final String format, final Object... args) {
        showMessage(project, "Error", Messages.getErrorIcon(), format, args);
    }

    private static void showMessage(final Project project, final String title, final Icon icon, final String format, final Object[] args) {
        Messages.showMessageDialog(project, String.format(format, args), title, icon);
    }
}
