package io.github.satr.idea.plugin.connector.la.entities;

import com.amazonaws.auth.profile.internal.BasicProfile;

import static org.apache.http.util.TextUtils.isEmpty;

public class CredentialProfileEntry {
    private final String name;
    private final BasicProfile basicProfile;

    public CredentialProfileEntry(String name, BasicProfile basicProfile) {
        this.name = name;
        this.basicProfile = basicProfile;
    }

    public String getName() {
        return name;
    }

    public BasicProfile getBasicProfile() {
        return basicProfile;
    }

    @Override
    public String toString() {
        String regionName = getBasicProfile().getRegion();
        return String.format("%s (%s)", getName(), isEmpty(regionName) ? "no region" : regionName);
    }
}
