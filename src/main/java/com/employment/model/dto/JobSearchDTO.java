package com.employment.model.dto;

import lombok.Data;

@Data
public class JobSearchDTO {
    private String keyword;
    private String city;
    private Integer page = 1;
    private Integer size = 10;
}
