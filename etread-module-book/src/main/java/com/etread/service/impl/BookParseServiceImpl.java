package com.etread.service.impl;

import com.etread.component.BookBatchProcessor;
import com.etread.dto.BookInfoDTO;
import com.etread.service.BookInfoService;
import com.etread.service.BookParseService;
import com.etread.parser.BookParser;
import com.etread.parser.ChapterMetadata;
import com.etread.parser.factory.ParserFactory;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
/**
 * 实现注册书籍信息
 * 并发传入书的章节
 */
public class BookParseServiceImpl implements BookParseService {

    @Autowired
    @Qualifier("bookParseExecutor")
    private Executor executor;

    @Autowired private ParserFactory parserFactory;
    @Autowired private BookBatchProcessor batchProcessor; // 注入咱们的苦力工头
    @Autowired private BookInfoServiceImpl bookInfoService;  // 注入书籍信息Service用来更新状态
    @Autowired private MultipartFile multipartFile;

    @Override
    public void parseBookConcurrently(BookInfoDTO bookInfoDTO) {
        String filename = bookInfoDTO.getFilename();
        MultipartFile file = bookInfoDTO.getFile();
        Long bookId =bookInfoDTO.getBookid();

        // 1. 准备工作
        BookParser parser = parserFactory.getParser(filename);
        List<ChapterMetadata> chapterMetadatas = parser.parseChapterList(file);

        Book epubBook = null;
        if(filename.toLowerCase().endsWith(".epub")) {
            try (InputStream inputStream = multipartFile.getInputStream()) {

                // EpubReader 很聪明，它能直接读 InputStream
                epubBook = new EpubReader().readEpub(inputStream);
            } catch (IOException e) {
                BookInfoDTO error = new BookInfoDTO();
                error.setBookid(bookId);
                error.setStatus(2);
                error.setError_msg("epub解析失败");
                bookInfoService.updateStatus(error);
                throw new RuntimeException(e);
            }
        }

        // 2. 规划任务
        int batchSize = 50;
        int step = 100;
        List<List<ChapterMetadata>> batches = Lists.partition(chapterMetadatas, batchSize);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 3. 派发并发任务
        for (int i = 0; i < batches.size(); i++) {
            List<ChapterMetadata> batch = batches.get(i);
            int startSortOrder = 100 + (i * batchSize * step);
            final Book finalEpubBook = epubBook;

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    //  核心逻辑全部委托给 batchProcessor
                    batchProcessor.processBatch(file, batch, bookId, parser, finalEpubBook, startSortOrder, step);
                } catch (Exception e) {
                    log.error("批次处理失败", e);
                }
            }, executor);

            futures.add(future);
        }

        // 4. 等待结果并收尾
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            BookInfoDTO success = new BookInfoDTO();
            success.setBookid(bookId);
            success.setStatus(1);
            success.setError_msg(null);
            bookInfoService.updateStatus(success); // 失败
        } catch (Exception e) {
            log.error("解析任务整体失败", e);
            BookInfoDTO error = new BookInfoDTO();
            error.setBookid(bookId);
            error.setStatus(2);
            error.setError_msg(e.getMessage());
            bookInfoService.updateStatus(error);
            bookInfoService.updateStatus(error); // 失败
            throw new RuntimeException("服务器出现错误，请稍后再试");
        }
    }
}