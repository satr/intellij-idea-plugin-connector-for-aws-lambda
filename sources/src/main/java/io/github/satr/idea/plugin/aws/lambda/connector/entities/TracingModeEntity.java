package io.github.satr.idea.plugin.aws.lambda.connector.entities;

import com.amazonaws.services.lambda.model.TracingConfig;
import com.amazonaws.services.lambda.model.TracingMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TracingModeEntity {
    private final TracingMode tracingMode;
    private final String name;
    private final static Map<String, TracingModeEntity> tracingModeEntityMap;
    private final static List<TracingModeEntity> tracingModeEntities;
    private final static TracingModeEntity defaultTracingModeEntity;

    static {
        tracingModeEntityMap = new LinkedHashMap<>();
        tracingModeEntities = new ArrayList<>();
        for(TracingMode tracingMode : TracingMode.values()) {
            TracingModeEntity entity = new TracingModeEntity(tracingMode);
            tracingModeEntityMap.put(tracingMode.name(), entity);
            tracingModeEntities.add(entity);
        }
        defaultTracingModeEntity = tracingModeEntities.size() > 0 ? tracingModeEntities.get(0) : null;
    }

    private final TracingConfig tracingConfig = new TracingConfig();

    public TracingModeEntity(TracingMode tracingMode) {
        this.tracingMode = tracingMode;
        tracingConfig.setMode(tracingMode);
        name = tracingMode.name();
    }

    public String getName() {
        return name;
    }

    public TracingMode getTracingMode() {
        return tracingMode;
    }

    @Override
    public String toString() {
        return String.format("%s", name);
    }

    public static List<TracingModeEntity> values() {
        return tracingModeEntities;
    }

    public static TracingModeEntity fromValue(String tracingModeName) {
        return tracingModeEntityMap.getOrDefault(tracingModeName, defaultTracingModeEntity);
    }

    public TracingConfig getTracingConfig() {
        return tracingConfig.withMode(getTracingMode());
    }
}
