package com.skillsense.survey.service;

import com.skillsense.survey.entity.Survey;
import com.skillsense.survey.model.SurveyStatus;
import com.skillsense.survey.repository.SurveyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SurveySchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SurveySchedulerService.class);

    private final SurveyRepository surveyRepository;

    public SurveySchedulerService(SurveyRepository surveyRepository) {
        this.surveyRepository = surveyRepository;
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void closeExpiredSurveys() {
        LocalDateTime now = LocalDateTime.now();
        List<Survey> expiredSurveys = surveyRepository.findByStatusAndClosesAtBefore(SurveyStatus.ACTIVE, now);

        if (!expiredSurveys.isEmpty()) {
            for (Survey survey : expiredSurveys) {
                survey.setStatus(SurveyStatus.CLOSED);
                surveyRepository.save(survey);
                log.info("Auto-closed expired survey: ID={}, Title='{}', ClosesAt={}",
                        survey.getSurveyId(), survey.getTitle(), survey.getClosesAt());
            }
        }
    }
}
