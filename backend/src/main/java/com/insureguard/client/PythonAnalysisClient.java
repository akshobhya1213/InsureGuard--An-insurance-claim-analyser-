package com.insureguard.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.insureguard.exception.AnalysisServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Map;

/**
 * Thin REST client for the Python analysis microservice.
 * Spring Boot is the caller; Python only performs the Watson NLU /
 * Roboflow-YOLO analysis and returns structured JSON.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PythonAnalysisClient {

    private final RestTemplate restTemplate;

    @Value("${app.python-service.base-url}")
    private String baseUrl;

    public JsonNode analyzeText(String reportText) {
        String url = baseUrl + "/analyze-text";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("text", reportText), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new AnalysisServiceException("Text analysis service returned an unexpected response");
            }
            return response.getBody();
        } catch (AnalysisServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Text analysis call failed: {}", ex.getMessage());
            throw new AnalysisServiceException("Failed to reach text analysis service: " + ex.getMessage(), ex);
        }
    }

    public JsonNode analyzeImage(File imageFile) {
        String url = baseUrl + "/analyze-image";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new FileSystemResource(imageFile));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new AnalysisServiceException("Image analysis service returned an unexpected response");
            }
            return response.getBody();
        } catch (AnalysisServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Image analysis call failed: {}", ex.getMessage());
            throw new AnalysisServiceException("Failed to reach image analysis service: " + ex.getMessage(), ex);
        }
    }
}
