package io.github.satr.idea.plugin.connector.la.models;
// Copyright © 2018, github.com/satr, MIT License

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfilesConfigFile;
import com.amazonaws.auth.profile.internal.BasicProfile;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.*;
import com.amazonaws.services.lambda.model.ResourceNotFoundException;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.*;
import io.github.satr.common.*;
import io.github.satr.idea.plugin.connector.la.entities.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.apache.http.util.TextUtils.isEmpty;

public class FunctionConnectorModel extends AbstractConnectorModel {
    private AWSLogs awsLogClient;
    private AWSLambda awsLambdaClient;
    private final int awsLogStreamItemsLimit = 50;
    private static final Map<String, String> regionDescriptions;

    static {
        regionDescriptions = createRegionDescriptionsMap();
    }

    private LastLogStreamState lastLogStreamState;

    private class LastLogStreamState {
        private String nextToken;
        private String functionName;

        private LastLogStreamState(String functionName) {
            this.functionName = functionName;
        }

        public String getNextToken() {
            return nextToken;
        }

        public LastLogStreamState setNextToken(String token) {
            this.nextToken = token;
            return this;
        }

        public boolean hasNextToken() {
            return !isEmpty(nextToken);
        }

        public boolean isForFunction(String functionName) {
            return this.functionName.equals(functionName);
        }
    }

    private static Map<String, String> createRegionDescriptionsMap() {
        HashMap<String, String> map = new LinkedHashMap<>();
        map.put("us-east-2", "US East (Ohio)");
        map.put("us-east-1", "US East (N. Virginia)");
        map.put("us-west-1", "US West (N. California)");
        map.put("us-west-2", "US West (Oregon)");
        map.put("ap-northeast-1", "Asia Pacific (Tokyo)");
        map.put("ap-northeast-2", "Asia Pacific (Seoul)");
        map.put("ap-south-1", "Asia Pacific (Mumbai)");
        map.put("ap-southeast-1", "Asia Pacific (Singapore)");
        map.put("ap-southeast-2", "Asia Pacific (Sydney)");
        map.put("ca-central-1", "Canada (Central)");
        map.put("cn-north-1", "China (Beijing)");
        map.put("cn-northwest-1", "China (Ningxia)");
        map.put("eu-central-1", "EU (Frankfurt)");
        map.put("eu-west-1", "EU (Ireland)");
        map.put("eu-west-2", "EU (London)");
        map.put("eu-west-3", "EU (Paris)");
        map.put("sa-east-1", "South America (Sao Paulo)");
        return map;
    }

    private ArrayList<RegionEntity> regionEntries;

    public FunctionConnectorModel(String regionName, String credentialProfileName, Logger logger) {
        super(regionName, credentialProfileName, logger);
        try {
            AWSCredentialsProvider credentialsProvider = getCredentialsProvider();
            ClientConfiguration clientConfiguration = getClientConfiguration();
            getLogger().logDebug("Build AWS Lambda client.");
            awsLambdaClient = AWSLambdaClientBuilder.standard()
                    .withRegion(getRegionName())
                    .withClientConfiguration(clientConfiguration)
                    .withCredentials(credentialsProvider)
                    .build();
            getLogger().logDebug("Build AWS Logs client.");
            awsLogClient = AWSLogsClientBuilder.standard()
                    .withRegion(getRegionName())
                    .withCredentials(credentialsProvider)
                    .withClientConfiguration(clientConfiguration)
                    .build();
            getLogger().logDebug("AWS Lambda and Logs clients created.");
        } catch (Exception e) {
            e.printStackTrace();
            logger.logError("Creating function connector failed: %s", e.getMessage());
            awsLambdaClient = AWSLambdaClientBuilder.standard().build();
            awsLogClient = AWSLogsClientBuilder.standard().build();
        }
    }


    public OperationValueResult<List<FunctionEntity>> getFunctions() {
        final List<FunctionEntity> entries = new ArrayList<>();
        final OperationValueResult<List<FunctionEntity>> result = new OperationValueResultImpl<List<FunctionEntity>>().withValue(entries);
        try {
            final ListFunctionsResult functionRequestResult = awsLambdaClient.listFunctions();
            for (FunctionConfiguration functionConfiguration : functionRequestResult.getFunctions()) {
                entries.add(createFunctionEntity(functionConfiguration));
            }
            result.setValue(entries);
        } catch (com.amazonaws.services.lambda.model.AWSLambdaException e) {
            if ("AccessDeniedException".equals(e.getErrorCode())) {
                reportErrorLoadingOfFunctionListFailed(result, e, "User has not access to a list of functions.");
            } else {
                reportErrorLoadingOfFunctionListFailed(result, e);
            }
        } catch (Exception e) {
            reportErrorLoadingOfFunctionListFailed(result, e);
        }
        return result;
    }

    private void reportErrorLoadingOfFunctionListFailed(OperationResult result, Exception e) {
        reportErrorLoadingOfFunctionListFailed(result, e, "");
    }

    public void reportErrorLoadingOfFunctionListFailed(OperationResult result,
                                                       Exception e, String additionalMessage) {
        e.printStackTrace();
        result.addError("Loading of a function list failed: %s %s", additionalMessage, e.getMessage());
    }

    @NotNull
    private FunctionEntity createFunctionEntity(FunctionConfiguration functionConfiguration) {
        return new FunctionEntity(functionConfiguration);
    }

    public OperationValueResult<FunctionEntity> updateWithArtifact(final FunctionEntity functionEntity, final String filePath) {
        return updateWithArtifact(functionEntity, new File(filePath));
    }

    public OperationValueResult<FunctionEntity> updateWithArtifact(final FunctionEntity functionEntity, final File file) {
        resetLastLogStreamNextToken();
        final OperationValueResultImpl<FunctionEntity> result = new OperationValueResultImpl<>();
        validateLambdaFunctionArtifactFile(file, result);
        if (result.failed())
            return result;

        try {
            final String readOnlyAccessFileMode = "r";
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, readOnlyAccessFileMode);
                 final FileChannel fileChannel = randomAccessFile.getChannel()) {
                OperationValueResult<FunctionEntity> functionCode = updateFunctionCode(functionEntity, fileChannel);
                randomAccessFile.close();
                return functionCode;
            }
        } catch (InvalidParameterValueException e) {
            reportErrorUpdateOfFunctionFailed(result, e,"invalid request parameters");
        } catch (ResourceNotFoundException e) {
            reportErrorUpdateOfFunctionFailed(result, e,"function not found");
        } catch (Exception e) {
            reportErrorUpdateOfFunctionFailed(result, e);
        }
        return result;
    }

    private OperationValueResult<FunctionEntity> updateFunctionCode(final FunctionEntity functionEntity, final FileChannel fileChannel) throws IOException {
        resetLastLogStreamNextToken();
        final OperationValueResultImpl<FunctionEntity> result = new OperationValueResultImpl<>();
        try {
            final MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            buffer.load();
            final UpdateFunctionCodeRequest request = new UpdateFunctionCodeRequest()
                    .withFunctionName(functionEntity.getFunctionName())
                    .withZipFile(buffer);
            final UpdateFunctionCodeResult updateFunctionResult = awsLambdaClient.updateFunctionCode(request);
            result.setValue(new FunctionEntity(updateFunctionResult));
        } catch (Exception e) {
            reportErrorUpdateOfFunctionFailed(result, e);
        }
        return result;
    }

    private void reportErrorUpdateOfFunctionFailed(OperationValueResultImpl<FunctionEntity> result, Exception e) {
        reportErrorUpdateOfFunctionFailed(result, e, "");
    }

    private void reportErrorUpdateOfFunctionFailed(OperationValueResultImpl<FunctionEntity> result, Exception e, String additionalMessage) {
        e.printStackTrace();
        result.addError("Update of the function failed: %s %s", additionalMessage, e.getMessage());
    }

    public OperationValueResult<String> invokeFunction(final String functionName, final String inputText) {
        OperationValueResult<String> operationResult = new OperationValueResultImpl<>();
        try {
            resetLastLogStreamNextToken();
            InvokeRequest invokeRequest = new InvokeRequest();
            invokeRequest.setFunctionName(functionName);
            invokeRequest.setPayload(inputText);
            final InvokeResult invokeResult = awsLambdaClient.invoke(invokeRequest);
            operationResult.addInfo("Invoked function \"%s\". Result status code: %d", functionName, invokeResult.getStatusCode());
            String functionError = invokeResult.getFunctionError();
            if (!isEmpty(functionError)) {
                operationResult.addError(functionError);
            }
            String logResult = invokeResult.getLogResult();
            if (!isEmpty(logResult)) {
                operationResult.addInfo(logResult);
            }
            ByteBuffer byteBuffer = invokeResult.getPayload();
            String rawJson = new String(byteBuffer.array(), "UTF-8");
            operationResult.setValue(rawJson);
        } catch (Exception e) {
            e.printStackTrace();
            operationResult.addError("Invocation of the function failed: %s", e.getMessage());
        }
        return operationResult;
    }

    public void resetLastLogStreamNextToken() {
        lastLogStreamState = null;
    }

    private void validateLambdaFunctionArtifactFile(File file, OperationResult result) {
        if (!file.exists()) {
            result.addError("Artifact file does not exist.");
            return;
        }

        try {
            if(file.getName().toLowerCase().endsWith(".jar")) {
                validateJarArtifact(file, result);
            } else if(file.getName().toLowerCase().endsWith(".zip")) {
                validateZipArtifact(file, result);
            }
        } catch (IOException e) {
            e.printStackTrace();
            result.addError("Validation of an artifact file failed: %s", e.getMessage());
        }
    }

    private void validateJarArtifact(File file, OperationResult result) throws IOException {
        final Object entityEnumeration = new JarFile(file).entries();
        if (entityEnumeration == null || !((Enumeration<JarEntry>) entityEnumeration).hasMoreElements())
            result.addError("The file is not a valid jar-file.");
    }

    private void validateZipArtifact(File file, OperationResult result) throws IOException {
        final Object entityEnumeration = new ZipFile(file).entries();
        if (entityEnumeration == null || !((Enumeration<ZipEntry>) entityEnumeration).hasMoreElements())
            result.addError("The file is not a valid zip-file.");
    }

    public List<RegionEntity> getRegions() {
        if (regionEntries != null) {
            return regionEntries;
        }

        regionEntries = new ArrayList<>();
        for (Region region : RegionUtils.getRegions()) {
            String description = regionDescriptions.get(region.getName());
            if (description != null)
                regionEntries.add(new RegionEntity(region, description));
        }
        return regionEntries;
    }

    public OperationValueResult<List<CredentialProfileEntity>> getCredentialProfileEntities() {
        OperationValueResult<List<CredentialProfileEntity>> valueResult = new OperationValueResultImpl<>();
        ArrayList<CredentialProfileEntity> credentialProfilesEntries = new ArrayList<>();
        valueResult.setValue(credentialProfilesEntries);
        if (!validateCredentialProfilesExist()) {
            valueResult.addWarning("No credential profiles file found.\n To create one - please follow the instruction:\n https://docs.aws.amazon.com/cli/latest/userguide/cli-multiple-profiles.html ");
            return valueResult;
        }
        ProfilesConfigFile profilesConfigFile = new ProfilesConfigFile();
        Map<String, BasicProfile> profiles = profilesConfigFile.getAllBasicProfiles();
        for (String credentialProfileName : profiles.keySet()) {
            credentialProfilesEntries.add(new CredentialProfileEntity(credentialProfileName, profiles.get(credentialProfileName)));
        }
        valueResult.addInfo("Found %d credential profiles.", credentialProfilesEntries.size());
        return valueResult;
    }

    public String getProxyDetails() {
        return proxyDetails;
    }

    public OperationValueResult<FunctionEntity> getFunctionBy(String name) {
        GetFunctionRequest getFunctionRequest = new GetFunctionRequest().withFunctionName(name);
        GetFunctionResult function = awsLambdaClient.getFunction(getFunctionRequest);
        OperationValueResultImpl<FunctionEntity> result = new OperationValueResultImpl<>();
        result.setValue(createFunctionEntity(function.getConfiguration()));
        return result;
    }

    public OperationResult updateConfiguration(FunctionEntity functionEntity) {
        resetLastLogStreamNextToken();
        OperationResultImpl operationResult = new OperationResultImpl();
        UpdateFunctionConfigurationRequest request = new UpdateFunctionConfigurationRequest()
                .withFunctionName(functionEntity.getFunctionName())
                .withDescription(functionEntity.getDescription())
                .withHandler(functionEntity.getHandler())
                .withTimeout(functionEntity.getTimeout())
                .withMemorySize(functionEntity.getMemorySize())
                .withRole(functionEntity.getRoleArn())
                .withTracingConfig(functionEntity.getTracingModeEntity().getTracingConfig());
        try {
            awsLambdaClient.updateFunctionConfiguration(request);
        } catch (Exception e) {
            e.printStackTrace();
            operationResult.addError("Failed update of function configuration: %s", e.getMessage());
        }
        return operationResult;
    }

    @NotNull
    public OperationValueResult<List<AwsLogStreamEntity>> getAwsLogStreamsFor(String functionName,
                                                                              AwsLogRequestMode awsLogRequestMode) {
        List<AwsLogStreamEntity> awsLogStreamEntities = new ArrayList<>();
        OperationValueResult<List<AwsLogStreamEntity>> result = new OperationValueResultImpl<>();
        result.setValue(awsLogStreamEntities);
        LogGroup logGroup = getLogGroupForAwsLambdaFunction(functionName);
        if(logGroup == null) {
            result.addInfo("Not found log group for the function \"%s\"", functionName);
            return result;
        }
        DescribeLogStreamsRequest describeLogStreamsRequest = new DescribeLogStreamsRequest()
                                                                    .withLogGroupName(logGroup.getLogGroupName())
                                                                    .withOrderBy(OrderBy.LastEventTime)
                                                                    .withDescending(true)
                                                                    .withLimit(awsLogStreamItemsLimit);
        if (awsLogRequestMode == AwsLogRequestMode.RequestNextSet
                && lastLogStreamState != null
                && lastLogStreamState.isForFunction(functionName)
                && lastLogStreamState.hasNextToken()  ) {
            describeLogStreamsRequest.withNextToken(lastLogStreamState.getNextToken());
        } else {
            lastLogStreamState = null;
        }
        //TODO move backward?
        DescribeLogStreamsResult describeLogStreamsResult = awsLogClient.describeLogStreams(describeLogStreamsRequest);
        String nextToken = describeLogStreamsResult.getNextToken();
        lastLogStreamState = getLastLogStreamStateFor(functionName).setNextToken(nextToken);
        List<LogStream> logStreams = describeLogStreamsResult.getLogStreams();
        for(LogStream logStream : logStreams) {
            awsLogStreamEntities.add(new AwsLogStreamEntity(logGroup.getLogGroupName(), logStream));
        }
        awsLogStreamEntities.sort(Comparator.comparing(AwsLogStreamEntity::getLastEventTime).reversed());
        return result;
    }

    public LastLogStreamState getLastLogStreamStateFor(String functionName) {
        return lastLogStreamState != null && lastLogStreamState.isForFunction(functionName)
               ? lastLogStreamState
               : new LastLogStreamState(functionName);
    }

    public OperationValueResult deleteAwsLogStreamsFor(String functionName) {
        resetLastLogStreamNextToken();
        OperationValueResult result = new OperationValueResultImpl();
        LogGroup logGroup = getLogGroupForAwsLambdaFunction(functionName);
        if(logGroup == null) {
            result.addError("Not found log group for the function \"%s\"", functionName);
            return result;
        }
        DeleteLogGroupResult deleteLogGroupResult = awsLogClient.deleteLogGroup(new DeleteLogGroupRequest(logGroup.getLogGroupName()));
        int httpStatusCode = deleteLogGroupResult.getSdkHttpMetadata().getHttpStatusCode();
        if (httpStatusCode == HttpStatusCode.OK.getCode()) {
            return result;
        }
        result.addError("Operation responded with code %d", httpStatusCode);
        return result;
    }

    private LogGroup getLogGroupForAwsLambdaFunction(String functionName) {
        DescribeLogGroupsRequest describeLogGroupsRequest = new DescribeLogGroupsRequest().withLogGroupNamePrefix("/aws/lambda");//add paging
        DescribeLogGroupsResult describeLogGroupsResult = this.awsLogClient.describeLogGroups(describeLogGroupsRequest);
        for (LogGroup logGroup : describeLogGroupsResult.getLogGroups()) {
            if (logGroup.getLogGroupName().endsWith("/" + functionName)) {
                return logGroup;
            }
        }
        return null;
    }

    public OperationValueResult<List<AwsLogStreamEventEntity>> getAwsLogStreamEventsFor(AwsLogStreamEntity awsLogStreamEntity) {
        OperationValueResultImpl<List<AwsLogStreamEventEntity>> operationResult = new OperationValueResultImpl<>();
        ArrayList<AwsLogStreamEventEntity> awsLogStreamEventEntities = new ArrayList<>();
        operationResult.setValue(awsLogStreamEventEntities);
        GetLogEventsResult logEventsResult = awsLogClient.getLogEvents(new GetLogEventsRequest()
                .withLogGroupName(awsLogStreamEntity.getLogGroupName())
                .withLogStreamName(awsLogStreamEntity.getLogStreamName()));
        for(OutputLogEvent event : logEventsResult.getEvents()) {
            awsLogStreamEventEntities.add(new AwsLogStreamEventEntity(event));
        }
        awsLogStreamEventEntities.sort(Comparator.comparing(AwsLogStreamEventEntity::getTimeStamp));
        return operationResult;
    }

    @Override
    public void shutdown() {
        if (awsLambdaClient != null) {
            awsLambdaClient.shutdown();
        }
        if (awsLogClient != null) {
            awsLogClient.shutdown();
        }
    }

    public enum AwsLogRequestMode {
        NewRequest,
        RequestNextSet
    }
}
