package io.github.satr.idea.plugin.connector.la;
// Copyright © 2018, github.com/satr, MIT License

import com.amazonaws.regions.Regions;
import io.github.satr.common.Constant;
import io.github.satr.common.Logger;
import io.github.satr.idea.plugin.connector.la.models.FunctionConnectorModel;
import org.junit.After;
import org.junit.Before;

import java.io.File;

import static org.mockito.Mockito.mock;

public class FunctionConnectorModelTestCasesBase {
    protected FunctionConnectorModel functionConnectorModel;
    Logger logger;

    @Before
    public void setUp() throws Exception {
        logger = mock(Logger.class);
        functionConnectorModel = new FunctionConnectorModel(Regions.US_EAST_1.getName(), Constant.CredentialProfile.DEFAULT, logger);
    }

    @After
    public void tearDown() throws Exception {
        functionConnectorModel.shutdown();
    }

    protected File getResourceFile(String fileRelativePath) {
        ClassLoader classLoader = FunctionConnectorModelTestCasesBase.class.getClassLoader();
        return new File(classLoader.getResource(fileRelativePath).getFile());
    }
}
