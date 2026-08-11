package com.employment.controller;

import com.employment.common.Constants;
import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.dto.CompanyRegisterDTO;
import com.employment.model.dto.LoginDTO;
import com.employment.model.dto.RegisterDTO;
import com.employment.model.entity.SysLog;
import com.employment.model.entity.SysUser;
import com.employment.model.vo.LoginVO;
import com.employment.repository.SysLogRepository;
import com.employment.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SysLogRepository logRepository;
    private final HttpServletRequest request;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        try {
            return Result.success("登录成功", authService.login(loginDTO));
        } catch (Exception e) {
            // 记录登录失败日志
            saveLoginFailLog(loginDTO.getUsername(), e.getMessage());
            throw e;
        }
    }

    /**
     * 记录登录失败日志
     */
    private void saveLoginFailLog(String username, String errorMsg) {
        try {
            SysLog sysLog = new SysLog();
            sysLog.setUsername(username);
            sysLog.setLogType(Constants.LOG_TYPE_LOGIN); // 登录日志
            sysLog.setModule("系统登录");
            sysLog.setDescription("登录失败: " + errorMsg);
            sysLog.setMethod("POST");
            sysLog.setUrl("/api/auth/login");
            sysLog.setIp(getClientIp());
            sysLog.setStatus("1"); // 失败状态
            sysLog.setErrorMsg(errorMsg);
            logRepository.save(sysLog);
        } catch (Exception ex) {
            // 忽略日志记录失败
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @PostMapping("/register")
    @OperationLog(module = "系统登录", content = "用户注册")
    public Result<SysUser> register(@Valid @RequestBody RegisterDTO registerDTO) {
        return Result.success("注册成功", authService.register(registerDTO));
    }

    @PostMapping("/company-register")
    @OperationLog(module = "企业管理", content = "企业入驻申请")
    public Result<SysUser> companyRegister(@Valid @RequestBody CompanyRegisterDTO dto) {
        return Result.success("入驻申请已提交，请等待审核", authService.companyRegister(dto));
    }

    @GetMapping("/userinfo")
    public Result<LoginVO> getUserInfo() {
        return Result.success(authService.getUserInfo());
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        // 记录登出日志
        try {
            String username = com.employment.security.SecurityUtils.getCurrentUsername();
            if (username != null) {
                SysLog sysLog = new SysLog();
                sysLog.setUsername(username);
                sysLog.setLogType(Constants.LOG_TYPE_LOGOUT);
                sysLog.setModule("系统登出");
                sysLog.setDescription("用户退出登录");
                sysLog.setMethod("POST");
                sysLog.setUrl("/api/auth/logout");
                sysLog.setIp(getClientIp());
                sysLog.setStatus("0");
                sysLog.setCreateTime(LocalDateTime.now());
                logRepository.save(sysLog);
            }
        } catch (Exception e) {
            // 忽略日志记录失败
        }
        authService.logout();
        return Result.success("退出成功", null);
    }

    @PostMapping("/refresh")
    @OperationLog(module = "系统登录", content = "刷新Token")
    public Result<String> refreshToken() {
        return Result.success("刷新成功", authService.refreshToken());
    }
}
