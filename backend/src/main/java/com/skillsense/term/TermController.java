package com.skillsense.term;

import com.skillsense.term.dto.TermRequest;
import com.skillsense.term.dto.TermResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/terms")
public class TermController {

    private final TermService termService;

    public TermController(TermService termService) {
        this.termService = termService;
    }

    @GetMapping
    public ResponseEntity<List<TermResponse>> getAllTerms() {
        return ResponseEntity.ok(termService.getAllTerms());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TermResponse> createTerm(@Valid @RequestBody TermRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(termService.createTerm(request));
    }

    @PutMapping("/{id}/current")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TermResponse> setCurrentTerm(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(termService.setCurrentTerm(id));
    }
}
