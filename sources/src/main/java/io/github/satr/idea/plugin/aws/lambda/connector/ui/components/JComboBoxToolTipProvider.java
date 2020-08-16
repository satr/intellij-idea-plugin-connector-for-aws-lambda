package io.github.satr.idea.plugin.aws.lambda.connector.ui.components;

public interface JComboBoxToolTipProvider<T> {
    String getToolTip();
    T getEntity();
}
