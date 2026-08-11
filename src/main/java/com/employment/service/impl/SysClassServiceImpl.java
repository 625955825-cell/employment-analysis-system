package com.employment.service.impl;

import com.employment.common.PageResult;
import com.employment.exception.BusinessException;
import com.employment.model.dto.BatchGenerateClassRequest;
import com.employment.model.entity.SysClass;
import com.employment.model.entity.SysDept;
import com.employment.model.entity.SysMajor;
import com.employment.repository.SysClassRepository;
import com.employment.repository.SysDeptRepository;
import com.employment.repository.SysMajorRepository;
import com.employment.repository.SysUserRepository;
import com.employment.service.SysClassService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysClassServiceImpl implements SysClassService {

    private final SysClassRepository sysClassRepository;
    private final SysDeptRepository sysDeptRepository;
    private final SysMajorRepository sysMajorRepository;
    private final SysUserRepository sysUserRepository;

    @Override
    public PageResult<SysClass> list(String keyword, Long deptId, Long majorId, Integer pageNum, Integer pageSize) {
        Specification<SysClass> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                Predicate classNameLike = cb.like(root.get("className"), "%" + keyword + "%");
                List<Long> matchingMajorIds = sysMajorRepository.findIdsByMajorNameContaining(keyword);
                if (!matchingMajorIds.isEmpty()) {
                    predicates.add(cb.or(classNameLike, root.get("majorId").in(matchingMajorIds)));
                } else {
                    predicates.add(classNameLike);
                }
            }
            if (deptId != null) {
                predicates.add(cb.equal(root.get("deptId"), deptId));
            }
            if (majorId != null) {
                predicates.add(cb.equal(root.get("majorId"), majorId));
            }
            query.where(predicates.toArray(new Predicate[0]));
            query.orderBy(cb.desc(root.get("createTime")));
            return query.getRestriction();
        };
        Page<SysClass> page = sysClassRepository.findAll(spec, PageRequest.of(pageNum - 1, pageSize));
        List<SysClass> records = page.getContent();

        Map<Long, Long> countMap = sysClassRepository.countStudentsByClassId().stream()
                .collect(Collectors.toMap(
                        m -> (Long) m.get("classId"),
                        m -> (Long) m.get("studentCount")
                ));

        Map<Long, String> deptMap = sysDeptRepository.findAll().stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName));
        Map<Long, String> majorMap = sysMajorRepository.findAll().stream()
                .collect(Collectors.toMap(SysMajor::getId, SysMajor::getMajorName));

        for (SysClass c : records) {
            c.setStudentCount(countMap.getOrDefault(c.getId(), 0L).intValue());
            c.setDeptName(deptMap.get(c.getDeptId()));
            c.setMajorName(majorMap.get(c.getMajorId()));
        }

        return new PageResult<>(page.getTotalElements(), records);
    }

    @Override
    public SysClass getById(Long id) {
        return sysClassRepository.findById(id)
                .orElseThrow(() -> new BusinessException("班级不存在"));
    }

    @Override
    @Transactional
    public SysClass save(SysClass sysClass) {
        if (sysClassRepository.existsByClassNameAndMajorId(sysClass.getClassName(), sysClass.getMajorId())) {
            throw new BusinessException("该专业下已存在同名班级");
        }
        sysClass.setStatus("0");
        return sysClassRepository.save(sysClass);
    }

    @Override
    @Transactional
    public SysClass update(Long id, SysClass sysClass) {
        SysClass existing = getById(id);
        if (!existing.getClassName().equals(sysClass.getClassName()) &&
                sysClassRepository.existsByClassNameAndMajorId(sysClass.getClassName(), sysClass.getMajorId())) {
            throw new BusinessException("该专业下已存在同名班级");
        }
        existing.setClassName(sysClass.getClassName());
        existing.setMajorId(sysClass.getMajorId());
        existing.setDeptId(sysClass.getDeptId());
        existing.setGrade(sysClass.getGrade());
        existing.setAdvisor(sysClass.getAdvisor());
        existing.setRemark(sysClass.getRemark());
        return sysClassRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        sysClassRepository.deleteById(id);
    }

    @Override
    public List<Map<String, Object>> getByMajorId(Long majorId) {
        List<SysClass> classes;
        if (majorId == null) {
            classes = sysClassRepository.findAll();
        } else {
            classes = sysClassRepository.findByMajorId(majorId);
        }
        Map<Long, Long> countMap = sysClassRepository.countStudentsByClassId().stream()
                .collect(Collectors.toMap(
                        m -> (Long) m.get("classId"),
                        m -> (Long) m.get("studentCount")
                ));
        return classes.stream().map(c -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", c.getId());
            map.put("className", c.getClassName());
            map.put("grade", c.getGrade());
            map.put("majorId", c.getMajorId());
            map.put("deptId", c.getDeptId());
            map.put("advisor", c.getAdvisor());
            map.put("advisorId", c.getAdvisorId());
            map.put("studentCount", countMap.getOrDefault(c.getId(), 0L).intValue());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getAllClasses() {
        List<SysClass> classes = sysClassRepository.findAll();
        Map<Long, String> deptMap = sysDeptRepository.findAll().stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName));
        Map<Long, String> majorMap = sysMajorRepository.findAll().stream()
                .collect(Collectors.toMap(SysMajor::getId, SysMajor::getMajorName));
        Map<Long, Long> countMap = sysClassRepository.countStudentsByClassId().stream()
                .collect(Collectors.toMap(m -> (Long) m.get("classId"), m -> (Long) m.get("studentCount")));
        return classes.stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getId());
            map.put("className", c.getClassName());
            map.put("majorId", c.getMajorId());
            map.put("majorName", majorMap.get(c.getMajorId()));
            map.put("deptId", c.getDeptId());
            map.put("deptName", deptMap.get(c.getDeptId()));
            map.put("grade", c.getGrade());
            map.put("advisor", c.getAdvisor());
            map.put("advisorId", c.getAdvisorId());
            map.put("studentCount", countMap.getOrDefault(c.getId(), 0L).intValue());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<SysClass> batchGenerateClasses(BatchGenerateClassRequest request) {
        String grade = request.getGrade();
        Map<Long, Integer> customCounts = request.getCustomClassCounts();
        List<SysClass> generatedClasses = new ArrayList<>();

        List<SysMajor> allMajors = sysMajorRepository.findAll();

        for (SysMajor major : allMajors) {
            int count = (customCounts != null && customCounts.containsKey(major.getId()))
                    ? customCounts.get(major.getId())
                    : request.getClassCountPerMajor();

            for (int i = 1; i <= count; i++) {
                String shortName = major.getShortName() != null && !major.getShortName().trim().isEmpty()
                        ? major.getShortName() : major.getMajorName();
                String className = shortName + grade.substring(2) + i;
                if (sysClassRepository.existsByClassNameAndMajorId(className, major.getId())) {
                    continue;
                }
                SysClass sysClass = new SysClass();
                sysClass.setClassName(className);
                sysClass.setMajorId(major.getId());
                sysClass.setDeptId(major.getDeptId());
                sysClass.setGrade(grade);
                sysClass.setStatus("0");
                generatedClasses.add(sysClassRepository.save(sysClass));
            }
        }

        return generatedClasses;
    }

    @Override
    @Transactional
    public void batchDeleteByGrade(String grade) {
        List<SysClass> classes = sysClassRepository.findByGrade(grade);
        for (SysClass cls : classes) {
            sysUserRepository.deleteByClassId(cls.getId());
        }
        sysClassRepository.deleteByGrade(grade);
    }
}
