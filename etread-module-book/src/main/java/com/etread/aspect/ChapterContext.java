package com.etread.aspect;

import java.lang.ThreadLocal;

/**
 * 线程上下文：用于存储当前批次的 ChapterID
 */
public class ChapterContext {
    private static final ThreadLocal<Long> CHAPTER_ID_HOLDER = new ThreadLocal<>();

    public static void setChapterId(Long chapterId) {
        CHAPTER_ID_HOLDER.set(chapterId);
    }

    public static Long getChapterId() {
        return CHAPTER_ID_HOLDER.get();
    }

    public static void clear() {
        CHAPTER_ID_HOLDER.remove();
    }
}
