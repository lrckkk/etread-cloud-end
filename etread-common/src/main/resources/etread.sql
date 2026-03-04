/*
 Navicat Premium Data Transfer

 Source Server         : Penstore
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : etread

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 02/03/2026 14:41:28
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for book_chapter
-- ----------------------------
DROP TABLE IF EXISTS `book_chapter`;
CREATE TABLE `book_chapter`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `book_id` bigint NOT NULL,
  `chapter_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '稀疏排序(100,200...)',
  `word_count` int NULL DEFAULT 0,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '增量同步锚点',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_book_sort`(`book_id` ASC, `sort_order` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '章节目录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of book_chapter
-- ----------------------------

-- ----------------------------
-- Table structure for book_chapter_content
-- ----------------------------
DROP TABLE IF EXISTS `book_chapter_content`;
CREATE TABLE `book_chapter_content`  (
  `chapter_id` bigint NOT NULL COMMENT '与book_chapter.id 1:1',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '清洗后的HTML，段落带hash-id',
  PRIMARY KEY (`chapter_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '章节正文' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of book_chapter_content
-- ----------------------------

-- ----------------------------
-- Table structure for book_info
-- ----------------------------
DROP TABLE IF EXISTS `book_info`;
CREATE TABLE `book_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '书名',
  `author` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '作者',
  `cover_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面图',
  `original_file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '原始文件(MinIO)',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '简介',
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标签冗余字段(逗号分隔)，用于列表页直出',
  `status` tinyint NULL DEFAULT 0 COMMENT '0-解析中, 1-上架, 2-失败, 3-下架',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '解析失败堆栈信息',
  `word_count` int NULL DEFAULT 0,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '书籍元数据' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of book_info
-- ----------------------------

-- ----------------------------
-- Table structure for book_paragraph_comment
-- ----------------------------
DROP TABLE IF EXISTS `book_paragraph_comment`;
CREATE TABLE `book_paragraph_comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `book_id` bigint NOT NULL,
  `chapter_id` bigint NOT NULL,
  `paragraph_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '段落内容MD5',
  `user_id` bigint NOT NULL,
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `like_count` int NULL DEFAULT 0,
  `parent_id` bigint NULL DEFAULT 0 COMMENT '楼中楼ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_chapter_para`(`chapter_id` ASC, `paragraph_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of book_paragraph_comment
-- ----------------------------

-- ----------------------------
-- Table structure for book_tag
-- ----------------------------
DROP TABLE IF EXISTS `book_tag`;
CREATE TABLE `book_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tag_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tag`(`tag_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of book_tag
-- ----------------------------

-- ----------------------------
-- Table structure for book_tag_relation
-- ----------------------------
DROP TABLE IF EXISTS `book_tag_relation`;
CREATE TABLE `book_tag_relation`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `book_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_book_tag`(`book_id` ASC, `tag_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of book_tag_relation
-- ----------------------------

-- ----------------------------
-- Table structure for comment_like_record
-- ----------------------------
DROP TABLE IF EXISTS `comment_like_record`;
CREATE TABLE `comment_like_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `comment_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_cmt`(`user_id` ASC, `comment_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of comment_like_record
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `createtime` datetime NULL DEFAULT NULL,
  `account` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`user_id` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (25, '$2a$10$68HBEDZDZA6iS.cncadebuMEl.ofHmqVerzWBQeo0yO/0TasKneHS', '刘', 'http://localhost:9000/avatars/b6cadf6b10c043ea8565298f8f252581-屏幕截图 2026-02-25 201509.png', '2026-03-01 17:31:46', '123');
INSERT INTO `sys_user` VALUES (24, '$2a$10$1Wl1/nvQ7uKSyKSxbdX25.8yMIrGi7Mq6ZKv7iPaUeOdW8J2s38ym', 'bu', 'http://localhost:9000/avatars/f6de8352e3bd434580a2245f1b8fc74e-ENDFIELD_SHARE_1769510992.png', '2026-02-26 22:19:39', 'jj');

-- ----------------------------
-- Table structure for user_read_progress
-- ----------------------------
DROP TABLE IF EXISTS `user_read_progress`;
CREATE TABLE `user_read_progress`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `book_id` bigint NOT NULL,
  `current_chapter_id` bigint NOT NULL COMMENT '当前阅读章节',
  `read_percentage` float NULL DEFAULT 0 COMMENT '阅读百分比',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_book`(`user_id` ASC, `book_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '阅读进度' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_read_progress
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
