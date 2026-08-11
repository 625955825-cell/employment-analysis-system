package com.employment.security;

import com.employment.model.entity.SysRole;
import com.employment.model.entity.SysUser;
import com.employment.model.entity.SysUserRole;
import com.employment.repository.SysRoleRepository;
import com.employment.repository.SysUserRepository;
import com.employment.repository.SysUserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final SysUserRepository sysUserRepository;

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal.toString();
    }

    /**
     * 静态方法：通过 SpringContextHolder 获取 Repository，查询当前用户ID。
     * 用于 Controller 层直接调用，避免每个 Controller 都注入 SecurityUtils。
     */
    public static Long getCurrentUserId() {
        String username = getCurrentUsername();
        if (username == null) {
            return null;
        }
        SysUserRepository repo = SpringContextHolder.getBean(SysUserRepository.class);
        if (repo == null) {
            return null;
        }
        return repo.findByUsername(username)
                .map(SysUser::getId)
                .orElse(null);
    }

    public Long getCurrentUserIdInstance() {
        String username = getCurrentUsername();
        if (username == null) {
            return null;
        }
        return sysUserRepository.findByUsername(username)
                .map(SysUser::getId)
                .orElse(null);
    }

    public String getCurrentRole() {
        Long userId = getCurrentUserId();
        if (userId == null) return null;
        SysUserRoleRepository userRoleRepo = SpringContextHolder.getBean(SysUserRoleRepository.class);
        SysRoleRepository roleRepo = SpringContextHolder.getBean(SysRoleRepository.class);
        if (userRoleRepo == null || roleRepo == null) return null;
        List<SysUserRole> roles = userRoleRepo.findByUserId(userId);
        return roles.stream()
                .findFirst()
                .flatMap(ur -> roleRepo.findById(ur.getRoleId()))
                .map(SysRole::getRoleKey)
                .orElse(null);
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(role));
    }
}
