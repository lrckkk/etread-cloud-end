package com.etread.parser;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.util.List;

public interface BookParser {
    // 快速扫描目录结构
    List<ChapterMetadata> parseChapterList(File file);
    // 解析具体章节内容 (HTML格式)
    String parseContent(File file, ChapterMetadata metadata, Long bookId);
}

