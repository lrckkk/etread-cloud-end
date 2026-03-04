package com.etread.parser.impl;

import com.etread.parser.BookParser;
import com.etread.parser.ChapterMetadata;
import com.etread.utils.MinioUtil;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class EpubBookParser implements BookParser {

    @Autowired private MinioUtil minioUtil;

    @Override
    public List<ChapterMetadata> parseChapterList(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            EpubReader reader = new EpubReader();
            Book book = reader.readEpub(fis);
            List<ChapterMetadata> list = new ArrayList<>();

            Map<String, String> tocMap = new HashMap<>();
            List<TOCReference> toc = book.getTableOfContents().getTocReferences();
            if (toc != null) {
                buildTocMapRecursive(toc, tocMap);
            }

            List<SpineReference> spineRefs = book.getSpine().getSpineReferences();
            for (SpineReference ref : spineRefs) {
                Resource res = ref.getResource();
                String href = removeAnchor(res.getHref());
                boolean isNewChapter = tocMap.containsKey(href) || list.isEmpty();

                if (isNewChapter) {
                    String title = tocMap.get(href);
                    if (title == null || title.isEmpty()) title = extractTitleFromHtml(res);
                    if (title == null || title.isEmpty()) title = "章节-" + (list.size() + 1);
                    list.add(new ChapterMetadata(title, 0, 0, href));
                } else {
                    ChapterMetadata lastChapter = list.get(list.size() - 1);
                    lastChapter.addSubHref(href);
                }
            }
            return list;
        } catch (IOException e) {
            throw new RuntimeException("EPUB目录解析失败", e);
        }
    }

    // 去除 URL 中的锚点 (#xxx)
    private String removeAnchor(String href) {
        int idx = href.indexOf('#');
        return idx > 0 ? href.substring(0, idx) : href;
    }

    private void buildTocMapRecursive(List<TOCReference> refs, Map<String, String> map) {
        for (TOCReference ref : refs) {
            if (ref.getResource() != null) {
                map.put(removeAnchor(ref.getResource().getHref()), ref.getTitle());
            }
            if (ref.getChildren() != null && !ref.getChildren().isEmpty()) {
                buildTocMapRecursive(ref.getChildren(), map);
            }
        }
    }

    private String extractTitleFromHtml(Resource res) {
        try {
            byte[] data = res.getData();
            int len = Math.min(data.length, 2048);
            String head = new String(data, 0, len, res.getInputEncoding());
            
            Matcher mTitle = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE).matcher(head);
            if (mTitle.find()) return mTitle.group(1).trim();
            
            Matcher mH1 = Pattern.compile("<h1.*?>(.*?)</h1>", Pattern.CASE_INSENSITIVE).matcher(head);
            if (mH1.find()) return Jsoup.parse(mH1.group(1)).text().trim();
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    @Override
    public String parseContent(File file, ChapterMetadata meta, Long bookId) {
        try (FileInputStream fis = new FileInputStream(file)) {
            EpubReader reader = new EpubReader();
            Book book = reader.readEpub(fis);
            StringBuilder fullContent = new StringBuilder();

            // 1. 解析主页面
            fullContent.append(parseOneResource(book, meta.getHref(), bookId));

            // 2. 解析子页面 (如果有)
            if (meta.getSubHrefs() != null) {
                for (String subHref : meta.getSubHrefs()) {
                    fullContent.append(parseOneResource(book, subHref, bookId));
                }
            }

            return fullContent.toString();
        } catch (IOException e) {
            throw new RuntimeException("EPUB内容解析失败", e);
        }
    }
    public String parseContent(Book book, ChapterMetadata meta, Long bookId) {
        StringBuilder fullContent = new StringBuilder();

        // 1. 解析主页面
        fullContent.append(parseOneResource(book, meta.getHref(), bookId));

        // 2. 解析子页面 (如果有)
        if (meta.getSubHrefs() != null) {
            for (String subHref : meta.getSubHrefs()) {
                fullContent.append(parseOneResource(book, subHref, bookId));
            }
        }
        return fullContent.toString();
    }

    private String parseOneResource(Book book, String href, Long bookId) {
        try {
            Resource resource = book.getResources().getByHref(href);
            if (resource == null) return "";

            String html = new String(resource.getData(), resource.getInputEncoding());
            Document doc = Jsoup.parse(html);

            StringBuilder sb = new StringBuilder();
            // 传入当前页面的 href 作为 baseHref，确保相对路径解析正确
            traverseDOM(doc.body(), sb, book, bookId, href);
            return sb.toString();
        } catch (Exception e) {
            log.error("解析子页面失败: " + href, e);
            return "";
        }
    }

    private void traverseDOM(Element element, StringBuilder sb, Book book, Long bookId, String baseHref) {
        for (org.jsoup.nodes.Node node : element.childNodes()) {
            if (node instanceof org.jsoup.nodes.TextNode) {
                String text = ((org.jsoup.nodes.TextNode) node).text().trim();
                if (!text.isEmpty()) {
                    sb.append("<p>").append(text).append("</p>");
                }
                continue;
            }

            if (node instanceof Element) {
                Element child = (Element) node;
                String tagName = child.tagName().toLowerCase();

                // 1. 图片 / SVG Image
                if (tagName.equals("img") || tagName.equals("image")) {
                    String src = child.attr("src");
                    if (src.isEmpty()) src = child.attr("xlink:href");
                    if (src.isEmpty()) src = child.attr("href");

                    if (src != null && !src.isEmpty()) {
                        String minioUrl = processImage(src, book, bookId, baseHref);
                        if (minioUrl != null) {
                            sb.append("<img src=\"").append(minioUrl).append("\" />");
                        }
                    }
                }
                // 2. 段落/标题
                else if ((tagName.equals("p") || tagName.matches("h[1-6]")) 
                        && child.select("img").isEmpty() && child.select("image").isEmpty()) {
                    String text = child.text().trim();
                    if (!text.isEmpty()) {
                        sb.append("<p>").append(text).append("</p>");
                    }
                }
                // 3. 其他容器 -> 递归
                else {
                    traverseDOM(child, sb, book, bookId, baseHref);
                }
            }
        }
    }

    private String processImage(String src, Book book, Long bookId, String baseHref) {
        try {
            String imageHref = resolveHref(baseHref, src);
            Resource imgRes = book.getResources().getByHref(imageHref);
            String bucketName="contentpicture";
            if (imgRes != null) {
                String objectName = "books/" + bookId + "/" + imageHref;
                return minioUtil.uploadBytes(imgRes.getData(), objectName, String.valueOf(imgRes.getMediaType()), bucketName);
            }
        } catch (Exception e) {
            log.error("图片处理失败: {}", src, e);
        }
        return null;
    }

    private String resolveHref(String baseHref, String relativeHref) {
        if (relativeHref.startsWith("http")) return relativeHref;
        try {
            int lastSlash = baseHref.lastIndexOf('/');
            String basePath = (lastSlash >= 0) ? baseHref.substring(0, lastSlash + 1) : "";
            URI baseUri = new URI("file:///" + basePath);
            URI resolvedUri = baseUri.resolve(relativeHref);
            String path = resolvedUri.getPath();
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            return relativeHref;
        }
    }
}
