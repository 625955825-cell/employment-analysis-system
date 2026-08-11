package com.employment.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String email;
    private String studentNo;
    private Long deptId;
    private Long majorId;
    private String className;
    private Long classId;
    private String role;
    private String invitationCode;
    private String phone;
}
