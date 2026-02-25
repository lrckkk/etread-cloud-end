package com.etread.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空哦！")
    @Size(min = 3, max = 20, message = "用户名长度要在3-20之间哦！")
    private String user_id;

    @NotBlank(message = "密码不能为空哦！")
    @Size(min = 6, message = "密码最少要6位哦！")
    private String password;

    private String nickname;

    // 用来接收前端传来的头像文件
    private MultipartFile avatarFile;

    private String account;
}