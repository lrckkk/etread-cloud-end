package com.etread.parser.factory;

import com.etread.parser.BookParser;
import com.etread.parser.impl.EpubBookParser;
import com.etread.parser.impl.TxtBookParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 简单工厂模式，根据文件名后缀返回对应的解析器
 */
@Component
public class ParserFactory {

    @Autowired private TxtBookParser txtBookParser;
    @Autowired private EpubBookParser epubBookParser;

    public BookParser getParser(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".txt")) {
            return txtBookParser;
        } else if (lower.endsWith(".epub")) {
            return epubBookParser;
        } else {
            throw new IllegalArgumentException("不支持的文件格式: " + filename);
        }
    }
}
