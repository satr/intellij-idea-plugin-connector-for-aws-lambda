package io.github.satr.idea.plugin.aws.lambda.connector.models;
// Copyright © 2020, github.com/satr, MIT License

import io.github.satr.idea.plugin.aws.lambda.connector.entities.ArtifactEntity;

import java.util.List;

public interface ProjectModel {
    List<? extends ArtifactEntity> getArtifacts();
}
