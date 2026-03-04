package com.etread.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.etread.entity.BookChapter;

import java.util.List;

public interface BookChapterService extends IService<BookChapter> {
   String addBookChapter(List<BookChapter> bookChapters);
}
