package com.etread.service;

import com.etread.dto.ProgressDTO;
import com.etread.vo.ProgressVO;

import java.util.List;

public interface ProgressSyncService {
    ProgressVO bufferProgress(Long userId, Long bookId, Long currentChapterId, Float readPercentage);
    ProgressVO getProgress(Long userId, Long bookId);
    List<ProgressVO> getAllProgress(Long userId);
    void flushProgressToDB();
}
