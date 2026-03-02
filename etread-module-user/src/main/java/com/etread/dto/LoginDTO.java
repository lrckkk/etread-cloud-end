package com.etread.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "无账号登录？")
    @Size(min=3,max=20)
    private String account;
    @NotBlank(message = "无密码登录？")
    @Size(min=6,max=20)
    private String password;
    private String token;
}
