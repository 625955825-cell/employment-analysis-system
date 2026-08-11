package com.employment.service;

import com.employment.model.dto.ResumeDTO;

public interface ResumeExportService {
    byte[] exportToPdf(Long resumeId, Long userId);
}
