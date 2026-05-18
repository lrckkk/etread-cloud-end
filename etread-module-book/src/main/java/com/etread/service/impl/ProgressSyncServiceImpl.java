package com.etread.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.etread.entity.UserBookshelf;
import com.etread.mapper.UserBookshelfMapper;
import com.etread.service.BookHotService;
import com.etread.service.ProgressSyncService;
import com.etread.vo.ProgressCacheVO;
import com.etread.vo.ProgressVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ProgressSyncServiceImpl implements ProgressSyncService {

    private static final String PROGRESS_KEY_PREFIX = "user:progress:";
    private static final String PROGRESS_DIRTY_SET_KEY = "user:progress:dirty";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserBookshelfMapper userBookshelfMapper;

    @Autowired
    private BookHotService bookHotService;

    @Override
    public ProgressVO bufferProgress(Long userId, Long bookId, Long currentChapterId, Float readPercentage) {
        if (userId == null || bookId == null || currentChapterId == null || readPercentage == null) {
            throw new IllegalArgumentException("userId, bookId, currentChapterId, readPercentage 不能为空");
        }

        Date now = new Date();
        ProgressCacheVO cacheVO = new ProgressCacheVO();
        cacheVO.setBookId(bookId);
        cacheVO.setCurrentChapterId(currentChapterId);
        cacheVO.setReadPercentage(readPercentage);
        cacheVO.setLastReadTime(now);
        cacheVO.setDirty(true);

        String progressJson = JSON.toJSONString(cacheVO);
        String progressRedisKey = PROGRESS_KEY_PREFIX + userId + ":" + bookId;

        stringRedisTemplate.opsForValue().set(progressRedisKey, progressJson);
        stringRedisTemplate.opsForSet().add(PROGRESS_DIRTY_SET_KEY, userId + ":" + bookId);

        bookHotService.incrementHotScore(bookId, 5.0);

        log.debug("buffered progress: userId={}, bookId={}, chapter={}, percentage={}",
                userId, bookId, currentChapterId, readPercentage);

        ProgressVO vo = new ProgressVO();
        vo.setUserId(userId);
        vo.setBookId(bookId);
        vo.setCurrentChapterId(currentChapterId);
        vo.setReadPercentage(readPercentage);
        vo.setLastReadTime(now);
        return vo;
    }

    @Override
    public ProgressVO getProgress(Long userId, Long bookId) {
        if (userId == null || bookId == null) {
            throw new IllegalArgumentException("userId, bookId 不能为空");
        }

        String progressRedisKey = PROGRESS_KEY_PREFIX + userId + ":" + bookId;
        String progressJson = stringRedisTemplate.opsForValue().get(progressRedisKey);

        if (progressJson != null && !progressJson.isEmpty()) {
            ProgressCacheVO cacheVO = JSON.parseObject(progressJson, ProgressCacheVO.class);
            ProgressVO vo = new ProgressVO();
            vo.setUserId(userId);
            vo.setBookId(bookId);
            vo.setCurrentChapterId(cacheVO.getCurrentChapterId());
            vo.setReadPercentage(cacheVO.getReadPercentage());
            vo.setLastReadTime(cacheVO.getLastReadTime());
            log.debug("read progress from Redis: userId={}, bookId={}", userId, bookId);
            return vo;
        }

        UserBookshelf shelf = userBookshelfMapper.selectOne(
                new LambdaQueryWrapper<UserBookshelf>()
                        .eq(UserBookshelf::getUserId, userId)
                        .eq(UserBookshelf::getBookId, bookId)
        );

        if (shelf != null) {
            String cacheVO = JSON.toJSONString(new ProgressCacheVO(
                    bookId,
                    shelf.getCurrentChapterId(),
                    shelf.getReadPercentage(),
                    shelf.getLastReadTime(),
                    false
            ));
            stringRedisTemplate.opsForValue().set(progressRedisKey, cacheVO);

            ProgressVO vo = new ProgressVO();
            vo.setUserId(userId);
            vo.setBookId(bookId);
            vo.setCurrentChapterId(shelf.getCurrentChapterId());
            vo.setReadPercentage(shelf.getReadPercentage());
            vo.setLastReadTime(shelf.getLastReadTime());
            log.debug("read progress from MySQL and cached to Redis: userId={}, bookId={}", userId, bookId);
            return vo;
        }

        return new ProgressVO(userId, bookId, null, 0F, null);
    }

    @Override
    public List<ProgressVO> getAllProgress(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }

        List<ProgressVO> result = new ArrayList<>();

        // 先从 Redis 获取用户的所有进度
        Set<String> keys = stringRedisTemplate.keys(PROGRESS_KEY_PREFIX + userId + ":*");
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                String progressJson = stringRedisTemplate.opsForValue().get(key);
                if (progressJson != null && !progressJson.isEmpty()) {
                    ProgressCacheVO cacheVO = JSON.parseObject(progressJson, ProgressCacheVO.class);
                    ProgressVO vo = new ProgressVO();
                    vo.setUserId(userId);
                    vo.setBookId(cacheVO.getBookId());
                    vo.setCurrentChapterId(cacheVO.getCurrentChapterId());
                    vo.setReadPercentage(cacheVO.getReadPercentage());
                    vo.setLastReadTime(cacheVO.getLastReadTime());
                    result.add(vo);
                }
            }
        }

        // 从 MySQL 获取用户书架中的书籍进度
        List<UserBookshelf> shelves = userBookshelfMapper.selectList(
                new LambdaQueryWrapper<UserBookshelf>()
                        .eq(UserBookshelf::getUserId, userId)
                        .isNotNull(UserBookshelf::getLastReadTime)
        );

        for (UserBookshelf shelf : shelves) {
            final Long bookId = shelf.getBookId();
            boolean exists = result.stream().anyMatch(v -> v.getBookId().equals(bookId));
            if (!exists) {
                ProgressVO vo = new ProgressVO();
                vo.setUserId(userId);
                vo.setBookId(bookId);
                vo.setCurrentChapterId(shelf.getCurrentChapterId());
                vo.setReadPercentage(shelf.getReadPercentage());
                vo.setLastReadTime(shelf.getLastReadTime());
                result.add(vo);
            }
        }

        // 按最后阅读时间倒序
        result.sort((a, b) -> {
            if (a.getLastReadTime() == null && b.getLastReadTime() == null) return 0;
            if (a.getLastReadTime() == null) return 1;
            if (b.getLastReadTime() == null) return -1;
            return b.getLastReadTime().compareTo(a.getLastReadTime());
        });

        log.debug("getAllProgress: userId={}, count={}", userId, result.size());
        return result;
    }

    @Override
    public void flushProgressToDB() {
        Set<String> dirtyRecords = stringRedisTemplate.opsForSet().members(PROGRESS_DIRTY_SET_KEY);

        if (dirtyRecords == null || dirtyRecords.isEmpty()) {
            log.debug("no dirty progress records to flush");
            return;
        }

        int flushedCount = 0;
        for (String dirtyRecord : dirtyRecords) {
            if (dirtyRecord == null || dirtyRecord.isEmpty()) {
                continue;
            }

            String[] parts = dirtyRecord.split(":");
            if (parts.length != 2) {
                log.warn("invalid dirty record format: {}", dirtyRecord);
                continue;
            }

            try {
                Long userId = Long.valueOf(parts[0]);
                Long bookId = Long.valueOf(parts[1]);
                String progressRedisKey = PROGRESS_KEY_PREFIX + userId + ":" + bookId;
                String progressJson = stringRedisTemplate.opsForValue().get(progressRedisKey);

                if (progressJson == null || progressJson.isEmpty()) {
                    log.warn("progress cache not found for {}", dirtyRecord);
                    stringRedisTemplate.opsForSet().remove(PROGRESS_DIRTY_SET_KEY, dirtyRecord);
                    continue;
                }

                ProgressCacheVO cacheVO = JSON.parseObject(progressJson, ProgressCacheVO.class);

                UserBookshelf existingShelf = userBookshelfMapper.selectOne(
                        new LambdaQueryWrapper<UserBookshelf>()
                                .eq(UserBookshelf::getUserId, userId)
                                .eq(UserBookshelf::getBookId, bookId)
                );

                if (existingShelf != null) {
                    userBookshelfMapper.update(null,
                            new LambdaUpdateWrapper<UserBookshelf>()
                                    .eq(UserBookshelf::getUserId, userId)
                                    .eq(UserBookshelf::getBookId, bookId)
                                    .set(UserBookshelf::getCurrentChapterId, cacheVO.getCurrentChapterId())
                                    .set(UserBookshelf::getReadPercentage, cacheVO.getReadPercentage())
                                    .set(UserBookshelf::getLastReadTime, cacheVO.getLastReadTime())
                    );
                } else {
                    UserBookshelf newShelf = new UserBookshelf();
                    newShelf.setUserId(userId);
                    newShelf.setBookId(bookId);
                    newShelf.setCurrentChapterId(cacheVO.getCurrentChapterId());
                    newShelf.setReadPercentage(cacheVO.getReadPercentage());
                    newShelf.setLastReadTime(cacheVO.getLastReadTime());
                    newShelf.setIsTop(0);
                    userBookshelfMapper.insert(newShelf);
                }

                stringRedisTemplate.delete(progressRedisKey);
                stringRedisTemplate.opsForSet().remove(PROGRESS_DIRTY_SET_KEY, dirtyRecord);
                flushedCount++;

                log.debug("flushed progress: userId={}, bookId={}, chapter={}", userId, bookId, cacheVO.getCurrentChapterId());
            } catch (Exception e) {
                log.error("error flushing progress record: {}", dirtyRecord, e);
            }
        }

        log.info("progress flush completed: {} records flushed", flushedCount);
    }
}
