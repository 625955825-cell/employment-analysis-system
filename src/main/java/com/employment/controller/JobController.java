package com.employment.controller;

import com.employment.common.PageResult;
import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.dto.JobApplyDTO;
import com.employment.model.dto.JobSearchDTO;
import com.employment.model.vo.JobDetailVO;
import com.employment.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping("/list")
    public Result<PageResult<?>> list(JobSearchDTO dto) {
        return Result.success(jobService.searchJobs(dto));
    }

    @GetMapping("/{id}")
    public Result<JobDetailVO> detail(@PathVariable Long id) {
        return Result.success(jobService.getJobDetail(id));
    }

    @PostMapping("/apply/{jobId}")
    @OperationLog(module = "职位管理", content = "投递简历")
    public Result<Void> apply(@PathVariable Long jobId, @RequestBody JobApplyDTO dto) {
        jobService.applyJob(jobId, dto.getResumeId(), dto.getApplyLetter());
        return Result.success();
    }

    @PostMapping("/favorite/{jobId}")
    @OperationLog(module = "职位管理", content = "收藏职位")
    public Result<Void> favorite(@PathVariable Long jobId) {
        jobService.favoriteJob(jobId);
        return Result.success();
    }

    @DeleteMapping("/favorite/{jobId}")
    public Result<Void> unfavorite(@PathVariable Long jobId) {
        jobService.unfavoriteJob(jobId);
        return Result.success();
    }

    @GetMapping("/my-applications")
    public Result<PageResult<?>> myApplications(JobSearchDTO dto) {
        return Result.success(jobService.getMyApplications(dto));
    }

    @GetMapping("/favorites")
    public Result<PageResult<?>> myFavorites(JobSearchDTO dto) {
        return Result.success(jobService.getMyFavorites(dto));
    }

    @DeleteMapping("/application/{id}")
    @OperationLog(module = "职位管理", content = "撤回投递")
    public Result<Void> cancelApplication(@PathVariable Long id) {
        jobService.cancelApplication(id);
        return Result.success();
    }
}
