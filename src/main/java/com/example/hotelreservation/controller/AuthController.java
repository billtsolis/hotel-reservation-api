package com.example.hotelreservation.controller;

import com.example.hotelreservation.dto.auth.LoginRequest;
import com.example.hotelreservation.dto.auth.LoginResponse;
import com.example.hotelreservation.security.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.username(),
                        request.password()
                );

        Authentication authenticatedUser =
                authenticationManager.authenticate(
                        authenticationRequest
                );

        LoginResponse response =
                jwtTokenService.generateToken(authenticatedUser);

        return ResponseEntity.ok(response);
    }
}