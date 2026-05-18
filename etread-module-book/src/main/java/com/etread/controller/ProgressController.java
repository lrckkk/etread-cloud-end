package com.etread.controller;

import com.etread.Result;
import com.etread.component.BookUserResolver;
import com.etread.dto.ProgressReq;
import com.etread.service.ProgressSyncService;
import com.etread.vo.ProgressVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/book")
@Validated
public class ProgressController {

    @Autowired
    private ProgressSyncService progressSyncService;

    @Autowired
    private BookUserResolver bookUserResolver;

    @PutMapping("/progress/sync")
    public Result<ProgressVO> syncProgress(@RequestHeader("token") String token,
                                           @Valid ProgressReq req) {
        Long userId = bookUserResolver.requireUserId(token);

        try {
            ProgressVO progressVO = progressSyncService.bufferProgress(
                    userId,
                    req.getBookId(),
                    req.getCurrentChapterId(),
                    req.getReadPercentage()
            );

            log.info("progress synced: userId={}, bookId={}, chapter={}, percentage={}",
                    userId, req.getBookId(), req.getCurrentChapterId(), req.getReadPercentage());

            return Result.success("同步成功", progressVO);
        } catch (Exception e) {
            log.error("error syncing progress", e);
            return Result.error("同步失败: " + e.getMessage());
        }
    }

    @GetMapping("/progress/my")
    public Result<ProgressVO> getMyProgress(@RequestHeader("token") String token,
                                           @RequestParam(required = true) Long bookId) {
        Long userId = bookUserResolver.requireUserId(token);

        try {
            ProgressVO progressVO = progressSyncService.getProgress(userId, bookId);
            return Result.success("查询成功", progressVO);
        } catch (Exception e) {
            log.error("error getting progress", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    // 获取用户所有阅读历史
    @GetMapping("/progress/history")
    public Result<List<ProgressVO>> getProgressHistory(@RequestHeader("token") String token) {
        Long userId = bookUserResolver.requireUserId(token);

        try {
            List<ProgressVO> progressList = progressSyncService.getAllProgress(userId);
            return Result.success("查询成功", progressList);
        } catch (Exception e) {
            log.error("error getting progress history", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
