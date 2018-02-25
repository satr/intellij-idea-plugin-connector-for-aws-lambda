package io.github.satr.idea.plugin.connector.la;

import io.github.satr.common.OperationValueResult;
import io.github.satr.idea.plugin.connector.la.entities.FunctionEntry;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

// Copyright © 2018, github.com/satr, MIT License

public class ConnectorModelTestCases extends ConnectorModelTestCasesBase {

    @Test
    public void getFunctions() throws Exception {
        OperationValueResult<List<FunctionEntry>> functionEntries = connectorModel.getFunctions();
        assertNotNull(functionEntries);
    }
}