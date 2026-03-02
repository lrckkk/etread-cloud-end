package com.etread.parser;

import com.etread.parser.impl.TxtBookParser;
import java.io.File;
import java.util.List;

public class TxtBookParserTest {
    public static void main(String[] args) {
        TxtBookParser parser = new TxtBookParser();
        File file = new File("C:\\Users\\runcheng tianxia\\Desktop\\小说\\红楼梦-曹雪芹.txt");

        if (!file.exists()) {
            System.out.println("文件不存在");
            return;
        }

        System.out.println("=== 开始测试目录解析 ===");
        List<ChapterMetadata> chapters = parser.parseChapterList(file);
        System.out.println("解析到章节数: " + chapters.size());

        // 循环输出前 3 章的完整内容
        int limit = Math.min(3, chapters.size());
        for (int i = 0; i < limit; i++) {
            ChapterMetadata meta = chapters.get(i);
            System.out.println("\n==================================================");
            System.out.println("章节 [" + (i + 1) + "]: " + meta.getTitle());
            System.out.println("偏移量: " + meta.getStartOffset() + " - " + meta.getEndOffset());
            System.out.println("==================================================");
            
            // 解析并打印完整内容
            String content = parser.parseContent(file, meta, 1L);
            System.out.println(content); 
        }
    }
}
