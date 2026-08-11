package com.employment.service;

import com.employment.model.entity.SysClass;
import com.employment.common.PageResult;
import com.employment.model.dto.BatchGenerateClassRequest;

import java.util.List;
import java.util.Map;

public interface SysClassService {

    PageResult<SysClass> list(String keyword, Long deptId, Long majorId, Integer pageNum, Integer pageSize);

    SysClass getById(Long id);

    SysClass save(SysClass sysClass);

    SysClass update(Long id, SysClass sysClass);

    void delete(Long id);

    List<Map<String, Object>> getByMajorId(Long majorId);

    List<Map<String, Object>> getAllClasses();

    List<SysClass> batchGenerateClasses(BatchGenerateClassRequest request);

    void batchDeleteByGrade(String grade);
}
