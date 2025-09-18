package io.github.satr.idea.plugin.connector.la;

import io.github.satr.common.OperationValueResult;
import io.github.satr.idea.plugin.connector.la.entities.FunctionEntity;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// Copyright © 2018, github.com/satr, MIT License

public class FunctionConnectorModelTestCases extends FunctionConnectorModelTestCasesBase {

    @Test
    public void getFunctions() throws Exception {
        OperationValueResult<List<FunctionEntity>> functionEntries = functionConnectorModel.getFunctions();
        assertNotNull(functionEntries);
    }
}