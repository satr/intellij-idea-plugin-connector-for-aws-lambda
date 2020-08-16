package io.github.satr.idea.plugin.aws.lambda.connector.models;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.internal.AllProfiles;
import com.amazonaws.auth.profile.internal.BasicProfile;
import com.amazonaws.auth.profile.internal.BasicProfileConfigLoader;
import com.amazonaws.auth.profile.internal.ProfileStaticCredentialsProvider;
import com.amazonaws.profile.path.AwsProfileFileLocationProvider;
import com.intellij.util.net.HttpConfigurable;
import io.github.satr.common.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.http.util.TextUtils.isEmpty;

public abstract class AbstractConnectorModel {
    protected String regionName;
    protected String proxyDetails = "Unknown";
    protected String credentialProfileName;
    private Logger logger;

    public AbstractConnectorModel(String regionName, String credentialProfileName, Logger logger) {
        this.regionName = regionName;
        this.credentialProfileName = credentialProfileName;
        this.logger = logger;
    }

    protected Logger getLogger() {
        return logger;
    }

    protected ClientConfiguration getClientConfiguration() {
        try {
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
            if (!useProxyAuto) {
                proxyDetails = String.format("%s:%s", proxyHost, proxyPort);
            }

            if (httpConfigurable.PROXY_AUTHENTICATION) {
                String proxyLogin = httpConfigurable.getProxyLogin();
                clientConfiguration = clientConfiguration.withProxyPassword(httpConfigurable.getPlainProxyPassword())
                        .withProxyUsername(proxyLogin);
            }
            return clientConfiguration;
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().logError("Getting od the client configuration failed: %s", e.getMessage());
        }
        return null;
    }

    protected AWSCredentialsProvider getCredentialsProvider() {
        if (isEmpty(credentialProfileName)) {
            getLogger().logDebug("Cannot get a profile for an empty name");
            return tryDefaultAwsCredentialsProviderChain();
        }
        if (!validateCredentialProfilesExist()) {
            getLogger().logError("Cannot find any credentials profiles. Please create at least one.");
            return tryDefaultAwsCredentialsProviderChain();
        }

        ProfileStaticCredentialsProvider profileCredentialsProvider = null;
        AllProfiles allBasicProfiles = loadProfilesWithProperties();
        BasicProfile profile = allBasicProfiles.getProfile(credentialProfileName);
        if (profile == null) {
            getLogger().logDebug("Last loaded profile does not exist: \"%s\".", credentialProfileName);
        } else {
            getLogger().logDebug("Select the profile \"%s\".", credentialProfileName);
            profileCredentialsProvider = tryCreateProfileCredentialsProvider(profile);
            if (profileCredentialsProvider != null) {
                String profileRegionName = profile.getRegion();
                if(!isEmpty(profileRegionName)) {
                    getLogger().logDebug("Selected a region from the profile: %s", regionName);
                    regionName = profileRegionName;
                }
                return profileCredentialsProvider;
            }
        }
        profileCredentialsProvider = tryGetAlternativeAwsCredentialsProvider(credentialProfileName, allBasicProfiles);
        if (profileCredentialsProvider != null) {
            return profileCredentialsProvider;
        }
        getLogger().logDebug("No profiles could be selected and used.");
        return tryDefaultAwsCredentialsProviderChain();
    }

    @Nullable
    private ProfileStaticCredentialsProvider tryGetAlternativeAwsCredentialsProvider(String skipProfileName, AllProfiles allBasicProfiles) {
        ProfileStaticCredentialsProvider profileCredentialsProvider;
        for(BasicProfile alternativeProfile : allBasicProfiles.getProfiles().values()) {
            if(alternativeProfile.getProfileName().equals(skipProfileName)) {
                continue; //skip the profile, already checked before
            }
            getLogger().logDebug("Try to selected the profile \"%s\".", alternativeProfile.getProfileName());
            profileCredentialsProvider = tryCreateProfileCredentialsProvider(alternativeProfile);
            if (profileCredentialsProvider != null) {
                credentialProfileName = alternativeProfile.getProfileName();
                String profileRegionName = alternativeProfile.getRegion();
                if(!isEmpty(profileRegionName)) {
                    getLogger().logDebug("Selected a region from the profile: %s", regionName);
                    regionName = profileRegionName;
                }
                return profileCredentialsProvider;
            }
        }
        return null;
    }

    //Load profiles from the file ".aws/credentials" with regions and other properties (if exist) from the file ".aws/config"
    //Logic has been borrowed from https://github.com/aws/aws-sdk-java/issues/803#issuecomment-374043898
    //The issue with not loading regions from the file ".aws/config" supposed to be fixed in Java SDK 2.0, which is in "preview" now
    //Java SDK 2.0  https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/welcome.html
    private static AllProfiles loadProfilesWithProperties() {
        final AllProfiles allProfiles = new AllProfiles(Stream.concat(
                BasicProfileConfigLoader.INSTANCE.loadProfiles(
                        AwsProfileFileLocationProvider.DEFAULT_CONFIG_LOCATION_PROVIDER.getLocation()).getProfiles().values().stream(),
                BasicProfileConfigLoader.INSTANCE.loadProfiles(
                        AwsProfileFileLocationProvider.DEFAULT_CREDENTIALS_LOCATION_PROVIDER.getLocation()).getProfiles().values().stream())
                .map(profile -> new BasicProfile(profile.getProfileName().replaceFirst("^profile ", ""), profile.getProperties()))
                .collect(Collectors.toMap(profile -> profile.getProfileName(), profile -> profile,
                        (left, right) -> {
                            final Map<String, String> properties = new HashMap<>(left.getProperties());
                            properties.putAll(right.getProperties());
                            return new BasicProfile(left.getProfileName(), properties);
                        })));

        return allProfiles;
    }


    @Nullable
    private ProfileStaticCredentialsProvider tryCreateProfileCredentialsProvider(BasicProfile profile) {
        try {
            return new ProfileStaticCredentialsProvider(profile);
        } catch (SdkClientException e) {
            e.printStackTrace();
            getLogger().logError("Cannot load the profile \"%s\": %s", profile.getProfileName(), e.getMessage());
            return null;
        }
    }

    private AWSCredentialsProvider tryDefaultAwsCredentialsProviderChain() {
        getLogger().logDebug("Trying to use default AWS credentials provider chain:");
        DefaultAWSCredentialsProviderChain defaultAWSCredentialsProviderChain = DefaultAWSCredentialsProviderChain.getInstance();
        AWSCredentials credentials = defaultAWSCredentialsProviderChain.getCredentials();
        if (!isEmpty(credentials.getAWSSecretKey()) && !isEmpty(credentials.getAWSAccessKeyId())) {
            getLogger().logError("Found credentials for default AWS credentials provider chain.");
        } else {
            getLogger().logError("Invalid or not defined AWSSecretKey or AWSAccessKeyId for default AWS credentials provider chain.");
        }
        return defaultAWSCredentialsProviderChain;
    }

    protected boolean validateCredentialProfilesExist() {
        return AwsProfileFileLocationProvider.DEFAULT_CREDENTIALS_LOCATION_PROVIDER.getLocation() != null;
    }

    public String getCredentialProfileName() {
        return credentialProfileName;
    }

    public String getRegionName() {
        return this.regionName;
    }

    public abstract void shutdown();
}
