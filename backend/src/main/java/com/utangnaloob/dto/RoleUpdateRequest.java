package com.utangnaloob.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public class RoleUpdateRequest {

    @NotEmpty(message = "At least one role must be provided")
    private Set<String> roles;

    public RoleUpdateRequest() {
    }

    public RoleUpdateRequest(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
