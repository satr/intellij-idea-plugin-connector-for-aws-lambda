package io.github.satr.idea.plugin.connector.la.entities;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Metric;

import java.util.ArrayList;
import java.util.List;

public class CloudWatchMetricEntity {

    private final String metricName;
    private final String metricNamespace;
    private final List<CloudWatchMetricDimensionEntity> metricDimensionEntityList = new ArrayList<>();

    public CloudWatchMetricEntity(Metric metric) {
        metricName = metric.getMetricName();
        metricNamespace = metric.getNamespace();
        for(Dimension dimension : metric.getDimensions()) {
            metricDimensionEntityList.add(new CloudWatchMetricDimensionEntity(dimension));
        }
    }

    public String getMetricName() {
        return metricName;
    }

    public String getMetricNamespace() {
        return metricNamespace;
    }

    public List<CloudWatchMetricDimensionEntity> getMetricDimensionEntityList() {
        return metricDimensionEntityList;
    }
}
