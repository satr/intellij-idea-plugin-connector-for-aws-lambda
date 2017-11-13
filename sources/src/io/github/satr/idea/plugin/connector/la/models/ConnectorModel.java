package io.github.satr.idea.plugin.connector.la.models;
// Copyright © 2017, github.com/satr, MIT License

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfilesConfigFile;
import com.amazonaws.auth.profile.internal.BasicProfile;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.*;
import io.github.satr.common.OperationResult;
import io.github.satr.common.OperationValueResult;
import io.github.satr.common.OperationValueResultImpl;
import io.github.satr.idea.plugin.connector.la.entities.CredentialProfileEntry;
import io.github.satr.idea.plugin.connector.la.entities.FunctionEntry;
import io.github.satr.idea.plugin.connector.la.entities.RegionEntry;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ConnectorModel {
    private AWSLambda awsLambdaClient;
    private static final Map<String, String> regionDescriptions;

    static {
        regionDescriptions = new LinkedHashMap<>();
        regionDescriptions.put("us-east-2", "US East (Ohio)");
        regionDescriptions.put("us-east-1", "US East (N. Virginia)");
        regionDescriptions.put("us-west-1", "US West (N. California)");
        regionDescriptions.put("us-west-2", "US West (Oregon)");
        regionDescriptions.put("ap-northeast-2", "Asia Pacific (Seoul)");
        regionDescriptions.put("ap-south-1", "Asia Pacific (Mumbai)");
        regionDescriptions.put("ap-southeast-1", "Asia Pacific (Singapore)");
        regionDescriptions.put("ap-southeast-2", "Asia Pacific (Sydney)");
        regionDescriptions.put("ap-northeast-1", "Asia Pacific (Tokyo)");
        regionDescriptions.put("ca-central-1", "Canada (Central)");
        regionDescriptions.put("eu-central-1", "EU (Frankfurt)");
        regionDescriptions.put("eu-west-1", "EU (Ireland)");
        regionDescriptions.put("eu-west-2", "EU (London)");
        regionDescriptions.put("sa-east-1", "South America (São Paulo)");
    }

    private ArrayList<RegionEntry> regionEntries;

    public ConnectorModel(Regions region, String profileName) {

        ProfileCredentialsProvider profileCredentialsProvider = new ProfileCredentialsProvider(profileName);

        awsLambdaClient = AWSLambdaClientBuilder.standard()
                .withRegion(region)
                .withCredentials(profileCredentialsProvider)
                .build();
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

    public List<RegionEntry> getRegions() {
        if(regionEntries != null)
            return regionEntries;

        regionEntries = new ArrayList<>();
        for(Region region : RegionUtils.getRegions()) {
            String description = regionDescriptions.get(region.getName());
            if(description != null)
                regionEntries.add(new RegionEntry(region, description));
        }
        return regionEntries;
    }

    public List<CredentialProfileEntry> getCredentialProfiles() {
        Map<String, BasicProfile> profiles = new ProfilesConfigFile().getAllBasicProfiles();
        List<CredentialProfileEntry> credentialProfilesEntries = new ArrayList<>();
        for (String profileName : profiles.keySet()) {
            credentialProfilesEntries.add(new CredentialProfileEntry(profileName, profiles.get(profileName)));
        }
        return credentialProfilesEntries;
    }
}
