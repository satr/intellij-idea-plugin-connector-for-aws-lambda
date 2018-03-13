package io.github.satr.idea.plugin.connector.la.models;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfilesConfigFile;
import com.amazonaws.profile.path.AwsProfileFileLocationProvider;
import com.amazonaws.regions.Regions;
import com.intellij.util.net.HttpConfigurable;

import static org.apache.http.util.TextUtils.isEmpty;

public abstract class AbstractConnectorModel {
    protected final Regions region;
    protected String proxyDetails = "Unknown";
    protected String credentialProfileName;

    public AbstractConnectorModel(Regions region, String credentialProfileName) {
        this.region = region;
        this.credentialProfileName = credentialProfileName;
    }

    protected ClientConfiguration getClientConfiguration() {
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
    }

    protected AWSCredentialsProvider getCredentialsProvider(String credentialProfileName) {
        return validateCredentialProfile(credentialProfileName)
                ? new ProfileCredentialsProvider(credentialProfileName)
                : DefaultAWSCredentialsProviderChain.getInstance();
    }

    private boolean validateCredentialProfile(String credentialProfileName) {
        return !isEmpty(credentialProfileName)
                && validateCredentialProfilesExist()
                && new ProfilesConfigFile().getAllBasicProfiles().containsKey(credentialProfileName);
    }

    protected boolean validateCredentialProfilesExist() {
        return AwsProfileFileLocationProvider.DEFAULT_CREDENTIALS_LOCATION_PROVIDER.getLocation() != null;
    }

    public Regions getRegion() {
        return region;
    }

    public String getCredentialProfileName() {
        return credentialProfileName;
    }

    public abstract void shutdown();
}
