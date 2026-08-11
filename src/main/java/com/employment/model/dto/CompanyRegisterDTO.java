package com.employment.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompanyRegisterDTO {

    @NotNull(message = "请选择入驻学院")
    private Long deptId;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @NotBlank(message = "联系人姓名不能为空")
    private String contactPerson;

    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    private String contactEmail;

    @NotBlank(message = "企业名称不能为空")
    private String companyName;

    private String unifiedCreditCode;

    private String province;

    private String city;

    private String district;

    private String address;

    private String industry;

    private String scale;

    private String nature;

    private String introduction;
}
