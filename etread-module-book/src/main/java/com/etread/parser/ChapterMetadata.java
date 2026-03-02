package com.etread.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChapterMetadata {
    private String title;
    private long startOffset; // TXT专用
    private long endOffset;   // TXT专用
    private String href;      // EPUB专用：主文件路径
    
    // 新增：子文件路径列表 (用于合并连续的插图页或分段章节)
    private List<String> subHrefs = new ArrayList<>();

    public ChapterMetadata(String title, long startOffset, long endOffset, String href) {
        this.title = title;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.href = href;
        this.subHrefs = new ArrayList<>();
    }

    public void addSubHref(String subHref) {
        if (this.subHrefs == null) {
            this.subHrefs = new ArrayList<>();
        }
        this.subHrefs.add(subHref);
    }
}
