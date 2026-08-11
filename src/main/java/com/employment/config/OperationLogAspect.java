package com.employment.config;

import cn.hutool.json.JSONUtil;
import com.employment.common.Constants;
import com.employment.model.entity.SysLog;
import com.employment.model.entity.SysUser;
import com.employment.repository.SysLogRepository;
import com.employment.repository.SysUserRepository;
import com.employment.security.SpringContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面：自动拦截所有 Controller 方法，记录操作日志到 sys_log 表。
 * 只有标注了 @OperationLog 的方法才记录。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysLogRepository sysLogRepository;

    @Pointcut("@annotation(com.employment.config.OperationLog)")
    public void operationLogPointcut() {}

    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long start = System.currentTimeMillis();

        HttpServletRequest request = getRequest();
        if (request == null) {
            return point.proceed();
        }

        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        SysLog sysLog = new SysLog();
        sysLog.setLogType(Constants.LOG_TYPE_OPERATION);
        sysLog.setModule(annotation.module());
        sysLog.setDescription(annotation.content());
        sysLog.setMethod(request.getMethod());
        sysLog.setUrl(request.getRequestURI());

        String username = null;
        Long userId = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails ud) {
                username = ud.getUsername();
                // 从 username 查询数据库获取 userId
                userId = SpringContextHolder.getBean(SysUserRepository.class)
                        .findByUsername(username)
                        .map(SysUser::getId)
                        .orElse(null);
            } else if (principal instanceof String s) {
                username = s;
            }
        }
        sysLog.setUserId(userId);
        sysLog.setUsername(username);
        sysLog.setIp(getClientIp(request));

        String params = null;
        try {
            Object[] args = point.getArgs();
            if (args != null && args.length > 0) {
                Object filtered = filterArgs(args);
                params = JSONUtil.toJsonStr(filtered);
                if (params != null && params.length() > 2000) {
                    params = params.substring(0, 2000);
                }
            }
        } catch (Exception ignored) {}
        sysLog.setParams(params);

        Object result = null;
        String status = "0";
        String errorMsg = null;
        try {
            result = point.proceed();
            status = "0";
        } catch (Throwable t) {
            status = "1";
            errorMsg = t.getMessage();
            throw t;
        } finally {
            sysLog.setStatus(status);
            sysLog.setErrorMsg(errorMsg);
            sysLog.setCostTime(System.currentTimeMillis() - start);
            sysLog.setCreateTime(LocalDateTime.now());
            try {
                sysLogRepository.save(sysLog);
            } catch (Exception e) {
                log.warn("记录操作日志失败: {}", e.getMessage());
            }
        }
        return result;
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 过滤掉 HttpServletRequest 等不可序列化的参数
     */
    private Object filterArgs(Object[] args) {
        if (args == null) return null;
        java.util.List<Object> filtered = new java.util.ArrayList<>();
        for (Object arg : args) {
            if (arg == null) continue;
            String typeName = arg.getClass().getName();
            if (typeName.startsWith("jakarta.servlet") || typeName.startsWith("org.springframework")) {
                continue;
            }
            filtered.add(arg);
        }
        return filtered;
    }
}
