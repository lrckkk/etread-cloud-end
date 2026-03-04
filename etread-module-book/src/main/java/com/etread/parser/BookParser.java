package com.etread.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import nl.siegmann.epublib.domain.Book;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

public interface BookParser {
    // 快速扫描目录结构
    List<ChapterMetadata> parseChapterList(MultipartFile file);
    // 解析具体章节内容 (HTML格式)
    String parseContent(MultipartFile file, ChapterMetadata metadata, Long bookId);
    String parseContent(Book book, ChapterMetadata metadata, Long bookId);
}

