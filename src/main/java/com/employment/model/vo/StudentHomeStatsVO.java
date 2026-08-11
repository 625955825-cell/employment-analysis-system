package com.employment.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentHomeStatsVO {

    private Long applicationCount;
    private Long interviewCount;
    private Long favoriteCount;
    private String employmentStatus;
    private String employmentStatusText;
}
