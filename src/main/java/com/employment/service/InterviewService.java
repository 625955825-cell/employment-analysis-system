package com.employment.service;

import com.employment.common.PageResult;
import com.employment.model.dto.JobSearchDTO;
import com.employment.model.entity.OfferLetter;

import java.util.List;

public interface InterviewService {
    PageResult<?> getMyInterviews(JobSearchDTO dto);
    void acceptInterview(Long id);
    void rejectInterview(Long id);

    // Offer相关
    PageResult<?> getMyOffers(JobSearchDTO dto);
    void acceptOffer(Long id);
    void rejectOffer(Long id);
}
