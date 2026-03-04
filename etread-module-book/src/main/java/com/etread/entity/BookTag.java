package com.etread.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("book_tag")
public class BookTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String tagName;
}
