package com.etread.parser.impl;

import com.etread.parser.BookParser;
import com.etread.parser.ChapterMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TxtBookParser implements BookParser {

    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "^\\s*第[一二三四五六七八九十百千万0-9]+[章回节卷集部篇].*$"
    );

    @Override
    public List<ChapterMetadata> parseChapterList(File file) {
        List<ChapterMetadata> list = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = raf.length();
            long pos = 0;
            String line;
            long lastPos = 0;
            String lastTitle = null;
            boolean isFirstLine = true;

            while ((line = raf.readLine()) != null) {
                String title = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);

                // 去除 BOM 头
                if (isFirstLine) {
                    title = title.replace("\uFEFF", "");
                    isFirstLine = false;
                }
                title = title.trim();

                if (CHAPTER_PATTERN.matcher(title).matches()) {
                    if (lastTitle != null) {
                        list.add(new ChapterMetadata(lastTitle, lastPos, pos, null));
                    } else if (pos > 50) {
                        // 只有当开头真的有一大段非标题内容时，才叫前言
                        list.add(new ChapterMetadata("前言", 0, pos, null));
                    }

                    lastTitle = title;
                    lastPos = pos;
                }
                pos = raf.getFilePointer();
            }

            if (lastTitle != null) {
                list.add(new ChapterMetadata(lastTitle, lastPos, length, null));
            } else {
                list.add(new ChapterMetadata(file.getName().replace(".txt", ""), 0, length, null));
            }

        } catch (IOException e) {
            throw new RuntimeException("TXT解析失败", e);
        }
        return list;
    }

    @Override
    public String parseContent(File file, ChapterMetadata meta, Long bookId) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(meta.getStartOffset());
            int len = (int) (meta.getEndOffset() - meta.getStartOffset());
            byte[] buffer = new byte[len];
            raf.readFully(buffer);
            String rawText = new String(buffer, StandardCharsets.UTF_8);

            return Arrays.stream(rawText.split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(s -> !s.matches("^[\\-=*#]+$"))
                    .filter(s -> !s.equals(meta.getTitle()))
                    .map(s -> "<p>" + s + "</p>")
                    .collect(Collectors.joining(""));
        } catch (IOException e) {
            throw new RuntimeException("读取TXT内容失败", e);
        }
    }
}