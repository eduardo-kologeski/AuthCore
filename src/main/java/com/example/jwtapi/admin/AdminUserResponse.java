package com.example.jwtapi.admin;

import com.example.jwtapi.user.Role;
import com.example.jwtapi.user.User;

public record AdminUserResponse(Long id, String name, String email, Role role) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
