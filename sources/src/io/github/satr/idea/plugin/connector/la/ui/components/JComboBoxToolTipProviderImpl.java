package io.github.satr.idea.plugin.connector.la.ui.components;

public class JComboBoxToolTipProviderImpl implements JComboBoxToolTipProvider {
    final Object value;
    final String toolTip;

    public JComboBoxToolTipProviderImpl(Object value, String toolTip) {
        this.value = value;
        this.toolTip = toolTip;
    }

    @Override
    public String getToolTip() {
        return toolTip;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
