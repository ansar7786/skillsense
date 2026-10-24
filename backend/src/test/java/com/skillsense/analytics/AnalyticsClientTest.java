package com.skillsense.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsense.analytics.client.AnalyticsClient;
import com.skillsense.analytics.client.DefaultAnalyticsClient;
import com.skillsense.analytics.dto.fastapi.FastApiAnalyzeRequest;
import com.skillsense.analytics.dto.fastapi.FastApiSurveyMeta;
import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class AnalyticsClientTest {

    private AnalyticsClient analyticsClient;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8001");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        analyticsClient = new DefaultAnalyticsClient(restClient, objectMapper);
    }

    @Test
    public void testAnalyze_Success() {
        mockServer.expect(requestTo("http://localhost:8001/analyze"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"response_count\": 10, \"index\": 75.0}", MediaType.APPLICATION_JSON));

        FastApiAnalyzeRequest req = new FastApiAnalyzeRequest(
                new FastApiSurveyMeta(1L, "PLACEMENT", false, 10, null, Collections.emptyMap()),
                Collections.emptyList()
        );

        JsonNode result = analyticsClient.analyze(req);
        assertNotNull(result);
        assertEquals(10, result.get("response_count").asInt());
        assertEquals(75.0, result.get("index").asDouble());
        mockServer.verify();
    }

    @Test
    public void testAnalyze_ServerUnavailable_Throws503() {
        mockServer.expect(requestTo("http://localhost:8001/analyze"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        FastApiAnalyzeRequest req = new FastApiAnalyzeRequest(
                new FastApiSurveyMeta(1L, "PLACEMENT", false, 10, null, Collections.emptyMap()),
                Collections.emptyList()
        );

        ApiException ex = assertThrows(ApiException.class, () -> analyticsClient.analyze(req));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertEquals(ErrorCode.ANALYTICS_UNAVAILABLE, ex.getErrorCode());
        mockServer.verify();
    }
}
