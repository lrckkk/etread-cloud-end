package com.etread.controller;

import com.etread.Result;
import com.etread.dto.RegisterDTO;
import com.etread.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired private UserService userService;

    @PostMapping("/register")
    public Result<String> register(RegisterDTO dto) {
        try {
            String token=userService.register(dto);
            return Result.success("注册成功！", token);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}