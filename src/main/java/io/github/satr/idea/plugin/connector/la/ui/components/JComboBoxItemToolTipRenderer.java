package io.github.satr.idea.plugin.connector.la.ui.components;

import javax.swing.*;
import java.awt.*;

public class JComboBoxItemToolTipRenderer extends DefaultListCellRenderer {

    private final JComboBox comboBox;

    public JComboBoxItemToolTipRenderer(JComboBox comboBox) {
        this.comboBox = comboBox;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        JComponent component = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected,
                cellHasFocus);
        String toolTipText = null;
        if (value instanceof JComboBoxToolTipProvider) {
            JComboBoxToolTipProvider toolTipProvider = (JComboBoxToolTipProvider) value;
            toolTipText = toolTipProvider.getToolTip();
        }
        list.setToolTipText(toolTipText);
        comboBox.setToolTipText(toolTipText);
        return component;
    }
}