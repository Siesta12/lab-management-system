package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.domain.dto.experiment.ExperimentReportReviewRequest;
import com.nlt.domain.dto.experiment.ExperimentReportSaveRequest;
import com.nlt.domain.entity.ExperimentReportEntity;
import com.nlt.service.ExperimentReportService;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/experiment-reports")
@RequiredArgsConstructor
public class ExperimentReportController {

    private final ExperimentReportService experimentReportService;

    @GetMapping
    public ApiResponse<PageData<ExperimentReportEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) String keyword) {
        return ApiResponse.success(experimentReportService.page(pageNum, pageSize, status, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExperimentReportEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(experimentReportService.getById(id));
    }

    @PostMapping("/draft")
    public ApiResponse<ExperimentReportEntity> createDraft(@Valid @RequestBody ExperimentReportSaveRequest request) {
        return ApiResponse.success(experimentReportService.createDraft(request));
    }

    @PutMapping("/{id}/draft")
    public ApiResponse<ExperimentReportEntity> updateDraft(@PathVariable Long id,
        @Valid @RequestBody ExperimentReportSaveRequest request) {
        return ApiResponse.success(experimentReportService.updateDraft(id, request));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<ExperimentReportEntity> submit(@PathVariable Long id) {
        return ApiResponse.success(experimentReportService.submit(id));
    }

    @PostMapping("/{id}/review")
    public ApiResponse<ExperimentReportEntity> review(@PathVariable Long id,
        @Valid @RequestBody ExperimentReportReviewRequest request) {
        return ApiResponse.success(experimentReportService.review(id, request));
    }

    @GetMapping("/{id}/word")
    public ResponseEntity<byte[]> downloadWord(@PathVariable Long id) {
        ExperimentReportEntity report = experimentReportService.getById(id);
        byte[] bytes = experimentReportService.exportWord(id);
        String fileName = "实验报告-" + safeName(report.getStudentName()) + "-" + safeName(report.getExperimentName()) + ".docx";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .body(bytes);
    }

    private String safeName(String value) {
        if (value == null || value.isBlank()) {
            return "未命名";
        }
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
