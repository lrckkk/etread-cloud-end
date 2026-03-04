package com.etread.service;

import com.etread.dto.BookInfoDTO;

import java.io.File;

/**
 * 书籍解析业务接口
 * 职责：定义并发解析的入口标准
 */
public interface BookParseService {

    /**
     * 启动并发解析任务
     * @param file     书籍文件 (临时文件)
     * @param bookId   书籍ID
     * @param filename 文件名 (用于判断是 epub 还是 txt)
     */
    void parseBookConcurrently(BookInfoDTO BookInfoDTO);
}