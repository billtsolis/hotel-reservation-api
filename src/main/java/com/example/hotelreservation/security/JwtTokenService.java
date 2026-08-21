package com.example.hotelreservation.security;

import com.example.hotelreservation.dto.auth.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties securityProperties;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            SecurityProperties securityProperties
    ) {
        this.jwtEncoder = jwtEncoder;
        this.securityProperties = securityProperties;
    }

    public LoginResponse generateToken(
            Authentication authentication
    ) {
        Instant issuedAt = Instant.now();
        Instant expiresAt =
                issuedAt.plusSeconds(securityProperties.jwtExpirationSeconds());

        List<String> roles = authentication
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("hotel-reservation-api")
                .subject(authentication.getName())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        return new LoginResponse(
                token,
                "Bearer",
                securityProperties.jwtExpirationSeconds()
        );
    }
}