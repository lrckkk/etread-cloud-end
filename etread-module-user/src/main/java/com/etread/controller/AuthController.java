package com.etread.controller;

import com.etread.Result;
import com.etread.dto.LoginDTO;
import com.etread.dto.RegisterDTO;
import com.etread.service.UserService;
import com.etread.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<UserVO> register(RegisterDTO dto) {

            UserVO userVO = userService.register(dto);
            String token = userVO.getToken();
            System.out.println("token:"+token);
            return Result.success("注册成功！", userVO);
    }
    @PostMapping("/login")
    public Result<UserVO> login(LoginDTO dto) {

            UserVO userVO = userService.login(dto);
            String token = userVO.getToken();
            System.out.println("token:"+token);
            return Result.success("登录成功！",userVO);
    }
    @PostMapping("/logout")
    public Result logout(LoginDTO dto) {
        // 安全！只能销毁发起请求者自己的凭证
        userService.logout(dto);
        return Result.success("登出成功",null);
    }
}