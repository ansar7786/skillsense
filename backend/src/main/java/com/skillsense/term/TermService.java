package com.skillsense.term;

import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.term.dto.TermRequest;
import com.skillsense.term.dto.TermResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TermService {

    private final AcademicTermRepository termRepository;

    public TermService(AcademicTermRepository termRepository) {
        this.termRepository = termRepository;
    }

    @Transactional(readOnly = true)
    public List<TermResponse> getAllTerms() {
        return termRepository.findAll().stream()
                .map(TermResponse::fromEntity)
                .toList();
    }

    @Transactional
    public TermResponse createTerm(TermRequest request) {
        if (termRepository.existsByLabel(request.getLabel())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Term with label already exists: " + request.getLabel());
        }

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Start date must be before or equal to end date");
        }

        AcademicTerm term = AcademicTerm.builder()
                .label(request.getLabel().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isCurrent(Boolean.TRUE.equals(request.getIsCurrent()))
                .build();

        AcademicTerm saved = termRepository.save(term);

        if (Boolean.TRUE.equals(saved.getIsCurrent())) {
            termRepository.resetOtherCurrentTerms(saved.getTermId());
        }

        return TermResponse.fromEntity(saved);
    }

    @Transactional
    public TermResponse setCurrentTerm(Integer termId) {
        AcademicTerm term = termRepository.findById(termId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Term not found: " + termId));

        termRepository.resetAllCurrentTerms();
        term.setIsCurrent(true);
        AcademicTerm updated = termRepository.save(term);

        return TermResponse.fromEntity(updated);
    }
}
