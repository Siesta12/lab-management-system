package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.experiment.ExperimentReportReviewRequest;
import com.nlt.domain.dto.experiment.ExperimentReportSaveRequest;
import com.nlt.domain.entity.ExperimentReportEntity;

public interface ExperimentReportService {

    PageData<ExperimentReportEntity> page(int pageNum, int pageSize, Integer status, String keyword);

    ExperimentReportEntity getById(Long id);

    ExperimentReportEntity createDraft(ExperimentReportSaveRequest request);

    ExperimentReportEntity updateDraft(Long id, ExperimentReportSaveRequest request);

    ExperimentReportEntity submit(Long id);

    ExperimentReportEntity review(Long id, ExperimentReportReviewRequest request);

    byte[] exportWord(Long id);
}
