package com.employment.controller;

import com.employment.common.PageResult;
import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.dto.JobSearchDTO;
import com.employment.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping("/invitations")
    public Result<PageResult<?>> invitations(JobSearchDTO dto) {
        return Result.success(interviewService.getMyInterviews(dto));
    }

    @PutMapping("/invitation/{id}/accept")
    @OperationLog(module = "面试管理", content = "接受面试邀请")
    public Result<Void> accept(@PathVariable Long id) {
        interviewService.acceptInterview(id);
        return Result.success();
    }

    @PutMapping("/invitation/{id}/reject")
    @OperationLog(module = "面试管理", content = "拒绝面试邀请")
    public Result<Void> reject(@PathVariable Long id) {
        interviewService.rejectInterview(id);
        return Result.success();
    }

    @GetMapping("/offers")
    public Result<PageResult<?>> offers(JobSearchDTO dto) {
        return Result.success(interviewService.getMyOffers(dto));
    }

    @PutMapping("/offer/{id}/accept")
    @OperationLog(module = "Offer管理", content = "接受Offer")
    public Result<Void> acceptOffer(@PathVariable Long id) {
        interviewService.acceptOffer(id);
        return Result.success();
    }

    @PutMapping("/offer/{id}/reject")
    @OperationLog(module = "Offer管理", content = "拒绝Offer")
    public Result<Void> rejectOffer(@PathVariable Long id) {
        interviewService.rejectOffer(id);
        return Result.success();
    }
}
