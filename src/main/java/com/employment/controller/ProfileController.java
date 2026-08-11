package com.employment.controller;

import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.exception.BusinessException;
import com.employment.model.entity.SysUser;
import com.employment.repository.SysUserRepository;
import com.employment.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final SysUserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public Result<Map<String, Object>> getProfile() {
        Long userId = securityUtils.getCurrentUserId();
        String username = securityUtils.getCurrentUsername();
        String role = securityUtils.getCurrentRole();

        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new com.employment.exception.BusinessException("用户不存在"));

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("username", username);
        data.put("realName", user.getRealName() != null ? user.getRealName() : username);
        data.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
        data.put("phone", user.getPhone() != null ? user.getPhone() : "");
        data.put("email", user.getEmail() != null ? user.getEmail() : "");
        data.put("role", role);
        data.put("roleName", getRoleName(role));
        data.put("deptId", user.getDeptId() != null ? user.getDeptId() : 0L);
        return Result.success(data);
    }

    @PutMapping
    @Transactional
    @OperationLog(module = "个人中心", content = "更新个人资料")
    public Result<Void> updateProfile(@RequestBody Map<String, String> params) {
        Long userId = securityUtils.getCurrentUserId();
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new com.employment.exception.BusinessException("用户不存在"));
        if (params.containsKey("realName")) user.setRealName(params.get("realName"));
        if (params.containsKey("phone")) user.setPhone(params.get("phone"));
        if (params.containsKey("email")) user.setEmail(params.get("email"));
        if (params.containsKey("avatar")) user.setAvatar(params.get("avatar"));
        userRepository.save(user);
        return Result.success();
    }

    @PostMapping("/avatar")
    @Transactional
    @OperationLog(module = "个人中心", content = "上传头像")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = securityUtils.getCurrentUserId();
        try {
            String uploadDir = "./uploads/avatars";
            Path dir = Paths.get(uploadDir);
            if (!Files.exists(dir)) Files.createDirectories(dir);

            String originalFilename = file.getOriginalFilename();
            String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
            String filename = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path path = dir.resolve(filename);
            Files.copy(file.getInputStream(), path);

            String avatarPath = "/uploads/avatars/" + filename;
            userRepository.findById(userId).ifPresent(u -> {
                u.setAvatar(avatarPath);
                userRepository.save(u);
            });
            return Result.success(avatarPath);
        } catch (Exception e) {
            throw new com.employment.exception.BusinessException("头像上传失败: " + e.getMessage());
        }
    }

    @PutMapping("/password")
    @Transactional
    @OperationLog(module = "个人中心", content = "修改密码")
    public Result<Void> changePassword(@RequestBody Map<String, String> params) {
        Long userId = securityUtils.getCurrentUserId();
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        if (oldPassword == null || oldPassword.isEmpty()) {
            throw new BusinessException(400, "旧密码不能为空");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new BusinessException(400, "新密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException(400, "新密码长度不能少于6位");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "旧密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return Result.success();
    }

    @PutMapping("/username")
    @Transactional
    @OperationLog(module = "个人中心", content = "修改用户名")
    public Result<Void> changeUsername(@RequestBody Map<String, String> params) {
        Long userId = securityUtils.getCurrentUserId();
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        String newUsername = params.get("username");
        if (newUsername == null || newUsername.trim().isEmpty()) {
            throw new BusinessException(400, "用户名不能为空");
        }
        String trimmed = newUsername.trim();
        if (trimmed.length() < 4 || trimmed.length() > 30) {
            throw new BusinessException(400, "用户名长度需在4-30个字符之间");
        }
        if (userRepository.findByUsername(trimmed).isPresent()
                && !trimmed.equals(user.getUsername())) {
            throw new BusinessException(400, "用户名已被占用");
        }

        user.setUsername(trimmed);
        userRepository.save(user);
        return Result.success();
    }

    private String getRoleName(String role) {
        if (role == null) return "";
        return switch (role) {
            case "student" -> "学生";
            case "class_teacher" -> "班主任";
            case "dept_teacher" -> "院级管理员";
            case "admin" -> "校级管理员";
            case "company" -> "企业用户";
            case "employment_staff" -> "就业专员";
            default -> role;
        };
    }
}
