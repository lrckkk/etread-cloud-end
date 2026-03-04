package com.etread.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("book_tag_relation")
public class BookTagRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long bookId;
    private Long tagId;
}
