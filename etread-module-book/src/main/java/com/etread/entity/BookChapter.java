package com.etread.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("book_chapter")
public class BookChapter {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long bookId;
    private String chapterTitle;
    private Integer sortOrder;
    private Integer wordCount;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
