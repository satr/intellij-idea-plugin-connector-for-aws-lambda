package io.github.satr.idea.plugin.connector.la.models;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.Role;
import io.github.satr.common.Logger;
import io.github.satr.common.OperationResult;
import io.github.satr.common.OperationResultImpl;
import io.github.satr.idea.plugin.connector.la.entities.RoleEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RoleConnectorModel extends AbstractConnectorModel {
    private static final int MAX_FETCHED_ROLE_COUNT = 50;
    private AmazonIdentityManagement identityManagementClient;
    private List<RoleEntity> roleEntities =  new ArrayList<>();
    private Map<String, RoleEntity> roleEntityMap = new LinkedHashMap<>();
    private boolean loaded;

    public RoleConnectorModel(String regionName, String credentialProfileName, Logger logger) {
        super(regionName, credentialProfileName, logger);
    }

    public OperationResult loadRoles() {
        OperationResultImpl result = new OperationResultImpl();
        try {
            identityManagementClient = AmazonIdentityManagementClientBuilder.standard()
                    .withRegion(getRegionName())
                    .withCredentials(getCredentialsProvider())
                    .withClientConfiguration(getClientConfiguration())
                    .build();
            populateRoleListAndMap(identityManagementClient, result);
            loaded = true;
        } catch (Exception e) {
            reportErrorLoadingOfRolesFailed(result, e);
        }
        return result;
    }

    @Override
    protected void finalize() throws Throwable {
        shutdown();
        super.finalize();
    }

    @Override
    public void shutdown() {
        if (identityManagementClient == null) {
            return;
        }
        identityManagementClient.shutdown();
    }

    public List<RoleEntity> getRoles() {
        return roleEntities;
    }

    private void populateRoleListAndMap(AmazonIdentityManagement identityManagementClient, OperationResult result) {
        roleEntities = new ArrayList<>();
        roleEntityMap = new LinkedHashMap<>();
        try {
            ListRolesRequest listRolesRequest = new ListRolesRequest().withMaxItems(MAX_FETCHED_ROLE_COUNT);
            ListRolesResult listRolesResult = identityManagementClient.listRoles(listRolesRequest);
            List<Role> roles = listRolesResult.getRoles();
            for (Role role : roles) {
                addRoleToListAndMap(role);
            }
            result.addDebug("Loaded %d roles", roles.size());
        } catch (AmazonIdentityManagementException e) {
            if ("AccessDenied".equals(e.getErrorCode())) {
                result.addDebug("User has not access to a list of roles");//skip the error
            } else {
                reportErrorLoadingOfRolesFailed(result, e);
            }
        } catch (Exception e) {
            reportErrorLoadingOfRolesFailed(result, e);
        }
    }

    private void reportErrorLoadingOfRolesFailed(OperationResult result, Exception e) {
        e.printStackTrace();
        result.addError("Loading of roles failed: %s", e.getMessage());
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

    public RoleEntity addRole(String roleArn) {
        return addRoleToListAndMap(RoleEntity.create(roleArn));
    }

    public boolean isLoaded() {
        return loaded;
    }
}
