package com.etread.dto;

import lombok.Data;
import java.util.Date;

/**
 * 章节同步请求 DTO
 * 接收客户端上传的本地章节状态
 */
@Data
public class ChapterSyncDTO {
    private Long chapterId;       // 客户端已有的章节ID
    private Date localUpdateTime; // 客户端该章节的最后更新时间
}
