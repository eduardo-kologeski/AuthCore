package com.example.jwtapi.admin;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/bootstrap")
public class AdminBootstrapController {

    private final AdminBootstrapService adminBootstrapService;

    public AdminBootstrapController(AdminBootstrapService adminBootstrapService) {
        this.adminBootstrapService = adminBootstrapService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse bootstrap(@Valid @RequestBody BootstrapAdminRequest request) {
        return adminBootstrapService.bootstrap(request);
    }
}
