package com.diploma.atsbff.auth;

public record UserProfileResponse(
    String username,
    String displayName,
    String role
) {
}
