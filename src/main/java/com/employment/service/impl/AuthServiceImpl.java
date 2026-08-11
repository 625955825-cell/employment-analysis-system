package com.employment.service.impl;

import com.employment.common.Constants;
import com.employment.exception.BusinessException;
import com.employment.model.dto.CompanyRegisterDTO;
import com.employment.model.dto.LoginDTO;
import com.employment.model.dto.RegisterDTO;
import com.employment.model.entity.*;
import com.employment.model.vo.LoginVO;
import com.employment.repository.*;
import com.employment.security.JwtTokenProvider;
import com.employment.security.SecurityUtils;
import com.employment.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final SysUserRepository userRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRoleRepository roleRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final SysDeptRepository deptRepository;
    private final SysClassRepository sysClassRepository;
    private final CompanyInfoRepository companyInfoRepository;
    private final CompanyAuthRepository companyAuthRepository;
    private final SysLogRepository logRepository;
    private final InvitationCodeRepository invitationCodeRepository;
    private final HttpServletRequest request;
    private final SecurityUtils securityUtils;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        SysUser user = userRepository.findByUsername(loginDTO.getUsername())
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 重新生成带userId的token（WebSocket需要）
        token = tokenProvider.generateToken(user.getUsername(), user.getId());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUsername(user.getUsername());
        loginVO.setRealName(user.getRealName());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setUserId(user.getId());

        List<SysUserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        if (!userRoles.isEmpty()) {
            Long roleId = userRoles.get(0).getRoleId();
            SysRole role = roleRepository.findById(roleId).orElse(null);
            if (role != null) {
                loginVO.setRole(role.getRoleKey());
            }
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("deptId", user.getDeptId());
        userInfo.put("studentNo", user.getStudentNo());
        loginVO.setUserInfo(userInfo);

        // 记录登录日志
        saveLoginLog(user, "登录成功", "0");

        return loginVO;
    }

    /**
     * 保存登录/登出日志
     */
    private void saveLoginLog(SysUser user, String description, String status) {
        try {
            SysLog sysLog = new SysLog();
            sysLog.setUserId(user.getId());
            sysLog.setUsername(user.getUsername());
            sysLog.setLogType(Constants.LOG_TYPE_LOGIN); // 登录日志
            sysLog.setModule("系统登录");
            sysLog.setDescription(description);
            sysLog.setMethod("POST");
            sysLog.setUrl("/api/auth/login");
            sysLog.setIp(getClientIp());
            sysLog.setStatus(status);
            logRepository.save(sysLog);
        } catch (Exception e) {
            log.error("记录登录日志失败: {}", e.getMessage());
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
        // 多个代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Override
    @Transactional
    public SysUser register(RegisterDTO registerDTO) {
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException(400, "两次密码输入不一致");
        }

        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            throw new BusinessException(400, "用户名已存在");
        }

        String roleKey = registerDTO.getRole();
        if (roleKey == null || roleKey.isEmpty()) {
            roleKey = Constants.ROLE_STUDENT;
        }

        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRealName(registerDTO.getRealName());
        user.setEmail(registerDTO.getEmail());
        user.setPhone(registerDTO.getPhone());
        user.setStudentNo(registerDTO.getStudentNo());
        user.setDeptId(registerDTO.getDeptId());
        user.setMajorId(registerDTO.getMajorId());
        user.setClassId(registerDTO.getClassId());
        user.setClassName(registerDTO.getClassName());
        if (Constants.ROLE_STUDENT.equals(roleKey) && registerDTO.getClassId() != null) {
            Integer graduationYear = sysClassRepository.findById(registerDTO.getClassId())
                    .map(SysClass::getGrade)
                    .map(grade -> Integer.parseInt(grade) + 2000 + 4)
                    .orElse(null);
            user.setGraduationYear(graduationYear);
        }
        user.setStatus(Constants.STATUS_NORMAL);
        user.setRemark("用户注册");
        SysUser savedUser = userRepository.save(user);

        // 注册码验证（仅老师需要）
        if (!Constants.ROLE_STUDENT.equals(roleKey)) {
            String invitationCode = registerDTO.getInvitationCode();
            if (invitationCode == null || invitationCode.trim().isEmpty()) {
                throw new BusinessException(400, "请输入注册码");
            }
            InvitationCode code = invitationCodeRepository.findByCodeAndStatus(invitationCode.trim(), "unused")
                    .orElseThrow(() -> new BusinessException(400, "注册码无效或已被使用"));
            if (code.getExpiresTime() != null && code.getExpiresTime().isBefore(LocalDateTime.now())) {
                throw new BusinessException(400, "注册码已过期");
            }
            code.setStatus("used");
            code.setUsedBy(savedUser.getId());
            code.setUsedUsername(savedUser.getUsername());
            code.setUsedTime(LocalDateTime.now());
            invitationCodeRepository.save(code);
        }

        roleRepository.findByRoleKey(roleKey).ifPresent(role -> {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(savedUser.getId());
            userRole.setRoleId(role.getId());
            userRoleRepository.save(userRole);
        });

        if (Constants.ROLE_CLASS_TEACHER.equals(roleKey) && savedUser.getClassId() != null) {
            sysClassRepository.findById(savedUser.getClassId()).ifPresent(c -> {
                c.setAdvisor(savedUser.getRealName());
                c.setAdvisorId(savedUser.getId());
                sysClassRepository.save(c);
            });
        }

        if (Constants.ROLE_STUDENT.equals(roleKey) && savedUser.getStudentNo() != null) {
            StudentInfo studentInfo = new StudentInfo();
            studentInfo.setUserId(savedUser.getId());
            studentInfo.setStudentNo(savedUser.getStudentNo());
            studentInfo.setRealName(savedUser.getRealName());
            studentInfo.setPhone(savedUser.getPhone());
            studentInfo.setEmail(savedUser.getEmail());
            studentInfo.setDeptId(savedUser.getDeptId());
            studentInfo.setMajorId(savedUser.getMajorId());
            studentInfo.setClassName(savedUser.getClassName());
            studentInfo.setClassId(savedUser.getClassId());
            studentInfo.setGraduationYear(savedUser.getGraduationYear());
            if (savedUser.getDeptId() != null) {
                deptRepository.findById(savedUser.getDeptId()).ifPresent(dept -> {
                    studentInfo.setDeptName(dept.getDeptName());
                });
            }
            studentInfoRepository.save(studentInfo);
        }

        log.info("用户注册成功: {}", savedUser.getUsername());
        return savedUser;
    }

    @Override
    @Transactional
    public SysUser companyRegister(CompanyRegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(400, "两次密码输入不一致");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException(400, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getContactPerson());
        user.setPhone(dto.getContactPhone());
        user.setEmail(dto.getContactEmail());
        user.setStatus(Constants.STATUS_NORMAL);
        user.setDeptId(dto.getDeptId());
        user.setRemark("企业入驻");
        SysUser savedUser = userRepository.save(user);

        roleRepository.findByRoleKey(Constants.ROLE_COMPANY).ifPresent(role -> {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(savedUser.getId());
            userRole.setRoleId(role.getId());
            userRoleRepository.save(userRole);
        });

        CompanyInfo company = new CompanyInfo();
        company.setUserId(savedUser.getId());
        company.setCompanyName(dto.getCompanyName());
        company.setContactPerson(dto.getContactPerson());
        company.setContactPhone(dto.getContactPhone());
        company.setContactEmail(dto.getContactEmail());
        company.setUnifiedCreditCode(dto.getUnifiedCreditCode());
        company.setProvince(dto.getProvince());
        company.setCity(dto.getCity());
        company.setDistrict(dto.getDistrict());
        company.setAddress(dto.getAddress());
        company.setIndustry(dto.getIndustry());
        company.setScale(dto.getScale());
        company.setNature(dto.getNature());
        company.setIntroduction(dto.getIntroduction());
        company.setAuthStatus("pending");
        company.setStatus("0");
        company.setDeptId(dto.getDeptId());
        CompanyInfo savedCompany = companyInfoRepository.save(company);

        // 创建企业认证记录，等待院级老师审核
        CompanyAuth auth = new CompanyAuth();
        auth.setCompanyId(savedCompany.getId());
        auth.setAuthType("营业执照");
        auth.setAuthName(dto.getCompanyName());
        auth.setAuditStatus("pending");
        companyAuthRepository.save(auth);

        log.info("企业入驻成功: {}, 企业: {}", savedUser.getUsername(), dto.getCompanyName());
        return savedUser;
    }

    @Override
    public LoginVO getUserInfo() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new BusinessException(401, "请先登录");
        }

        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        LoginVO loginVO = new LoginVO();
        loginVO.setUsername(user.getUsername());
        loginVO.setRealName(user.getRealName());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setUserId(user.getId());

        List<SysUserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        if (!userRoles.isEmpty()) {
            Long roleId = userRoles.get(0).getRoleId();
            SysRole role = roleRepository.findById(roleId).orElse(null);
            if (role != null) {
                loginVO.setRole(role.getRoleKey());
            }
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("deptId", user.getDeptId());
        userInfo.put("majorId", user.getMajorId());
        userInfo.put("studentNo", user.getStudentNo());
        loginVO.setUserInfo(userInfo);

        return loginVO;
    }

    @Override
    public void logout() {
        SecurityContextHolder.clearContext();
    }

    @Override
    public String refreshToken() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new BusinessException(401, "请先登录");
        }
        Long userId = securityUtils.getCurrentUserIdInstance();
        return tokenProvider.generateToken(username, userId);
    }
}
