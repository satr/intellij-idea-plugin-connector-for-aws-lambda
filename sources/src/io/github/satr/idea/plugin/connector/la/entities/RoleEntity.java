package io.github.satr.idea.plugin.connector.la.entities;

import com.amazonaws.services.identitymanagement.model.Role;

import java.util.Objects;

public class RoleEntity {
    private final Role role;
    private final String arn;
    private String name;

    public RoleEntity(Role role) {

        this.role = role;
        name = role.getRoleName();
        arn = role.getArn();
    }

    public String getName() {
        return name;
    }

    public String getArn() {
        return arn;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, arn);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RoleEntity that = (RoleEntity) o;
        return Objects.equals(arn, that.arn) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arn, name);
    }
}
