package com.skillsense.survey;

import com.fasterxml.jackson.databind.JsonNode;
import com.skillsense.survey.service.PresetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/survey-presets")
@PreAuthorize("hasAnyRole('FACULTY', 'HOD', 'PLACEMENT_OFFICER', 'ADMIN')")
public class PresetController {

    private final PresetService presetService;

    public PresetController(PresetService presetService) {
        this.presetService = presetService;
    }

    @GetMapping("/{category}")
    public ResponseEntity<JsonNode> getPreset(@PathVariable("category") String category) {
        return ResponseEntity.ok(presetService.getPreset(category));
    }
}
