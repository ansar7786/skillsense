package com.skillsense.analytics.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.skillsense.analytics.dto.InsightResponse;
import com.skillsense.analytics.dto.StudentResultResponse;
import com.skillsense.analytics.dto.fastapi.FastApiAnalyzeRequest;
import com.skillsense.analytics.dto.fastapi.FastApiInsightsRequest;
import com.skillsense.analytics.dto.fastapi.FastApiStudentResultRequest;
import com.skillsense.analytics.dto.fastapi.FastApiTrendRequest;

public interface AnalyticsClient {

    JsonNode analyze(FastApiAnalyzeRequest request);

    JsonNode trend(FastApiTrendRequest request);

    InsightResponse insights(FastApiInsightsRequest request);

    StudentResultResponse studentResult(FastApiStudentResultRequest request);
}
