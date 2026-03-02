package com.etread.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.etread.dto.LoginDTO;
import com.etread.dto.RegisterDTO;
import com.etread.entity.User;
import com.etread.vo.UserVO;

public interface UserService extends IService<User> {

    UserVO register(RegisterDTO dto);
    UserVO login(LoginDTO dto);
    UserVO converUserToVO(User user,String token);
    void logout(LoginDTO dto);

}
