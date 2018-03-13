package io.github.satr.idea.plugin.connector.la;
// Copyright © 2018, github.com/satr, MIT License

import com.amazonaws.regions.Regions;
import io.github.satr.common.Constant;
import io.github.satr.idea.plugin.connector.la.models.FunctionConnectorModel;
import org.junit.After;
import org.junit.Before;

import java.io.File;

public class FunctionConnectorModelTestCasesBase {
    protected FunctionConnectorModel functionConnectorModel;

    @Before
    public void setUp() throws Exception {
        functionConnectorModel = new FunctionConnectorModel(Regions.US_EAST_1, Constant.CredentialProfile.DEFAULT);
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
