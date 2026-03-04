package com.etread.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * 书籍上传 DTO
 */
@Data
public class BookUploadDTO {
    private MultipartFile file;    // 上传的原始文件 (TXT/EPUB)
    private String title;          // 书名
    private String author;         // 作者 (可选)
    private List<String> tags;     // 标签列表，如 ["玄幻", "热血"]
    private MultipartFile cover;
    private String description;    // 简介
    private String status;         //状态
}
