package com.employment.service;

import com.employment.model.dto.StudentInfoDTO;
import com.employment.model.entity.StudentInfo;
import com.employment.model.vo.StudentHomeStatsVO;

public interface StudentService {

    StudentInfoDTO getProfile();

    StudentInfoDTO updateProfile(StudentInfoDTO dto);

    StudentHomeStatsVO getHomeStats();
}
