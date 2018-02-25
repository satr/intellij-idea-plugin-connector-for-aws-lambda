package io.github.satr.idea.plugin.connector.la.entities;

import com.amazonaws.services.lambda.model.TracingMode;

import java.util.*;

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

    public TracingModeEntity(TracingMode tracingMode) {
        this.tracingMode = tracingMode;
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
}
