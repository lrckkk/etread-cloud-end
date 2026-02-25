package com.etread.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;
//the same as database
@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long user_id;

    private String password;

    private String nickname;

    private String avatar;

    private Date createtime;

    private String account;


}
