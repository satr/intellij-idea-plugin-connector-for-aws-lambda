package io.github.satr.idea.plugin.connector.la.entities;

import com.amazonaws.regions.Region;

public class RegionEntity {
    private final String name;
    private final Region region;
    private final String description;

    public RegionEntity(Region region, String description) {
        this.name = region.getName();
        this.region = region;
        this.description = description;
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public String toString() {
        return String.format("%s : %s", name, description);
    }

    public String getName() {
        return name;
    }
}
