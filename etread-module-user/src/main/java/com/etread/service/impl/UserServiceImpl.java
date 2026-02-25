package com.etread.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etread.dto.RegisterDTO;
import com.etread.entity.User;
import com.etread.common.MinioUtil;
import com.etread.mapper.UserMapper;
import com.etread.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private MinioUtil minioUtil;
    @Override
    public void register(RegisterDTO dto){
       //查重
        if(this.getOne(new LambdaQueryWrapper<User>().eq(User::getAccount,dto.getAccount()))!=null){
            throw new RuntimeException("账号已注册！");
        }
        String avatarurl="";
        try{
            if(dto.getAvatarFile()!=null&&!dto.getAvatarFile().isEmpty()){
                avatarurl=minioUtil.uploadFile(dto.getAvatarFile(), "avatars");
            }else{
                avatarurl="http://localhost:9000/avatars/default-user.png";
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("服务器好像出问题了：" + e.getMessage());
        }
        User user=new User();
        user.setAccount(dto.getAccount());
        user.setPassword(bCryptPasswordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setAvatar(avatarurl);
        this.save(user);
    }



}
