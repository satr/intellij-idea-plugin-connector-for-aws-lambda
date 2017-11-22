package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2017, github.com/satr, MIT License

import com.amazonaws.regions.Regions;
import io.github.satr.idea.plugin.connector.la.entities.ArtifactEntry;
import io.github.satr.idea.plugin.connector.la.entities.CredentialProfileEntry;
import io.github.satr.idea.plugin.connector.la.entities.FunctionEntry;
import io.github.satr.idea.plugin.connector.la.entities.RegionEntry;

import java.util.Collection;
import java.util.List;

public interface ConnectorView {
    void setFunctionList(List<FunctionEntry> functions);
    void setArtifactList(Collection<? extends ArtifactEntry> artifacts);
    void setRegionList(List<RegionEntry> regions, Regions selectedRegion);
    void setCredentialProfilesList(List<CredentialProfileEntry> credentialProfiles, String selectedCredentialsProfile);
    void setRegion(Regions region);
    void refreshStatus(String function, String artifact, String region, String credentialProfile);
}
