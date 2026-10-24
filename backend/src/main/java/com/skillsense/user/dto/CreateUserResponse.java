package com.skillsense.user.dto;

public class CreateUserResponse {
    private UserResponse user;
    private String temporaryPassword;

    public CreateUserResponse() {
    }

    public CreateUserResponse(UserResponse user, String temporaryPassword) {
        this.user = user;
        this.temporaryPassword = temporaryPassword;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }

    public void setTemporaryPassword(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UserResponse user;
        private String temporaryPassword;

        public Builder user(UserResponse user) {
            this.user = user;
            return this;
        }

        public Builder temporaryPassword(String temporaryPassword) {
            this.temporaryPassword = temporaryPassword;
            return this;
        }

        public CreateUserResponse build() {
            return new CreateUserResponse(user, temporaryPassword);
        }
    }
}
