package io.github.satr.idea.plugin.connector.la.models;
// Copyright © 2017, github.com/satr, MIT License

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.*;
import io.github.satr.common.OperationResult;
import io.github.satr.common.OperationValueResult;
import io.github.satr.common.OperationValueResultImpl;
import io.github.satr.idea.plugin.connector.la.entities.FunctionEntry;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ConnectorModel {
    private AWSLambda awsLambdaClient;

    public ConnectorModel() {
        awsLambdaClient = AWSLambdaClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
    }

    public List<FunctionEntry> getFunctions(){
        final ListFunctionsResult result = awsLambdaClient.listFunctions();
        final ArrayList<FunctionEntry> entries = new ArrayList<>();

        for(FunctionConfiguration configuration : result.getFunctions())
            entries.add(new FunctionEntry(configuration));

        return entries;
    }

    @Override
    protected void finalize() throws Throwable {
        shutdown();
        super.finalize();
    }

    public void shutdown() {
        if (awsLambdaClient == null)
            return;

        awsLambdaClient.shutdown();
        awsLambdaClient = null;
    }

    public OperationValueResult<FunctionEntry> updateWithJar(final String functionName, final String filePath) {
        return updateWithJar(functionName, new File(filePath));
    }

    public OperationValueResult<FunctionEntry> updateWithJar(final String functionName, final File file) {
        final OperationValueResultImpl<FunctionEntry> operationResult = new OperationValueResultImpl<>();
        validateLambdaFunctionJarFile(file, operationResult);
        if(operationResult.failed())
            return operationResult;

        try {
            final String readOnlyAccessFileMode = "r";
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, readOnlyAccessFileMode);
                 final FileChannel fileChannel = randomAccessFile.getChannel()) {
                    final FunctionEntry functionEntry = updateFunctionCode(functionName, fileChannel);
                    operationResult.setValue(functionEntry);
            }
        }catch (InvalidParameterValueException e) {
            operationResult.addError("Invalid request parameters: %s", e.getMessage());
        } catch (ResourceNotFoundException e) {
            operationResult.addError("Function not found.");
        } catch (Exception e) {
            e.printStackTrace();
            operationResult.addError(e.getMessage());
        }
        return operationResult;
    }

    private FunctionEntry updateFunctionCode(final String functionName, final FileChannel fileChannel) throws IOException {
        final MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
        buffer.load();
        final UpdateFunctionCodeRequest request = new UpdateFunctionCodeRequest()
                .withFunctionName(functionName)
                .withZipFile(buffer);
        final UpdateFunctionCodeResult result = awsLambdaClient.updateFunctionCode(request);
        return new FunctionEntry(result);
    }

    private void validateLambdaFunctionJarFile(File file, OperationResult operationResult) {
        if(!file.exists()) {
            operationResult.addError("JAR-file does not exist.");
            return;
        }

        try {
            final Object jarEntryEnumeration = new JarFile(file).entries();
            if(jarEntryEnumeration == null || !((Enumeration<JarEntry>)jarEntryEnumeration).hasMoreElements())
                operationResult.addError("The file is not a valid jar-file.");
        } catch (IOException e) {
            e.printStackTrace();
            operationResult.addError(e.getMessage());
        }
    }
}
