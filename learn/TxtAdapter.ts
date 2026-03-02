/**
 * TXT 格式适配器
 * 将 TXT 文件转换为统一的 UnifiedBook 结构
 * 使用 ContentLayer 架构优化内存
 */

import type { UnifiedBook, UnifiedChapter, ContentLayer } from '@/types/book';

export class TxtAdapter {
  /**
   * 章节检测正则表达式
   * 匹配：第x章、第x回、第x节等常见格式
   */
  private static readonly CHAPTER_REGEX = /^第[一二三四五六七八九十百千万0-9]+[章回节卷集部篇].*/;

  /**
   * Maximum characters per ContentLayer to prevent DOM performance issues
   */
  private static readonly MAX_LAYER_LENGTH = 15000;

  /**
   * 解析 TXT 文件为 UnifiedBook
   */
  static async parse(
    arrayBuffer: ArrayBuffer,
    filename: string
  ): Promise<UnifiedBook> {
    console.log('[TxtAdapter] 开始解析 TXT 文件:', filename);

    // 1. 编码检测和文本解码
    const text = this.decodeText(arrayBuffer);
    console.log('[TxtAdapter] 文本长度:', text.length, '字符');

    // 2. 提取元数据
    const title = this.extractTitle(filename);
    const author = 'Unknown';

    // 3. 切分章节
    const chapters = this.splitChapters(text);
    console.log('[TxtAdapter] 检测到章节数:', chapters.length);

    // 4. 构建 UnifiedBook
    const book: UnifiedBook = {
      title,
      author,
      cover: null,
      format: 'txt',
      chapters,
      rawData: arrayBuffer,
      addTime: Date.now()
    };

    return book;
  }

  /**
   * 编码检测和文本解码
   * 优先尝试 UTF-8，失败则尝试 GBK
   */
  private static decodeText(arrayBuffer: ArrayBuffer): string {
    try {
      // 尝试 UTF-8 严格模式
      const text = new TextDecoder('utf-8', { fatal: true }).decode(arrayBuffer);
      console.log('[TxtAdapter] 使用 UTF-8 编码');
      return text;
    } catch {
      try {
        // 尝试 GBK
        const text = new TextDecoder('gbk').decode(arrayBuffer);
        console.log('[TxtAdapter] 使用 GBK 编码');
        return text;
      } catch {
        // 降级到 UTF-8 非严格模式
        console.log('[TxtAdapter] 使用 UTF-8 非严格模式');
        return new TextDecoder('utf-8').decode(arrayBuffer);
      }
    }
  }

  /**
   * 从文件名提取标题
   */
  private static extractTitle(filename: string): string {
    return filename.replace(/\.(txt|TXT)$/, '');
  }

  /**
   * 切分章节
   * 使用正则表达式检测章节标题
   */
  private static splitChapters(text: string): UnifiedChapter[] {
    const lines = text.split('\n');
    const chapters: UnifiedChapter[] = [];
    let currentChapter: UnifiedChapter | null = null;
    let chapterIndex = 0;

    for (const line of lines) {
      const trimmedLine = line.trim();
      
      // 跳过空行
      if (!trimmedLine) continue;

      // 检测是否为章节标题
      if (this.CHAPTER_REGEX.test(trimmedLine)) {
        // 保存上一章
        if (currentChapter) {
          chapters.push(currentChapter);
        }

        // 创建新章节（懒加载，layers 为空）
        currentChapter = {
          id: chapterIndex++,
          title: trimmedLine,
          layers: [],
          isLoaded: false
        };
      } else {
        // 如果还没有章节，创建默认章节
        if (!currentChapter) {
          currentChapter = {
            id: chapterIndex++,
            title: '正文',
            layers: [],
            isLoaded: false
          };
        }
      }
    }

    // 保存最后一章
    if (currentChapter) {
      chapters.push(currentChapter);
    }

    // 如果没有检测到任何章节，创建单章节
    if (chapters.length === 0) {
      chapters.push({
        id: 0,
        title: '全文',
        layers: [],
        isLoaded: false
      });
    }

    return chapters;
  }

  /**
   * 懒加载：解析指定章节的内容
   * 将文本转换为 ContentLayer 数组（段落数组格式）
   * 应用文本分块以防止 DOM 性能问题
   */
  static async loadChapter(
    book: UnifiedBook,
    chapterId: number
  ): Promise<ContentLayer[]> {
    console.log('[TxtAdapter] 懒加载章节:', chapterId);

    const chapter = book.chapters[chapterId];
    if (!chapter) {
      throw new Error(`章节 ${chapterId} 不存在`);
    }

    // 如果已加载，直接返回
    if (chapter.isLoaded && chapter.layers.length > 0) {
      console.log('[TxtAdapter] 章节已缓存，直接返回');
      return chapter.layers;
    }

    // 解码文本
    const text = this.decodeText(book.rawData);
    const lines = text.split('\n');

    // 找到章节的起始和结束位置
    const { startLine, endLine } = this.findChapterBounds(lines, chapter.title, chapterId, book.chapters);

    // 提取章节内容，按段落存储
    const paragraphs: string[] = [];
    
    for (let i = startLine; i < endLine; i++) {
      const line = lines[i].trim();
      
      // 跳过章节标题行
      if (this.CHAPTER_REGEX.test(line)) continue;
      
      // 跳过空行
      if (!line) continue;
      
      // 每一行作为一个段落
      paragraphs.push(line);
    }

    // 应用段落分块策略
    const layers = this.chunkParagraphs(paragraphs);

    console.log('[TxtAdapter] 章节解析完成，layers 数:', layers.length);
    console.log('[TxtAdapter] 段落总数:', paragraphs.length);
    
    return layers;
  }

  /**
   * Chunk paragraphs into multiple ContentLayers
   * Prevents DOM performance issues with extremely long content
   * 
   * 索引计数规则：
   * - 每个段落占用 1 个索引位
   * - startIndex 记录该 Layer 第一段的全局索引
   */
  static chunkParagraphs(paragraphs: string[]): ContentLayer[] {
    const layers: ContentLayer[] = [];
    let currentChunk: string[] = [];
    let currentChunkSize = 0;
    let startIndex = 0;

    for (const para of paragraphs) {
      // Check if adding this paragraph would exceed the limit
      if (currentChunkSize + para.length > this.MAX_LAYER_LENGTH && currentChunk.length > 0) {
        // Create a layer with current chunk
        layers.push({
          paragraphs: [...currentChunk],
          startIndex
        });
        
        // Start new chunk
        startIndex += currentChunk.length;
        currentChunk = [];
        currentChunkSize = 0;
      }
      
      currentChunk.push(para);
      currentChunkSize += para.length;
    }

    // Add remaining chunk
    if (currentChunk.length > 0) {
      layers.push({
        paragraphs: [...currentChunk],
        startIndex
      });
    }

    return layers;
  }

  /**
   * 查找章节在原文中的边界
   */
  private static findChapterBounds(
    lines: string[],
    chapterTitle: string,
    chapterId: number,
    allChapters: UnifiedChapter[]
  ): { startLine: number; endLine: number } {
    let startLine = 0;
    let endLine = lines.length;

    // 查找当前章节的起始行
    for (let i = 0; i < lines.length; i++) {
      if (lines[i].trim() === chapterTitle) {
        startLine = i + 1; // 跳过标题行
        break;
      }
    }

    // 查找下一章节的起始行（作为当前章节的结束）
    if (chapterId < allChapters.length - 1) {
      const nextChapterTitle = allChapters[chapterId + 1].title;
      for (let i = startLine; i < lines.length; i++) {
        if (lines[i].trim() === nextChapterTitle) {
          endLine = i;
          break;
        }
      }
    }

    return { startLine, endLine };
  }
}
