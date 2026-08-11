package com.employment.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class BatchGenerateClassRequest {

    @NotBlank(message = "年级不能为空")
    private String grade;

    @NotNull(message = "每个专业的班级数量不能为空")
    @Min(value = 1, message = "每个专业至少生成1个班级")
    private Integer classCountPerMajor;

    private Map<Long, Integer> customClassCounts;
}
