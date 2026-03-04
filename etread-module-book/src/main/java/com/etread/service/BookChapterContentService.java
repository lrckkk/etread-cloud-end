package com.etread.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.etread.entity.BookChapterContent;

public interface BookChapterContentService extends IService<BookChapterContent> {
    boolean saveBookChapterContent(BookChapterContent bookChapterContent);
}
