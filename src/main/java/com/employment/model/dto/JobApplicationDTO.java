package com.employment.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobApplicationDTO {

    @NotNull(message = "职位ID不能为空")
    private Long jobId;

    private Long resumeId;
    private String applyLetter;
}
