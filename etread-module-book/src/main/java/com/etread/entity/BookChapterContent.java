package com.etread.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("book_chapter_content")
public class BookChapterContent {
    @TableId // 与 book_chapter.id 一对一，非自增
    private Long chapterId;
    
    private String content;
}
