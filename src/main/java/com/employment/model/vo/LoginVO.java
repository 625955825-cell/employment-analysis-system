package com.employment.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {
    private String token;
    private String username;
    private String realName;
    private String avatar;
    private String role;
    private Long userId;
    private Map<String, Object> userInfo;
    private List<String> permissions;
}
