package io.github.satr.idea.plugin.connector.la.entities;

import com.amazonaws.regions.Region;

public class RegionEntry {
    private final String name;
    private final Region region;
    private final String description;

    public RegionEntry(Region region, String description) {
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
