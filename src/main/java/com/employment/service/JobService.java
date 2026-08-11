package com.employment.service;

import com.employment.common.PageResult;
import com.employment.model.dto.JobSearchDTO;
import com.employment.model.vo.JobDetailVO;

public interface JobService {

    PageResult<?> searchJobs(JobSearchDTO dto);

    JobDetailVO getJobDetail(Long id);

    void applyJob(Long jobId, Long resumeId, String applyLetter);

    void favoriteJob(Long jobId);

    void unfavoriteJob(Long jobId);

    PageResult<?> getMyApplications(JobSearchDTO dto);

    PageResult<?> getMyFavorites(JobSearchDTO dto);

    void cancelApplication(Long applicationId);
}
