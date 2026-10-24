package com.skillsense.user.dto;

public class ResetPasswordResponse {
    private String temporaryPassword;

    public ResetPasswordResponse() {
    }

    public ResetPasswordResponse(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
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
        private String temporaryPassword;

        public Builder temporaryPassword(String temporaryPassword) {
            this.temporaryPassword = temporaryPassword;
            return this;
        }

        public ResetPasswordResponse build() {
            return new ResetPasswordResponse(temporaryPassword);
        }
    }
}
