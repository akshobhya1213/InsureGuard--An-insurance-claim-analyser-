package com.insureguard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insureguard.client.PythonAnalysisClient;
import com.insureguard.dto.AnalysisResultResponse;
import com.insureguard.dto.DamageDetectionResponse;
import com.insureguard.dto.TextAnalysisResponse;
import com.insureguard.entity.*;
import com.insureguard.exception.ResourceNotFoundException;
import com.insureguard.redis.ReportCacheService;
import com.insureguard.repository.AnalysisResultRepository;
import com.insureguard.repository.DamageDetectionRepository;
import com.insureguard.repository.ReportRepository;
import com.insureguard.repository.TextAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Orchestrates the end-to-end analysis flow for a report:
 * calls the Python service for Watson text analysis and Roboflow/YOLO
 * image analysis, persists results, and computes the combined verdict.
 *
 * The fraud/suspicion SCORE itself is computed by the Python service's
 * fraud_analyzer (kept there so text + score travel together); this class
 * is responsible for orchestration, persistence and caching only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

    private final PythonAnalysisClient pythonAnalysisClient;
    private final ReportRepository reportRepository;
    private final TextAnalysisRepository textAnalysisRepository;
    private final DamageDetectionRepository damageDetectionRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final ReportCacheService reportCacheService;
    private final ObjectMapper objectMapper;

    /** Triggered synchronously via POST /api/reports/{id}/analyze. */
    @Transactional
    public AnalysisResultResponse analyzeReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));

        report.setStatus(ReportStatus.PROCESSING);
        reportRepository.save(report);

        try {
            TextAnalysis textAnalysis = runTextAnalysis(report);
            List<DamageDetection> detections = report.getImagePath() != null
                    ? runImageAnalysis(report)
                    : List.of();

            int fraudScore = textAnalysis.getSuspicionScore() != null ? textAnalysis.getSuspicionScore() : 0;
            String overallStatus = textAnalysis.getStatus();

            AnalysisResult result = analysisResultRepository.findByReportId(reportId)
                    .orElse(AnalysisResult.builder().report(report).build());
            result.setFraudScore(fraudScore);
            result.setOverallStatus(overallStatus);
            analysisResultRepository.save(result);

            report.setStatus(ReportStatus.COMPLETED);
            reportRepository.save(report);

            AnalysisResultResponse response = buildResponse(report, textAnalysis, detections, result);
            reportCacheService.put(reportId, response);
            return response;

        } catch (Exception ex) {
            report.setStatus(ReportStatus.FAILED);
            reportRepository.save(report);
            log.error("Analysis failed for report {}: {}", reportId, ex.getMessage());
            throw ex;
        }
    }

    /** Triggered asynchronously by the Kafka consumer after REPORT_SUBMITTED. */
    public void processReportAsync(Long reportId) {
        analyzeReport(reportId);
    }

    public AnalysisResultResponse getResult(Long reportId) {
        AnalysisResultResponse cached = reportCacheService.get(reportId);
        if (cached != null) {
            return cached;
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));
        AnalysisResult result = analysisResultRepository.findByReportId(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("No analysis result yet for report: " + reportId));
        TextAnalysis textAnalysis = textAnalysisRepository.findByReportId(reportId).orElse(null);
        List<DamageDetection> detections = damageDetectionRepository.findByReportId(reportId);

        AnalysisResultResponse response = buildResponse(report, textAnalysis, detections, result);
        reportCacheService.put(reportId, response);
        return response;
    }

    private TextAnalysis runTextAnalysis(Report report) {
        JsonNode result = pythonAnalysisClient.analyzeText(report.getDescription());

        TextAnalysis textAnalysis = textAnalysisRepository.findByReportId(report.getId())
                .orElse(TextAnalysis.builder().report(report).build());

        textAnalysis.setKeywords(safeWriteJson(result.path("watson").path("keywords")));
        textAnalysis.setEntities(safeWriteJson(result.path("watson").path("entities")));
        textAnalysis.setConcepts(safeWriteJson(result.path("watson").path("concepts")));
        textAnalysis.setSentiment(safeWriteJson(result.path("watson").path("sentiment")));
        textAnalysis.setSuspicionScore(result.path("fraud_analysis").path("score").asInt(0));
        textAnalysis.setSuspicionIndicators(safeWriteJson(result.path("fraud_analysis").path("indicators")));
        textAnalysis.setStatus(result.path("fraud_analysis").path("status").asText("LOW RISK"));

        return textAnalysisRepository.save(textAnalysis);
    }

    private List<DamageDetection> runImageAnalysis(Report report) {
        File imageFile = new File(report.getImagePath());
        JsonNode result = pythonAnalysisClient.analyzeImage(imageFile);

        List<DamageDetection> saved = new ArrayList<>();
        String annotatedPath = result.path("annotated_image_path").asText(null);

        for (JsonNode detection : result.path("detections")) {
            DamageDetection dd = DamageDetection.builder()
                    .report(report)
                    .damageType(detection.path("class").asText())
                    .confidence(detection.path("confidence").asDouble())
                    .bboxX(detection.path("x").asInt())
                    .bboxY(detection.path("y").asInt())
                    .bboxWidth(detection.path("width").asInt())
                    .bboxHeight(detection.path("height").asInt())
                    .annotatedImagePath(annotatedPath)
                    .build();
            saved.add(damageDetectionRepository.save(dd));
        }
        return saved;
    }

    private AnalysisResultResponse buildResponse(Report report, TextAnalysis textAnalysis,
                                                  List<DamageDetection> detections, AnalysisResult result) {
        TextAnalysisResponse textResponse = null;
        if (textAnalysis != null) {
            textResponse = TextAnalysisResponse.builder()
                    .keywords(readStringList(textAnalysis.getKeywords()))
                    .entities(readStringList(textAnalysis.getEntities()))
                    .concepts(readStringList(textAnalysis.getConcepts()))
                    .sentiment(readTree(textAnalysis.getSentiment()))
                    .suspicionScore(textAnalysis.getSuspicionScore())
                    .status(textAnalysis.getStatus())
                    .indicators(readStringList(textAnalysis.getSuspicionIndicators()))
                    .build();
        }

        List<DamageDetectionResponse> damageResponses = detections.stream()
                .map(d -> DamageDetectionResponse.builder()
                        .damageType(d.getDamageType())
                        .confidence(d.getConfidence())
                        .x(d.getBboxX())
                        .y(d.getBboxY())
                        .width(d.getBboxWidth())
                        .height(d.getBboxHeight())
                        .build())
                .toList();

        String annotatedImagePath = detections.isEmpty() ? null : detections.get(0).getAnnotatedImagePath();

        return AnalysisResultResponse.builder()
                .reportId(report.getId())
                .overallStatus(result.getOverallStatus())
                .fraudScore(result.getFraudScore())
                .textAnalysis(textResponse)
                .damageDetections(damageResponses)
                .annotatedImagePath(annotatedImagePath)
                .build();
    }

    private String safeWriteJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            List<String> values = new ArrayList<>();
            StreamSupport.stream(node.spliterator(), false).forEach(n -> {
                if (n.has("text")) {
                    values.add(n.path("text").asText());
                } else if (n.isTextual()) {
                    values.add(n.asText());
                } else {
                    values.add(n.toString());
                }
            });
            return values;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Object readTree(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return null;
        }
    }
}
