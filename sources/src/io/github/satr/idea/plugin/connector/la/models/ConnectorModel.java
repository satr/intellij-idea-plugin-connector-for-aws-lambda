package io.github.satr.idea.plugin.connector.la.models;
// Copyright © 2018, github.com/satr, MIT License

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfilesConfigFile;
import com.amazonaws.auth.profile.internal.BasicProfile;
import com.amazonaws.profile.path.AwsProfileFileLocationProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.*;
import com.intellij.util.net.HttpConfigurable;
import io.github.satr.common.OperationResult;
import io.github.satr.common.OperationResultImpl;
import io.github.satr.common.OperationValueResult;
import io.github.satr.common.OperationValueResultImpl;
import io.github.satr.idea.plugin.connector.la.entities.CredentialProfileEntry;
import io.github.satr.idea.plugin.connector.la.entities.FunctionEntry;
import io.github.satr.idea.plugin.connector.la.entities.RegionEntry;
import io.github.satr.idea.plugin.connector.la.entities.RoleEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.apache.http.util.TextUtils.isEmpty;

public class ConnectorModel {
    private final Regions region;
    private final AmazonIdentityManagement identityManagementClient;
    private static final int MAX_FETCHED_ROLE_COUNT = 50;
    private AWSLambda awsLambdaClient;
    private static final Map<String, String> regionDescriptions;
    private ArrayList<RoleEntity> roleEntities;
    private Map<String, RoleEntity> roleEntityMap;

    static {
        regionDescriptions = createRegionDescriptionsMap();
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
        map.put("eu-central-1", "EU (Frankfurt)");
        map.put("eu-west-1", "EU (Ireland)");
        map.put("eu-west-2", "EU (London)");
        map.put("eu-west-3", "EU (Paris)");
        map.put("sa-east-1", "South America (Sao Paulo)");
        return map;
    }

    private ArrayList<RegionEntry> regionEntries;
    private String credentialProfileName;
    private String proxyDetails = "Unknown";

    public ConnectorModel(Regions region, String credentialProfileName) {
        this.region = region;
        this.credentialProfileName = credentialProfileName;
        AWSCredentialsProvider credentialsProvider = getCredentialsProvider(credentialProfileName);
        ClientConfiguration clientConfiguration = getClientConfiguration();
        awsLambdaClient = AWSLambdaClientBuilder.standard()
                .withRegion(region)
                .withClientConfiguration(clientConfiguration)
                .withCredentials(credentialsProvider)
                .build();
        identityManagementClient = AmazonIdentityManagementClientBuilder.standard()
                .withRegion(region)
                .withCredentials(credentialsProvider)
                .withClientConfiguration(clientConfiguration)
                .build();
        populateRoleListAndMap();
    }


    private ClientConfiguration getClientConfiguration() {
        HttpConfigurable httpConfigurable = HttpConfigurable.getInstance();
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        boolean useProxyAuto = httpConfigurable.USE_PROXY_PAC;
        if (useProxyAuto) {
            proxyDetails = "Auto";
        } else if (!httpConfigurable.USE_HTTP_PROXY) {
            proxyDetails = "Not used";
            return clientConfiguration;
        }
        String proxyHost = httpConfigurable.PROXY_HOST;
        int proxyPort = httpConfigurable.PROXY_PORT;
        clientConfiguration = clientConfiguration.withProxyHost(proxyHost)
                                                 .withProxyPort(proxyPort);
        if(!useProxyAuto) {
            proxyDetails = String.format("%s:%s", proxyHost, proxyPort);
        }

        if (httpConfigurable.PROXY_AUTHENTICATION) {
            String proxyLogin = httpConfigurable.getProxyLogin();
            clientConfiguration = clientConfiguration.withProxyPassword(httpConfigurable.getPlainProxyPassword())
                                                    .withProxyUsername(proxyLogin);
        }
        return clientConfiguration;
    }

    private AWSCredentialsProvider getCredentialsProvider(String credentialProfileName) {
        return validateCredentialProfile(credentialProfileName)
                                                        ? new ProfileCredentialsProvider(credentialProfileName)
                                                        : DefaultAWSCredentialsProviderChain.getInstance();
    }

    public OperationValueResult<List<FunctionEntry>> getFunctions(){
        final List<FunctionEntry> entries = new ArrayList<>();
        final OperationValueResult<List<FunctionEntry>> operationResult = new OperationValueResultImpl<List<FunctionEntry>>().withValue(entries);
        try {
            final ListFunctionsResult functionRequestResult = awsLambdaClient.listFunctions();
            for(FunctionConfiguration functionConfiguration : functionRequestResult.getFunctions()) {
                FunctionEntry functionEntry = createFunctionEntry(functionConfiguration, operationResult);
                entries.add(functionEntry);
            }
            operationResult.setValue(entries);
        } catch (com.amazonaws.services.lambda.model.AWSLambdaException e) {
            if("AccessDeniedException".equals(e.getErrorCode())) {
                operationResult.addError("User has not access to a list of functions.");
            } else {
                operationResult.addError(e.getMessage());
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return operationResult;
    }

    @NotNull
    private FunctionEntry createFunctionEntry(FunctionConfiguration functionConfiguration, OperationResult operationResult) {
        final String roleArn = functionConfiguration.getRole();
        final RoleEntity roleEntity = getRoleEntity(functionConfiguration.getFunctionName(), roleArn, operationResult);
        return new FunctionEntry(functionConfiguration, roleEntity);
    }

    @Nullable
    private RoleEntity getRoleEntity(String functionName, String roleArn, OperationResult operationResult) {
        RoleEntity roleEntity = getRoleEntity(roleArn);
        if (roleEntity != null) {
            return roleEntity;
        }
        roleEntity = addRoleToListAndMap(new Role().withArn(roleArn).withRoleName("-"));
        operationResult.addInfo("Added a role \"%s\" from a function \"%s\".",
                                roleArn, functionName);
        return roleEntity;
    }

    private RoleEntity getRoleEntity(String roleName) {
        return roleEntityMap.get(roleName);
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

    public OperationValueResult<FunctionEntry> updateWithJar(final FunctionEntry functionEntry, final String filePath) {
        return updateWithJar(functionEntry, new File(filePath));
    }

    public OperationValueResult<FunctionEntry> updateWithJar(final FunctionEntry functionEntity, final File file) {
        final OperationValueResultImpl<FunctionEntry> operationResult = new OperationValueResultImpl<>();
        validateLambdaFunctionJarFile(file, operationResult);
        if(operationResult.failed())
            return operationResult;

        try {
            final String readOnlyAccessFileMode = "r";
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, readOnlyAccessFileMode);
                 final FileChannel fileChannel = randomAccessFile.getChannel()) {
                    return updateFunctionCode(functionEntity, fileChannel);
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

    private OperationValueResult<FunctionEntry> updateFunctionCode(final FunctionEntry functionEntry, final FileChannel fileChannel) throws IOException {
        final OperationValueResultImpl<FunctionEntry> valueResult = new OperationValueResultImpl<>();
        try {
            final MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            buffer.load();
            final UpdateFunctionCodeRequest request = new UpdateFunctionCodeRequest()
                                                            .withFunctionName(functionEntry.getFunctionName())
                                                            .withZipFile(buffer);
            final UpdateFunctionCodeResult updateFunctionResult = awsLambdaClient.updateFunctionCode(request);
            final String roleName = updateFunctionResult.getRole();
            final RoleEntity roleEntity = getRoleEntity(updateFunctionResult.getFunctionName(), roleName, valueResult);
            if(roleEntity == null) {
                valueResult.addError("Not found role by name \"%s\"", roleName);
            } else {
                valueResult.setValue(new FunctionEntry(updateFunctionResult, roleEntity));
            }
        } catch (IOException e) {
            e.printStackTrace();
            valueResult.addError("Update function error: %s", e.getMessage());
        }
        return valueResult;
    }

    public OperationValueResult<String> invokeFunction(final String functionName, final String inputText) {
        OperationValueResult<String> operationResult = new OperationValueResultImpl<>();
        try {
            InvokeRequest invokeRequest = new InvokeRequest();
            invokeRequest.setFunctionName(functionName);
            invokeRequest.setPayload(inputText);
            final InvokeResult invokeResult = awsLambdaClient.invoke(invokeRequest);
            operationResult.addInfo("Invoked function \"%s\". Result status code: %d", functionName, invokeResult.getStatusCode());
            String functionError = invokeResult.getFunctionError();
            if(!isEmpty(functionError)){
                operationResult.addError(functionError);
            }
            String logResult = invokeResult.getLogResult();
            if(!isEmpty(logResult)){
                operationResult.addInfo(logResult);
            }
            ByteBuffer byteBuffer = invokeResult.getPayload();
            String rawJson = new String(byteBuffer.array(), "UTF-8");
            operationResult.setValue(rawJson);
        } catch (Exception e) {
            e.printStackTrace();
            operationResult.addError(e.getMessage());
        }
        return operationResult;
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

    public List<RegionEntry> getRegions() {
        if(regionEntries != null) {
            return regionEntries;
        }

        regionEntries = new ArrayList<>();
        for(Region region : RegionUtils.getRegions()) {
            String description = regionDescriptions.get(region.getName());
            if(description != null)
                regionEntries.add(new RegionEntry(region, description));
        }
        return regionEntries;
    }

    public OperationValueResult<List<CredentialProfileEntry>> getCredentialProfiles() {
        OperationValueResult<List<CredentialProfileEntry>> valueResult = new OperationValueResultImpl<>();
        ArrayList<CredentialProfileEntry> credentialProfilesEntries = new ArrayList<>();
        valueResult.setValue(credentialProfilesEntries);
        if(!validateCredentialProfilesExist()) {
            valueResult.addWarning("No credential profiles file found.\n To create one - please follow the instruction:\n https://docs.aws.amazon.com/cli/latest/userguide/cli-multiple-profiles.html ");
            return valueResult;
        }
        ProfilesConfigFile profilesConfigFile = new ProfilesConfigFile();
        Map<String, BasicProfile> profiles = profilesConfigFile.getAllBasicProfiles();
        for (String credentialProfileName : profiles.keySet()) {
            credentialProfilesEntries.add(new CredentialProfileEntry(credentialProfileName, profiles.get(credentialProfileName)));
        }
        valueResult.addInfo("Found %d credential profiles.", credentialProfilesEntries.size());
        return valueResult;
    }

    private boolean validateCredentialProfile(String credentialProfileName) {
        return !isEmpty(credentialProfileName)
                && validateCredentialProfilesExist()
                && new ProfilesConfigFile().getAllBasicProfiles().containsKey(credentialProfileName);
    }
    private boolean validateCredentialProfilesExist() {
        return AwsProfileFileLocationProvider.DEFAULT_CREDENTIALS_LOCATION_PROVIDER.getLocation() != null;
    }

    public Regions getRegion() {
        return region;
    }

    public String getCredentialProfileName() {
        return credentialProfileName;
    }

    public String getProxyDetails() {
        return proxyDetails;
    }

    public List<RoleEntity> getRoles() {
        return roleEntities;
    }

    private void populateRoleListAndMap() {
        roleEntities = new ArrayList<>();
        roleEntityMap = new LinkedHashMap<>();
        try {
            ListRolesRequest listRolesRequest = new ListRolesRequest().withMaxItems(MAX_FETCHED_ROLE_COUNT);
            ListRolesResult listRolesResult = identityManagementClient.listRoles(listRolesRequest);
            List<Role> roles = listRolesResult.getRoles();
            for (Role role: roles) {
                addRoleToListAndMap(role);
            }
        } catch (AmazonIdentityManagementException e) {
            if("AccessDenied".equals(e.getErrorCode())) {
                //"User has not access to a list of roles.
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private RoleEntity addRoleToListAndMap(Role role) {
        RoleEntity roleEntity = roleEntityMap.get(role.getArn());
        if (roleEntity != null) {
            return roleEntity;
        }
        roleEntity = new RoleEntity(role);
        roleEntityMap.put(role.getArn(), roleEntity);
        roleEntities.add(roleEntity);
        return roleEntity;
    }

    public OperationValueResult<FunctionEntry> getFunctionBy(String name) {
        GetFunctionRequest getFunctionRequest = new GetFunctionRequest().withFunctionName(name);
        GetFunctionResult function = awsLambdaClient.getFunction(getFunctionRequest);
        OperationValueResultImpl<FunctionEntry> valueResult = new OperationValueResultImpl<>();
        FunctionConfiguration functionConfiguration = function.getConfiguration();
        FunctionEntry functionEntry = createFunctionEntry(functionConfiguration, valueResult);
        valueResult.setValue(functionEntry);
        return valueResult;
    }

    public OperationResult updateConfiguration(FunctionEntry functionEntry) {
        OperationResultImpl operationResult = new OperationResultImpl();
        UpdateFunctionConfigurationRequest request = new UpdateFunctionConfigurationRequest()
                .withFunctionName(functionEntry.getFunctionName())
                .withDescription(functionEntry.getDescription())
                .withHandler(functionEntry.getHandler())
                .withTimeout(functionEntry.getTimeout())
                .withMemorySize(functionEntry.getMemorySize())
                .withRole(functionEntry.getRoleEntity().getArn())
                .withTracingConfig(functionEntry.getTracingModeEntity().getTracingConfig());
        try {
            awsLambdaClient.updateFunctionConfiguration(request);
        } catch (Exception e) {
            e.printStackTrace();
            operationResult.addError("Failed update of function configuration: %s", e.getMessage());
        }
        return operationResult;
    }
}
