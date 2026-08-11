package com.employment.service;

import com.employment.model.dto.CompanyRegisterDTO;
import com.employment.model.dto.LoginDTO;
import com.employment.model.dto.RegisterDTO;
import com.employment.model.entity.SysUser;
import com.employment.model.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginDTO loginDTO);
    SysUser register(RegisterDTO registerDTO);
    SysUser companyRegister(CompanyRegisterDTO dto);
    LoginVO getUserInfo();
    void logout();
    String refreshToken();
}
