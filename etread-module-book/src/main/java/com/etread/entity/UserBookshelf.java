package com.etread.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("user_bookshelf")
public class UserBookshelf {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private Long bookId;
    
    private Long currentChapterId;
    private Float readPercentage;
    private Date lastReadTime;
    
    private Integer isTop; // 0-否, 1-置顶
    
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
