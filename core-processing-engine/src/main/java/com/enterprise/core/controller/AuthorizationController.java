package com.enterprise.core.controller;

import com.enterprise.core.model.AuthorizationRequest;
import com.enterprise.core.model.AuthorizationResponse;
import com.enterprise.core.service.AuthorizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/authorizations")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public ResponseEntity<AuthorizationResponse> authorize(@Valid @RequestBody AuthorizationRequest request) {
        AuthorizationResponse response = authorizationService.authorize(request);
        return ResponseEntity.ok(response);
    }
}
