package com.etread.dto;

import com.etread.entity.BookChapter;
import com.etread.entity.BookInfo;
import lombok.Data;
import java.util.List;

/**
 * 书籍同步结果 DTO
 * 返回给客户端：书籍元数据 + 完整目录 + 需要更新的章节ID列表
 */
@Data
public class ChapterSyncResult {
    private BookInfo bookInfo;             // 书籍简介等
    private List<BookChapter> allChapters; // 完整目录（轻量级，不含正文）
    private List<Long> updatedChapterIds;  // 需要拉取正文的章节ID列表
}
