package com.skillsense.survey.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.survey.model.SurveyCategory;
import com.skillsense.survey.model.schema.SurveyQuestionSchema;
import com.skillsense.survey.validation.QuestionSchemaValidator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class PresetService {

    private final ObjectMapper objectMapper;
    private final QuestionSchemaValidator schemaValidator;

    public PresetService(ObjectMapper objectMapper, QuestionSchemaValidator schemaValidator) {
        this.objectMapper = objectMapper;
        this.schemaValidator = schemaValidator;
    }

    public JsonNode getPreset(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Category is required");
        }

        SurveyCategory category;
        try {
            category = SurveyCategory.valueOf(categoryName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Unknown preset category: " + categoryName);
        }

        String path = "presets/" + category.name() + ".json";
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Preset not found for category: " + category.name());
        }

        try (InputStream is = resource.getInputStream()) {
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            // Verify schema is valid
            SurveyQuestionSchema schema = objectMapper.readValue(json, SurveyQuestionSchema.class);
            schemaValidator.validate(schema);
            return objectMapper.readTree(json);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.VALIDATION_FAILED, "Failed to load preset for " + category.name() + ": " + e.getMessage());
        }
    }
}
