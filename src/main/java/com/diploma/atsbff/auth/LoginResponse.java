package com.diploma.atsbff.auth;

public record LoginResponse(
    String token,
    UserProfileResponse user
) {
}
