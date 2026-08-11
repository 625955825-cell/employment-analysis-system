package com.employment.service;

import java.util.List;
import java.util.Map;

public interface DataInitService {
    void initRoles();
    void initDepartments();
    void initMajors();
    void initAdminUser();
    void initTestUsers();
    List<Map<String, Object>> getDeptTree();
    List<Map<String, Object>> getMajorsByDeptId(Long deptId);
    void initCompaniesAndJobs();
    void initClasses();
    Map<String, Object> autoInitAll();
    Map<String, Object> resetAndInitAll();
}
