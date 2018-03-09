package io.github.satr.idea.plugin.connector.la.entities;

import com.amazonaws.services.cloudwatch.model.Dimension;

public class CloudWatchMetricDimensionEntity {

    private final String name;
    private final String value;

    public CloudWatchMetricDimensionEntity(Dimension dimension) {
        name = dimension.getName();
        value = dimension.getValue();
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", name, value);
    }
}
