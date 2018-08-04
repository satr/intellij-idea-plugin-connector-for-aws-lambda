package io.github.satr.idea.plugin.connector.la.models;
// Copyright © 2018, github.com/satr, MIT License

import io.github.satr.idea.plugin.connector.la.entities.ArtifactEntity;

import java.util.List;

public interface ProjectModel {
    List<? extends ArtifactEntity> getArtifacts();
}
