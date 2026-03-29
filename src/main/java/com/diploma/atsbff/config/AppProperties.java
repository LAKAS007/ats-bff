package com.diploma.atsbff.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @Valid
    private final Jwt jwt = new Jwt();

    @Valid
    private final DemoUser demoUser = new DemoUser();

    @Valid
    private final Upstream python = new Upstream();

    @Valid
    private final Upstream bybit = new Upstream();

    @Valid
    private final Ollama ollama = new Ollama();

    public Jwt getJwt() {
        return jwt;
    }

    public DemoUser getDemoUser() {
        return demoUser;
    }

    public Upstream getPython() {
        return python;
    }

    public Upstream getBybit() {
        return bybit;
    }

    public Ollama getOllama() {
        return ollama;
    }

    public static class Jwt {
        @NotBlank
        private String secret;

        @Min(60)
        private long expirationSeconds = 43200;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationSeconds() {
            return expirationSeconds;
        }

        public void setExpirationSeconds(long expirationSeconds) {
            this.expirationSeconds = expirationSeconds;
        }
    }

    public static class DemoUser {
        @NotBlank
        private String username;

        @NotBlank
        private String password;

        @NotBlank
        private String displayName;

        @NotBlank
        private String role = "USER";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class Upstream {
        @NotBlank
        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Ollama extends Upstream {
        @NotBlank
        private String model = "llama3.1:8b";

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}
