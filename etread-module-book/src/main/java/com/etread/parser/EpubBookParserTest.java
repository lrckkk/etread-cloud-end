package com.etread.parser;

import com.etread.parser.impl.EpubBookParser;
import com.etread.utils.MinioUtil;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * 这是一个临时的启动类，用于在 Spring 环境中运行测试
 * 使用 @SpringBootApplication 启动容器，从而自动读取 application.yml 中的配置
 * 排除数据库和Redis自动配置，只专注于测试解析器和 MinIO
 */
@SpringBootApplication(
        // 只扫描 parser 包，避免扫描到 GlobalWebConfig 或 RedisUtil 等其他组件
        scanBasePackages = "com.etread.parser",
        exclude = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                RedisAutoConfiguration.class
        }
)
// 显式导入 MinioUtil，因为它不在 parser 包下，且我们不想扫描整个 utils 包 (避免引入 RedisUtil)
@Import(MinioUtil.class)
public class EpubBookParserTest {

    public static void main(String[] args) {
        System.out.println("=== 启动 Spring 环境进行 EPUB 解析测试 ===");
        
        // 1. 启动 Spring 容器 (会自动读取 application.yml 并注入 MinioUtil)
        try (ConfigurableApplicationContext context = SpringApplication.run(EpubBookParserTest.class, args)) {
            
            // 2. 从容器中获取自动装配好的 Parser
            EpubBookParser parser = context.getBean(EpubBookParser.class);
            
            // 3. 执行测试逻辑
            runTest(parser);
            
        } catch (Exception e) {
            System.err.println("❌ 测试启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runTest(EpubBookParser parser) {
        // 设置测试文件路径
        File file = new File("C:\\Users\\runcheng tianxia\\Desktop\\小说\\安可小说（1-11卷）\\[橘公司].约会大作战.安可短篇集.01（台）.epub"); // 请替换为您真实的 epub 文件路径
        if (!file.exists()) {
            System.err.println("❌ 测试文件不存在: " + file.getAbsolutePath());
            return;
        }

        System.out.println("\n=== 1. 测试目录解析 ===");
        List<ChapterMetadata> chapters = parser.parseChapterList(file);
        System.out.println("解析到章节数: " + chapters.size());

        // 打印前 5 章目录
        for (int i = 0; i < Math.min(19, chapters.size()); i++) {
            System.out.println(" - " + chapters.get(i).getTitle() + " (" + chapters.get(i).getHref() + ")");
        }
        Book epubBook=null;
        System.out.println("\n=== 2. 测试内容解析 (前6章) ===");
        try (final FileInputStream fis = new FileInputStream(file)){
            epubBook=new EpubReader().readEpub(fis);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < Math.min(19, chapters.size()); i++) {
            ChapterMetadata meta = chapters.get(i);
            System.out.println("\n--- 解析章节: " + meta.getTitle() + " ---");
            try {
                // 传入虚拟 bookId，MinIO 中会生成路径 books/1001/...
                String content = parser.parseContent(epubBook, meta, 1001L);

                // 打印前 900 个字符预览
                System.out.println("内容预览:\n" + (content.length() > 10000 ? content.substring(0, 900) + "..." : content));

                // 检查图片标签
                if (content.contains("<img src=\"http")) {
                    System.out.println("✅ 检测到图片标签，且包含完整 HTTP 链接!");
                } else if (content.contains("<img")) {
                    System.out.println("⚠️ 检测到图片标签，但链接格式可能不对: " + content.substring(content.indexOf("<img"), Math.min(content.length(), content.indexOf("<img") + 50)));
                }

            } catch (Exception e) {
                System.err.println("❌ 解析失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
