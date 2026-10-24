package com.skillsense.analytics.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsense.analytics.dto.InsightResponse;
import com.skillsense.analytics.dto.StudentResultResponse;
import com.skillsense.analytics.dto.fastapi.FastApiAnalyzeRequest;
import com.skillsense.analytics.dto.fastapi.FastApiInsightsRequest;
import com.skillsense.analytics.dto.fastapi.FastApiStudentResultRequest;
import com.skillsense.analytics.dto.fastapi.FastApiTrendRequest;
import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component
public class DefaultAnalyticsClient implements AnalyticsClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultAnalyticsClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public DefaultAnalyticsClient(
            @Value("${app.analytics.url:http://localhost:8001}") String analyticsUrl,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(analyticsUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
    }

    public DefaultAnalyticsClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode analyze(FastApiAnalyzeRequest request) {
        try {
            return restClient.post()
                    .uri("/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.error("Analytics service call failed on /analyze: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.ANALYTICS_UNAVAILABLE,
                    "Analytics service is currently unavailable");
        }
    }

    @Override
    public JsonNode trend(FastApiTrendRequest request) {
        try {
            return restClient.post()
                    .uri("/trend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.error("Analytics service call failed on /trend: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.ANALYTICS_UNAVAILABLE,
                    "Analytics service is currently unavailable");
        }
    }

    @Override
    public InsightResponse insights(FastApiInsightsRequest request) {
        try {
            return restClient.post()
                    .uri("/insights")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(InsightResponse.class);
        } catch (RestClientException e) {
            log.error("Analytics service call failed on /insights: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.ANALYTICS_UNAVAILABLE,
                    "Analytics service is currently unavailable");
        }
    }

    @Override
    public StudentResultResponse studentResult(FastApiStudentResultRequest request) {
        try {
            return restClient.post()
                    .uri("/student-result")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(StudentResultResponse.class);
        } catch (RestClientException e) {
            log.error("Analytics service call failed on /student-result: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.ANALYTICS_UNAVAILABLE,
                    "Analytics service is currently unavailable");
        }
    }
}
