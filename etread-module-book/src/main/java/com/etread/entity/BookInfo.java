package com.etread.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("book_info")
public class BookInfo {
    private Long id;
    
    private String title;
    private String author;
    private String coverUrl;
    private String originalFileUrl;
    private String description;
    
    // 冗余字段，逗号分隔的标签，用于快速展示
    private String tags; 
    
    // 0-解析中, 1-上架, 2-失败, 3-下架
    private Integer status;
    private String errorMsg;
    private Integer wordCount;
    
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
