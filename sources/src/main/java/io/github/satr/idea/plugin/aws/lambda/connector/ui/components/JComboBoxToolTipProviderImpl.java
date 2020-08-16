package io.github.satr.idea.plugin.aws.lambda.connector.ui.components;

public class JComboBoxToolTipProviderImpl<T> implements JComboBoxToolTipProvider {
    final Object value;
    final String toolTip;
    private T entity;

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

    public JComboBoxToolTipProviderImpl withEntity(T entity) {
        this.entity = entity;
        return this;
    }

    @Override
    public T getEntity() {
        return entity;
    }
}
