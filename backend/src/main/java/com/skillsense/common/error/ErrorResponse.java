package com.skillsense.common.error;

import java.util.ArrayList;
import java.util.List;

public class ErrorResponse {
    private int status;
    private String code;
    private String message;
    private List<String> details = new ArrayList<>();

    public ErrorResponse() {
    }

    public ErrorResponse(int status, String code, String message, List<String> details) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.details = details != null ? details : new ArrayList<>();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details != null ? details : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int status;
        private String code;
        private String message;
        private List<String> details = new ArrayList<>();

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder details(List<String> details) {
            this.details = details;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(status, code, message, details);
        }
    }
}
