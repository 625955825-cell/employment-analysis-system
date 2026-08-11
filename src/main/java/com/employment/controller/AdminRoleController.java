package com.employment.controller;

import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.entity.SysPermission;
import com.employment.model.entity.SysRole;
import com.employment.model.entity.SysRolePermission;
import com.employment.repository.SysPermissionRepository;
import com.employment.repository.SysRolePermissionRepository;
import com.employment.repository.SysRoleRepository;
import com.employment.repository.SysUserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final SysRoleRepository roleRepository;
    private final SysPermissionRepository permissionRepository;
    private final SysRolePermissionRepository rolePermissionRepository;
    private final SysUserRoleRepository userRoleRepository;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        List<SysRole> roles = roleRepository.findAll();
        List<Map<String, Object>> result = roles.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("roleName", r.getRoleName());
            map.put("roleKey", r.getRoleKey());
            map.put("roleSort", r.getRoleSort());
            map.put("status", r.getStatus());
            map.put("remark", r.getRemark());
            long userCount = userRoleRepository.countByRoleId(r.getId());
            map.put("userCount", userCount);
            return map;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    @GetMapping("/permissions")
    public Result<List<Map<String, Object>>> permissions() {
        List<SysPermission> all = permissionRepository.findAll();
        List<Map<String, Object>> root = new ArrayList<>();
        Map<Long, List<SysPermission>> childrenMap = new HashMap<>();
        for (SysPermission p : all) {
            if (p.getParentId() == null || p.getParentId() == 0L) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", p.getId());
                node.put("permissionName", p.getPermissionName());
                node.put("permissionKey", p.getPermissionKey());
                node.put("permissionType", p.getPermissionType());
                node.put("children", new ArrayList<>());
                root.add(node);
            } else {
                childrenMap.computeIfAbsent(p.getParentId(), k -> new ArrayList<>()).add(p);
            }
        }
        for (Map<String, Object> node : root) {
            Long pid = (Long) node.get("id");
            List<SysPermission> children = childrenMap.get(pid);
            if (children != null) {
                for (SysPermission child : children) {
                    Map<String, Object> childNode = new LinkedHashMap<>();
                    childNode.put("id", child.getId());
                    childNode.put("permissionName", child.getPermissionName());
                    childNode.put("permissionKey", child.getPermissionKey());
                    childNode.put("permissionType", child.getPermissionType());
                    childNode.put("children", new ArrayList<>());
                    ((List<Map<String, Object>>) node.get("children")).add(childNode);
                }
            }
        }
        return Result.success(root);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        SysRole role = roleRepository.findById(id)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "角色不存在"));
        List<Long> permissionIds = rolePermissionRepository.findAll().stream()
                .filter(rp -> rp.getRoleId().equals(id))
                .map(SysRolePermission::getPermissionId)
                .collect(Collectors.toList());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", role.getId());
        data.put("roleName", role.getRoleName());
        data.put("roleKey", role.getRoleKey());
        data.put("roleSort", role.getRoleSort());
        data.put("status", role.getStatus());
        data.put("remark", role.getRemark());
        data.put("permissionIds", permissionIds);
        return Result.success(data);
    }

    @PostMapping
    @Transactional
    @OperationLog(module = "角色管理", content = "创建角色")
    public Result<SysRole> create(@RequestBody SysRole role) {
        if (roleRepository.existsByRoleKey(role.getRoleKey())) {
            throw new com.employment.exception.BusinessException(400, "角色标识已存在");
        }
        role.setId(null);
        return Result.success("角色创建成功", roleRepository.save(role));
    }

    @PutMapping("/{id}")
    @Transactional
    @OperationLog(module = "角色管理", content = "编辑角色")
    public Result<SysRole> update(@PathVariable Long id, @RequestBody SysRole role) {
        SysRole existing = roleRepository.findById(id)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "角色不存在"));
        if (role.getRoleName() != null) existing.setRoleName(role.getRoleName());
        if (role.getRoleSort() != null) existing.setRoleSort(role.getRoleSort());
        if (role.getStatus() != null) existing.setStatus(role.getStatus());
        if (role.getRemark() != null) existing.setRemark(role.getRemark());
        return Result.success("角色更新成功", roleRepository.save(existing));
    }

    @PutMapping("/{id}/permissions")
    @Transactional
    @OperationLog(module = "角色管理", content = "分配权限")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        if (!roleRepository.existsById(id)) {
            throw new com.employment.exception.BusinessException(404, "角色不存在");
        }
        List<SysRolePermission> existing = rolePermissionRepository.findAll().stream()
                .filter(rp -> rp.getRoleId().equals(id))
                .collect(Collectors.toList());
        rolePermissionRepository.deleteAll(existing);

        List<Long> permissionIds = body.get("permissionIds");
        if (permissionIds != null) {
            for (Long pid : permissionIds) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(id);
                rp.setPermissionId(pid);
                rolePermissionRepository.save(rp);
            }
        }
        return Result.success("权限分配成功", null);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @OperationLog(module = "角色管理", content = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        if (!roleRepository.existsById(id)) {
            throw new com.employment.exception.BusinessException(404, "角色不存在");
        }
        long userCount = userRoleRepository.countByRoleId(id);
        if (userCount > 0) {
            throw new com.employment.exception.BusinessException(400, "该角色下有 " + userCount + " 个用户，无法删除");
        }
        List<SysRolePermission> rps = rolePermissionRepository.findAll().stream()
                .filter(rp -> rp.getRoleId().equals(id))
                .collect(Collectors.toList());
        rolePermissionRepository.deleteAll(rps);
        roleRepository.deleteById(id);
        return Result.success("删除成功", null);
    }
}
