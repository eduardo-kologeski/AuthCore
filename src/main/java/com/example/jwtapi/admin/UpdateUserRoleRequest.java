package com.example.jwtapi.admin;

import com.example.jwtapi.user.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull Role role
) {
}
