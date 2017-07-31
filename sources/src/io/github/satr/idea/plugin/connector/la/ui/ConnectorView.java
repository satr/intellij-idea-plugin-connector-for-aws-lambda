package io.github.satr.idea.plugin.connector.la.ui;
// Copyright © 2017, github.com/satr, MIT License

import io.github.satr.idea.plugin.connector.la.entities.ArtifactEntry;
import io.github.satr.idea.plugin.connector.la.entities.FunctionEntry;

import java.util.Collection;
import java.util.List;

public interface ConnectorView {
    void setFunctionList(List<FunctionEntry> functions);
    void setArtifactList(Collection<? extends ArtifactEntry> artifacts);
}
