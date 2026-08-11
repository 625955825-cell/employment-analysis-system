package com.employment.service;

import com.employment.model.dto.ResumeDTO;

import java.util.List;

public interface ResumeService {

    List<ResumeDTO> getMyResumes();

    ResumeDTO getResumeById(Long id);

    ResumeDTO createResume(ResumeDTO dto);

    ResumeDTO updateResume(Long id, ResumeDTO dto);

    void deleteResume(Long id);

    void setDefaultResume(Long id);
}
