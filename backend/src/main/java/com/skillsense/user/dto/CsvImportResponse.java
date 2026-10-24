package com.skillsense.user.dto;

import java.util.ArrayList;
import java.util.List;

public class CsvImportResponse {

    private int created;
    private int skipped;
    private List<RowError> errors = new ArrayList<>();
    private String credentials;

    public CsvImportResponse() {
    }

    public CsvImportResponse(int created, int skipped, List<RowError> errors, String credentials) {
        this.created = created;
        this.skipped = skipped;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.credentials = credentials;
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public List<RowError> getErrors() {
        return errors;
    }

    public void setErrors(List<RowError> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    public static class RowError {
        private int row;
        private String message;

        public RowError() {
        }

        public RowError(int row, String message) {
            this.row = row;
            this.message = message;
        }

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int created;
        private int skipped;
        private List<RowError> errors = new ArrayList<>();
        private String credentials;

        public Builder created(int created) {
            this.created = created;
            return this;
        }

        public Builder skipped(int skipped) {
            this.skipped = skipped;
            return this;
        }

        public Builder errors(List<RowError> errors) {
            this.errors = errors;
            return this;
        }

        public Builder credentials(String credentials) {
            this.credentials = credentials;
            return this;
        }

        public CsvImportResponse build() {
            return new CsvImportResponse(created, skipped, errors, credentials);
        }
    }
}
