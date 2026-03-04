package com.etread.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

/**
 * HTML 内容处理工具
 * 职责：纯粹的文本处理，不涉及数据库
 */
@Component
public class HtmlContentUtil {

    /**
     * 给 HTML 中的每个 p 标签注入内容 MD5 ID
     */
    public String injectMd5ToHtml(String html) {
        if (html == null || html.isEmpty()) return "";
        Document doc = Jsoup.parseBodyFragment(html);
        Elements ps = doc.select("p");
        for (Element p : ps) {
            String text = p.text().trim();
            if (!text.isEmpty()) {
                p.attr("id", DigestUtils.md5DigestAsHex(text.getBytes()));
            }
        }
        return doc.body().html();
    }
}