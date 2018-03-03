package io.github.satr.idea.plugin.connector.la;
// Copyright © 2018, github.com/satr, MIT License

import io.github.satr.common.OperationResult;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

//Tests expected an existing AWS Lambda function with a name "JavaFuncToTestConnector" in the region US-EAST-1.
//Alter the function name in the constant FUNC_NAME in this class, if needed.
//Alter the region if the ConnectorModel class, if needed.
public class UpdateFunctionWithJarTestCases extends ConnectorModelTestCasesBase {

    private final String FUNC_NAME = "JavaFuncToTestConnector";
    private final String INVALID_FUNC_NAME = "InvalidFunctionName";
    private final String FUNC_JAR_FILE_NAME = "resources/func-for-test.jar";
//    private final String NOT_FUNC_JAR_FILE_NAME = "resources/simple-app.jar";
    private final String INVALID_FUNC_JAR_FILE_NAME = "invalidFilePath";

    @Test
    public void failedUpdateWithInvalidFilePath() throws Exception {
        File appJarFile = new File(INVALID_FUNC_JAR_FILE_NAME);
        OperationResult result = connectorModel.updateWithJar(FUNC_NAME, appJarFile);
        assertFalse(result.success());
    }

    @Test
    public void updateWithValidFunctionJar() throws Exception {
        File lambdaFunctionJarFile = getResourceFile(FUNC_JAR_FILE_NAME);
        OperationResult result = connectorModel.updateWithJar(FUNC_NAME, lambdaFunctionJarFile);
        assertTrue(result.success());
    }

    @Test
    public void failedUpdateWithInvalidFunctionName() throws Exception {
        File lambdaFunctionJarFile = getResourceFile(FUNC_JAR_FILE_NAME);
        OperationResult result = connectorModel.updateWithJar(INVALID_FUNC_NAME, lambdaFunctionJarFile);
        assertFalse(result.success());
    }

//TODO: determine if this jar contains a class implementing com.amazonaws.services.lambda.runtime.RequestHandler
//    @Test
//    public void failedUpdateWithValidNotFunctionJar() throws Exception {
//        File jarFileLambdaFunction = getResourceFile(NOT_FUNC_JAR_FILE_NAME);
//        boolean updated = connectorModel.updateWithJar(FUNC_NAME, jarFileLambdaFunction.getCanonicalPath());
//        assertTrue(updated);
//    }
}
