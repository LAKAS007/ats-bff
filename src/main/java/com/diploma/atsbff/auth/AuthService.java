package com.diploma.atsbff.auth;

import com.diploma.atsbff.common.ApiException;
import com.diploma.atsbff.config.AppProperties;
import com.diploma.atsbff.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String encodedPassword;

    public AuthService(
        AppProperties properties,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.encodedPassword = passwordEncoder.encode(properties.getDemoUser().getPassword());
    }

    public LoginResponse login(LoginRequest request) {
        if (
            !properties.getDemoUser().getUsername().equalsIgnoreCase(request.username()) ||
            !passwordEncoder.matches(request.password(), encodedPassword)
        ) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        UserProfileResponse user = demoUser();
        String token = jwtService.generateToken(user.username(), user.displayName(), user.role());
        return new LoginResponse(token, user);
    }

    public UserProfileResponse currentUser(String username) {
        if (!properties.getDemoUser().getUsername().equalsIgnoreCase(username)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unknown user");
        }
        return demoUser();
    }

    private UserProfileResponse demoUser() {
        return new UserProfileResponse(
            properties.getDemoUser().getUsername(),
            properties.getDemoUser().getDisplayName(),
            properties.getDemoUser().getRole()
        );
    }
}
