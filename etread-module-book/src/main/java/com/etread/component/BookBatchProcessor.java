package com.etread.component; // 建议放在 component 包下

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.etread.entity.BookChapter;
import com.etread.entity.BookChapterContent;
import com.etread.service.IBookChapterContentService;
import com.etread.service.IBookChapterService;
import com.etread.parser.BookParser;
import com.etread.parser.ChapterMetadata;
import com.etread.parser.impl.EpubBookParser;
import com.etread.utils.ChapterIdGenerator;
import com.etread.utils.HtmlContentUtil;
import nl.siegmann.epublib.domain.Book;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 书籍批次处理器
 * 职责：处理单个批次的 解析 -> 转换 -> 入库
 */
@Component
public class BookBatchProcessor {

    @Autowired private HtmlContentUtil htmlContentUtil;
    @Autowired private ChapterIdGenerator chapterIdGenerator;
    @Autowired private IBookChapterService bookChapterService;
    @Autowired private IBookChapterContentService bookChapterContentService;

    public void processBatch(MultipartFile file, List<ChapterMetadata> batch, Long bookId,
                             BookParser parser, Book epubBook, int startSortOrder, int step) {

        List<BookChapter> chapters = new ArrayList<>();
        List<BookChapterContent> contents = new ArrayList<>();
        int currentSort = startSortOrder;

        for (ChapterMetadata meta : batch) {
            String rawHtml;
            // 1. 调用解析器 (保留类型判断逻辑)
            if (epubBook != null && parser instanceof EpubBookParser) {
                rawHtml = ((EpubBookParser) parser).parseContent(epubBook, meta, bookId);
            } else {
                rawHtml = parser.parseContent(file, meta, bookId);
            }

            // 2. 调用工具类注入 MD5
            String finalHtml = htmlContentUtil.injectMd5ToHtml(rawHtml);

            // 3. 组装实体
            long chapterId = chapterIdGenerator.generate(bookId,currentSort);

            BookChapter chapter = new BookChapter();
            chapter.setId(chapterId);
            chapter.setBookId(bookId);
            chapter.setChapterTitle(meta.getTitle());
            chapter.setSortOrder(currentSort);
            chapter.setWordCount(Jsoup.parse(finalHtml).text().length());
            chapters.add(chapter);

            BookChapterContent content = new BookChapterContent();
            content.setChapterId(chapterId);
            content.setContent(finalHtml);
            contents.add(content);

            currentSort += step;
        }

        // 4. 批量入库
        if (!chapters.isEmpty()) {
            bookChapterService.saveBatch(chapters);
            bookChapterContentService.saveBatch(contents);
        }
    }
}