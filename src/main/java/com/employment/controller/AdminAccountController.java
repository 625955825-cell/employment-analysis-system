package com.employment.controller;

import com.employment.common.PageResult;
import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.entity.SysRole;
import com.employment.model.entity.SysClass;
import com.employment.model.entity.SysUser;
import com.employment.model.entity.SysUserRole;
import com.employment.repository.SysClassRepository;
import com.employment.repository.SysRoleRepository;
import com.employment.repository.SysUserRepository;
import com.employment.repository.SysUserRoleRepository;
import com.employment.util.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysClassRepository sysClassRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long deptId) {
        // 预加载所有角色和用户-角色关联（避免N+1查询）
        List<SysRole> allRoles = roleRepository.findAll();
          Map<Long, SysRole> roleMap = allRoles.stream().collect(Collectors.toMap(SysRole::getId, r -> r));
        Map<Long, Long> userRoleMap = userRoleRepository.findAll().stream()
                .collect(Collectors.toMap(SysUserRole::getUserId, SysUserRole::getRoleId, (a, b) -> a));

        // 预加载所有用户（keyword、role、deptId 任一存在时查全量，否则用分页）
        List<SysUser> allUsers;
        boolean hasKeyword = keyword != null && !keyword.isEmpty();
        boolean hasRole = role != null && !role.isEmpty();
        boolean hasDeptId = deptId != null;
        if (hasKeyword || hasRole || hasDeptId) {
            allUsers = userRepository.findAll();
        } else {
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));
            allUsers = userRepository.findAll(pageable).getContent();
        }

        // 按条件过滤
        Stream<SysUser> stream = allUsers.stream();
        if (keyword != null && !keyword.isEmpty()) {
            String kw = keyword.toLowerCase();
            stream = stream.filter(u ->
                    (u.getUsername() != null && u.getUsername().toLowerCase().contains(kw)) ||
                    (u.getRealName() != null && u.getRealName().toLowerCase().contains(kw)));
        }
        if (role != null && !role.isEmpty()) {
            stream = stream.filter(u -> {
                Long roleId = userRoleMap.get(u.getId());
                SysRole r = roleId != null ? roleMap.get(roleId) : null;
                return r != null && role.equals(r.getRoleKey());
            });
        }
        if (deptId != null) {
            stream = stream.filter(u -> deptId.equals(u.getDeptId()));
        }

        // 有keyword时需要内存分页
        List<SysUser> filtered;
        if (keyword != null && !keyword.isEmpty()) {
            filtered = stream.collect(Collectors.toList());
            int start = (page - 1) * size;
            int end = Math.min(start + size, filtered.size());
            filtered = start < filtered.size() ? filtered.subList(start, end) : Collections.emptyList();
        } else {
            filtered = stream.collect(Collectors.toList());
        }

        final Map<Long, SysRole> finalRoleMap = roleMap;
        final Map<Long, Long> finalUserRoleMap = userRoleMap;
        List<Map<String, Object>> records = filtered.stream()
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("username", u.getUsername());
                    m.put("realName", u.getRealName());
                    m.put("phone", u.getPhone());
                    m.put("email", u.getEmail());
                    m.put("status", u.getStatus());
                    m.put("deptId", u.getDeptId());
                    m.put("className", u.getClassName());
                    m.put("studentNo", u.getStudentNo());
                    Long roleId = finalUserRoleMap.get(u.getId());
                    if (roleId != null) {
                        SysRole r = finalRoleMap.get(roleId);
                        if (r != null) {
                            m.put("role", r.getRoleKey());
                            m.put("roleName", r.getRoleName());
                        }
                    }
                    return m;
                })
                .collect(Collectors.toList());

        long total = (hasKeyword || hasRole || hasDeptId)
                ? allUsers.stream().filter(u -> {
                    if (hasRole) {
                        Long rid = userRoleMap.get(u.getId());
                        SysRole r = rid != null ? roleMap.get(rid) : null;
                        if (r == null || !role.equals(r.getRoleKey())) return false;
                    }
                    if (hasDeptId && !deptId.equals(u.getDeptId())) return false;
                    return true;
                }).count()
                : userRepository.count();

        return Result.success(new PageResult<>(total, records));
    }

    @GetMapping("/roles")
    public Result<List<Map<String, Object>>> getRoles() {
        List<Map<String, Object>> roles = roleRepository.findAll().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("roleKey", r.getRoleKey());
                    m.put("roleName", r.getRoleName());
                    m.put("status", r.getStatus());
                    return m;
                })
                .collect(Collectors.toList());
        return Result.success(roles);
    }

    @PostMapping
    @Transactional
    @OperationLog(module = "账号管理", content = "创建账号")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        if (username == null || username.isEmpty()) {
            throw new com.employment.exception.BusinessException(400, "用户名不能为空");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new com.employment.exception.BusinessException(400, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        String password = body.get("password") != null ? (String) body.get("password") : "123456";
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(body.get("realName") != null ? (String) body.get("realName") : username);
        user.setPhone((String) body.get("phone"));
        user.setEmail((String) body.get("email"));
        user.setStatus("0");

        // 院系
        if (body.get("deptId") != null) {
            user.setDeptId(((Number) body.get("deptId")).longValue());
        }
        // 班级ID -> 设置 classId 和 className
        if (body.get("classId") != null) {
            Long classId = ((Number) body.get("classId")).longValue();
            user.setClassId(classId);
            sysClassRepository.findById(classId).ifPresent(cls -> user.setClassName(cls.getClassName()));
        }
        // 学号
        if (body.get("studentNo") != null) {
            user.setStudentNo((String) body.get("studentNo"));
        }

        SysUser savedUser = userRepository.save(user);

        String roleKey = (String) body.get("role");
        if (roleKey != null && !roleKey.isEmpty()) {
            roleRepository.findByRoleKey(roleKey).ifPresent(r -> {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(savedUser.getId());
                ur.setRoleId(r.getId());
                userRoleRepository.save(ur);
            });
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", savedUser.getId());
        result.put("username", savedUser.getUsername());
        result.put("realName", savedUser.getRealName());
        // 返回明文密码供管理员告知用户，不要返回哈希
        result.put("password", password);
        return Result.success("账号创建成功，默认密码为 " + password, result);
    }

    @PutMapping("/{id}/reset-password")
    @Transactional
    @OperationLog(module = "账号管理", content = "重置密码")
    public Result<String> resetPassword(@PathVariable Long id) {
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "用户不存在"));
        String newPwd = PasswordGenerator.generate(8);
        user.setPassword(passwordEncoder.encode(newPwd));
        userRepository.save(user);
        return Result.success("密码重置成功，新密码：" + newPwd, newPwd);
    }

    @PutMapping("/{id}/role")
    @Transactional
    @OperationLog(module = "账号管理", content = "分配角色")
    public Result<Void> assignRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String roleKey = body.get("role");
        userRoleRepository.deleteByUserId(id);
        if (roleKey != null && !roleKey.isEmpty()) {
            roleRepository.findByRoleKey(roleKey).ifPresent(r -> {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(id);
                ur.setRoleId(r.getId());
                userRoleRepository.save(ur);
            });
        }
        return Result.success("角色分配成功", null);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @OperationLog(module = "账号管理", content = "删除账号")
    public Result<Void> delete(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new com.employment.exception.BusinessException(404, "用户不存在");
        }
        userRoleRepository.deleteByUserId(id);
        userRepository.deleteById(id);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}")
    @Transactional
    @OperationLog(module = "账号管理", content = "编辑账号")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        SysUser existing = userRepository.findById(id)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "用户不存在"));
        if (user.getRealName() != null) existing.setRealName(user.getRealName());
        if (user.getPhone() != null) existing.setPhone(user.getPhone());
        if (user.getEmail() != null) existing.setEmail(user.getEmail());
        if (user.getStatus() != null) existing.setStatus(user.getStatus());
        userRepository.save(existing);
        return Result.success("修改成功", null);
    }

    private Map<String, Object> toMap(SysUser u, List<SysRole> allRoles) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("username", u.getUsername());
        map.put("realName", u.getRealName());
        map.put("phone", u.getPhone());
        map.put("email", u.getEmail());
        map.put("status", u.getStatus());
        map.put("deptId", u.getDeptId());
        map.put("className", u.getClassName());
        map.put("studentNo", u.getStudentNo());

        String role = userRoleRepository.findByUserId(u.getId()).stream()
                .findFirst()
                .flatMap(ur -> roleRepository.findById(ur.getRoleId()).map(SysRole::getRoleKey))
                .orElse("");
        map.put("role", role);

        String roleName = userRoleRepository.findByUserId(u.getId()).stream()
                .findFirst()
                .flatMap(ur -> roleRepository.findById(ur.getRoleId()).map(SysRole::getRoleName))
                .orElse("");
        map.put("roleName", roleName);

        return map;
    }
}
