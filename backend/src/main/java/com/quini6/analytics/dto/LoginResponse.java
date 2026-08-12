package com.quini6.analytics.dto;

public record LoginResponse(
    String token,
    String username,
    String role,
    long expiresIn
) {}
