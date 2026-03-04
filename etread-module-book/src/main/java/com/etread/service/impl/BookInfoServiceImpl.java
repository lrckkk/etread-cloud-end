package com.etread.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etread.dto.BookInfoDTO;
import com.etread.entity.BookInfo;
import com.etread.mapper.BookInfoMapper;
import com.etread.service.BookInfoService;

public class BookInfoServiceImpl extends ServiceImpl<BookInfoMapper, BookInfo> implements BookInfoService {

    @Override
    public boolean updateStatus(BookInfoDTO bookInfo) {
        return false;
    }
}
