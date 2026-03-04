package com.etread.utils;

public class ChapterIdGenerator {

    /**
     * 生成 ChapterID
     * 规则：BookID前6位 + (currentSort / 100)的后4位
     * @param bookId 书籍ID (Long)
     * @param currentSort 当前排序号 (int)，如 100, 200, 300...
     * @return 生成的 10 位左右的 Long ID
     */
    public static Long generate(Long bookId, int currentSort) {
        if (bookId == null) {
            throw new IllegalArgumentException("BookID cannot be null");
        }

        // 1. 处理前缀：BookID 前 6 位
        String bookIdStr = String.valueOf(bookId);
        String prefix;
        if (bookIdStr.length() >= 6) {
            prefix = bookIdStr.substring(0, 6);
        } else {
            // 不足6位，右侧补0 (例如 1001 -> 100100)
            prefix = String.format("%-6s", bookIdStr).replace(' ', '0');
        }

        // 2. 处理后缀：(currentSort / 100) 格式化为 4 位数字
        // 假设 currentSort=100 -> batchNum=1 -> 0001
        // 假设 currentSort=5000 -> batchNum=50 -> 0050
        int batchNum = currentSort / 100;
        String suffix = String.format("%04d", batchNum);

        // 3. 拼接并返回
        return Long.parseLong(prefix + suffix);
    }
}
