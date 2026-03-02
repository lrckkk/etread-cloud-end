package com.etread.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etread.utils.MinioUtil;
import com.etread.constant.AuthConstant;
import com.etread.dto.LoginDTO;
import com.etread.dto.RegisterDTO;
import com.etread.dto.UserDTO;
import com.etread.entity.User;
import com.etread.mapper.UserMapper;
import com.etread.service.UserService;
import com.etread.utils.RedisUtil;
import com.etread.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private MinioUtil minioUtil;
    @Autowired
    private RedisUtil redisUtil;
    //translate to vo
    @Override
    public UserVO converUserToVO(User user,String token){
        UserVO vo = new UserVO();
        vo.setId(String.valueOf(user.getUserId())); // 转 String 防止精度丢失
        vo.setAccount(user.getAccount());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setToken(token);
        return vo;
    }

    //注册功能
    @Override
    public UserVO register(RegisterDTO dto){

        User existingUserAccount = this.getOne(new LambdaQueryWrapper<User>().eq(User::getAccount, dto.getAccount()));
        User exisingUserNickname= this.getOne(new LambdaQueryWrapper<User>().eq(User::getNickname, dto.getNickname()));
        if (existingUserAccount != null) {
            throw new RuntimeException("账号已注册！");
        }else if(exisingUserNickname != null){
            throw new RuntimeException("这个昵称"+exisingUserNickname.getNickname()+"已经有了，换一个8");
        }
        String avatarurl="";
        try{
            if(dto.getAvatarFile()!=null&&!dto.getAvatarFile().isEmpty()){
                avatarurl=minioUtil.uploadFile(dto.getAvatarFile(), "avatars");
                System.out.println("文件名："+dto.getAvatarFile().getOriginalFilename());
            }else{
                System.out.println("未收到文件");
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
        user.setCreatetime(new Date());
        this.save(user);
        //将符号去掉确保token生成纯英文
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = AuthConstant.LOGIN_TOKEN_PREFIX + token;
        UserDTO userDTO=new UserDTO();
        userDTO.setAccount(user.getAccount());
        userDTO.setUser_id(user.getUserId());
        userDTO.setNickname(user.getNickname());
        userDTO.setAvatar(user.getAvatar());
        String userJson=JSON.toJSONString(userDTO);
        redisUtil.set(key,userJson, AuthConstant.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        UserVO userVO=converUserToVO(user,token);
        return userVO;
    }
//    登录并生成token
    @Override
    public UserVO login(LoginDTO dto) {
        User user=this.getOne(new LambdaQueryWrapper<User>().eq(User::getAccount, dto.getAccount()));
        System.out.println("userid in user"+user.getUserId());
        if(user==null){
            throw new RuntimeException("账号不存在");
        }
        //密码检验
        if(!bCryptPasswordEncoder.matches(dto.getPassword(),user.getPassword())){
            throw new RuntimeException("密码错误");
        }
        String token= UUID.randomUUID().toString().replace("-","");
        //store user information
        String key= AuthConstant.LOGIN_TOKEN_PREFIX+token;
        //为了职责不混淆，将user存入userdto存入token
        UserDTO userDTO=new UserDTO();
        userDTO.setAccount(user.getAccount());
        userDTO.setUser_id(user.getUserId());
        userDTO.setNickname(user.getNickname());
        userDTO.setAvatar(user.getAvatar());
        String userJson=JSON.toJSONString(userDTO);
        redisUtil.set(key,userJson, AuthConstant.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        UserVO userVO=converUserToVO(user,token);
        return userVO;
    }
    //退出登录
    @Override
    public void logout(LoginDTO dto){
        String token=dto.getToken();
        String key = AuthConstant.LOGIN_TOKEN_PREFIX+token;
        redisUtil.delete(key);
    }





}
