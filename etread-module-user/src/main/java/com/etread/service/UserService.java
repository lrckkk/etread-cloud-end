package com.etread.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.etread.dto.RegisterDTO;
import com.etread.entity.User;

public interface UserService extends IService<User> {

    void register(RegisterDTO dto);
}
