package com.etread.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@Data
public class BookInfoDTO {
    private Long bookid;
    private MultipartFile file;// 上传的原始文件 (TXT/EPUB)
    private String filename;
    private String title;          // 书名
    private String author;         // 作者 (可选)
    private List<String> tags;     // 标签列表，如 ["玄幻", "热血"]
    private MultipartFile cover;
    private String description;    // 简介
    private int status;         //状态
    private String error_msg;      //错误信息
}
