# ETRead 后端（多模块）说明文档

本仓库是 ETRead 在线阅读与书城平台的后端实现，按业务拆分为多个 Spring Boot 模块：用户、书籍/书城、段评评论、公共组件。本文档用于后续学习解读、联调排查与面试答辩复盘。

---

## 1. 模块与端口

- `etread-module-user`（用户与登录）
    - 端口：`8081`
    - 主要能力：注册 / 登录 / 登出，签发 token

- `etread-module-book`（书城、在线阅读、上传解析、书评、书架）
    - 端口：`8082`
    - 主要能力：
        - 书籍上传 → MinIO → MySQL 落库 → 并发解析章节/内容
        - 书籍搜索（书名/作者/评分/标签/字数等）
        - 章节目录/内容接口（含 Redis 缓存 + 读后预热）
        - 书评（评分聚合：total_score/rating_count）
        - 云端书架：加入/移除/查询

- `etread-module-comment`（段评/评论/点赞）
    - 端口：`8083`
    - 主要能力：段评发布/查询、点赞/取消点赞、热评聚合（部分功能使用 Redis）

- `etread-common`（公共代码）
    - `Result` 统一返回
    - `RedisUtil/MinioUtil/JwtUtil`
    - 登录拦截器与全局异常处理等

---

## 2. 运行依赖（本地开发）

### 2.1 必需依赖
- MySQL：数据库 `etread`
- Redis：缓存与登录 token 存储
- MinIO：对象存储（原书文件、封面、章节资源）

项目提供了 `docker-compose.yml` 用于启动 Redis + MinIO：
- 文件：[docker-compose.yml](file:///D:/etread-cloud-end333/docker-compose.yml)

MinIO 默认：
- Endpoint: `http://localhost:9000`
- Console: `http://localhost:9001`
- AccessKey/SecretKey: `minioadmin/minioadmin`

### 2.2 数据库初始化
- SQL：`etread-common/src/main/resources/etread.sql`
    - 路径：[etread.sql](file:///D:/etread-cloud-end333/etread-common/src/main/resources/etread.sql)

说明：
- 你们的“云端书架”使用表 `user_bookshelf`，如果 SQL 文件里没有该表，请以最新数据库为准（你们已说明数据库已更新）。

---

## 3. 配置文件（重要）

### 3.1 书籍模块配置
- [etread-module-book/application.yml](file:///D:/etread-cloud-end333/etread-module-book/src/main/resources/application.yml)
    - MySQL：`jdbc:mysql://localhost:3306/etread`
    - Redis：`localhost:6379`
    - 上传限制：`max-file-size: 10MB`
    - MinIO：`endpoint/accessKey/secretKey`

### 3.2 评论模块配置
- [etread-module-comment/application.yml](file:///D:/etread-cloud-end333/etread-module-comment/src/main/resources/application.yml)

---

## 4. 认证与登录态（token 机制）

### 4.1 token 在哪里存
- Redis Key 前缀：`login:token:`（见 [AuthConstant](file:///D:/etread-cloud-end333/etread-common/src/main/java/com/etread/constant/AuthConstant.java#L3-L6)）
- TTL：默认 30 分钟（同上）

### 4.2 前端如何传 token
后端接口普遍使用请求头：
- `token: <token-string>`

部分模块也会读 `Authorization`，但以 `token` 头为主。

### 4.3 后端如何从 token 取 userId
书籍模块里存在两种解析方式：
- `Tokencheck.getUserId(token)`：[Tokencheck](file:///D:/etread-cloud-end333/etread-module-book/src/main/java/com/etread/component/Tokencheck.java#L35-L40)
- `BookUserResolver.requireUserId(token)`（更健壮，会校验空 token/过期/缺字段）：[BookUserResolver](file:///D:/etread-cloud-end333/etread-module-book/src/main/java/com/etread/component/BookUserResolver.java#L19-L33)

---

## 5. 书籍上传解析：完整链路（面试高频）

入口：`POST /book/upload`  
控制器：[UploadController](file:///D:/etread-cloud-end333/etread-module-book/src/main/java/com/etread/controller/UploadController.java)

关键流程（逻辑顺序）：
1. 接收 multipart 表单：`file/cover/title/author/tags/description`
    - DTO：[BookUploadDTO](file:///D:/etread-cloud-end333/etread-module-book/src/main/java/com/etread/dto/BookUploadDTO.java)
2. 使用 token 解析上传者（publisher），防止冒名上传
3. MinIO 上传：
    - 原始书文件 → `books` bucket
    - 封面/内容图片 → `contentpicture` bucket
4. `book_info` 落库（保存 title/author/cover_url/original_file_url/status 等）
5. 标签落库：
    - 标签库表：`book_tag`
    - 关系表：`book_tag_relation`
    - 冗余字段：`book_info.tags`（逗号分隔，用于列表直出）
6. 并发解析章节：
    - 解析服务：[BookParseServiceImpl](file:///D:/etread-cloud-end333/etread-module-book/src/main/java/com/etread/service/impl/BookParseServiceImpl.java)
    - 解析完成后：
        - 章节目录入库（`book_chapter`）
        - 章节内容入库（`book_chapter_content`）
        - 统计字数并更新 `book_info.word_count`
        - 成功则更新 `book_info.status=1`，失败 `status=2`

状态字段（面试常问）：
- `status=0`：解析中
- `status=1`：上架（书城可见）
- `status=2`：失败
- `status=3`：下架

---

## 6. 书城搜索（多条件）

入口：`POST /book/info/search?page=1&size=10`  
控制器：[BookInfoController.search](file:///D:/etread-cloud-end333/etread-module-book/src/main/java/com/etread/controller/BookInfoController.java#L42-L52)

请求 DTO：
- [BookSearchDTO](file:///D:/etread-cloud-end333/etread-module-book/src/main/java/com/etread/dto/BookSearchDTO.java)

SQL 动态条件（关键逻辑）：
- Mapper：[BookInfoMapper.searchBooks](file:///D:/etread-cloud-end333/etread-module-book/src/main/java/com/etread/mapper/BookInfoMapper.java#L30-L112)
- 特点：
    - 默认只查 `status=1`
    - title/author 模糊搜索
    - 评分区间基于 `total_score/rating_count`
    - 标签筛选通过 `book_tag_relation + book_tag`，并且 `HAVING COUNT(DISTINCT tag_name) = tagCount`（多标签是“交集匹配”）

---

## 7. 章节内容接口与性能优化（高频请求优化）

章节目录：
- `POST /book/chapter/catalog`

章节内容（批量）：
- `POST /book/chapter/contents`

控制器：
- [ChapterController](file:///D:/etread-cloud-end333/etread-module-book/src/main/java/com/etread/controller/ChapterController.java)

后端性能点：
1. Redis 缓存章节内容（先查缓存，miss 查 DB，再回填）
    - [BookChapterContentServiceImpl.listByChapterIds](file:///D:/etread-cloud-end333/etread-module-book/src/main/java/com/etread/service/impl/BookChapterContentServiceImpl.java#L55-L113)
2. “读后预热”：请求当前章节内容后，异步预加载后续 2 章到 Redis
    - [ReadAheadPrewarmServiceImpl](file:///D:/etread-cloud-end333/etread-module-book/src/main/java/com/etread/service/impl/ReadAheadPrewarmServiceImpl.java#L38-L83)

---

## 8. 书评与平均分（评分聚合）

书评接口：
- `POST /book/review/add`
- `GET /book/review/list/{bookId}`

控制器：
- [BookReviewController](file:///D:/etread-cloud-end333/etread-module-book/src/main/java/com/etread/controller/BookReviewController.java)

聚合更新（平均分依赖字段）：
- 服务：[BookReviewServiceImpl.addReview](file:///D:/etread-cloud-end333/etread-module-book/src/main/java/com/etread/service/impl/BookReviewServiceImpl.java#L25-L46)
    - 每次新增书评会更新 `book_info.total_score += rating`、`book_info.rating_count += 1`
- 平均分一般由 `total_score / rating_count` 计算得到（前端/后端可择一计算）

---

## 9. 云端书架（加入/移除/查询）

接口：
- `POST /book/shelf/add`（加入书架）
- `POST /book/shelf/remove`（移出书架）
- `GET /book/bookshelf/{userId}`（查询书架列表）

控制器：
- [BookShelfController](file:///D:/etread-cloud-end333/etread-module-book/src/main/java/com/etread/controller/BookShelfController.java)

核心业务：
- [UserBookshelfServiceImpl](file:///D:/etread-cloud-end333/etread-module-book/src/main/java/com/etread/service/impl/UserBookshelfServiceImpl.java)
    - 加入时校验：书必须存在且 `status==1`，避免把“解析中/下架/失败”的书加入书架
    - 重复加入会报错
    - 上传者不允许“移出书架”，提示应“删除书籍”

表实体：
- [UserBookshelf](file:///D:/etread-cloud-end333/etread-module-book/src/main/java/com/etread/entity/UserBookshelf.java)

---

## 10. 段评/评论模块（概览）

模块：
- `etread-module-comment`

核心能力：
- 段评发布/查询、点赞/取消点赞
- Redis 辅助热评与计数（部分实现）

入口控制器：
- [CommentController.java](file:///D:/etread-cloud-end333/etread-module-comment/src/main/java/com/etread/controller/CommentController.java)

---

## 11. 本地开发启动建议（Windows）

按模块启动（IDE 直接 Run 各自的 `*Application.java`）：
- `UserApplication`（8081）
- `EtreadModuleBookApplication`（8082）
- `EtreadModuleCommentApplication`（8083）

依赖先启动：
- Redis + MinIO：`docker-compose up -d`（在仓库根目录）

---

## 12. 面试答辩口径（建议背的 6 句话）

1. 上传链路：前端 multipart → 后端 token 校验 → MinIO 存文件 → MySQL 存元数据 → 异步并发解析章节 → 成功上架。
2. 搜索：基于动态 SQL 支持多条件筛选，标签用关系表并做交集匹配，默认只查上架书。
3. 章节性能：章节内容 Redis 缓存 + miss 回源 DB + 回填缓存，并在读取后异步预热后续章节。
4. 评分：书评落库后用聚合字段（total_score/rating_count）维护平均分，避免每次详情页实时聚合计算。
5. 云端书架：用户-书关系表，加入时校验书状态，防止无效书进入书架，并提供查询/移除接口。
6. 安全：用户身份不信任前端字段，核心接口从 token 强制解析 userId 作为作者/书评者/书架操作者。

---
