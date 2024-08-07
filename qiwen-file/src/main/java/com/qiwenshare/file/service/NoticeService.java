package com.qiwenshare.file.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiwenshare.file.api.INoticeService;
import com.qiwenshare.file.domain.Notice;
import com.qiwenshare.file.dto.notice.NoticeListDTO;
import com.qiwenshare.file.mapper.NoticeMapper;
import com.qiwenshare.file.util.BeanCopyUtils;
import com.qiwenshare.file.vo.notice.NoticeVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class NoticeService extends ServiceImpl<NoticeMapper, Notice> implements INoticeService
{
    @Resource
    NoticeMapper noticeMapper;

    @Override
    public IPage<NoticeVO> selectUserPage(NoticeListDTO noticeListDTO) {
        Page<Notice> page = new Page<>(noticeListDTO.getPage(), noticeListDTO.getPageSize());
        final IPage<Notice> noticeIPage = noticeMapper.selectPageVo(page, noticeListDTO);
        return BeanCopyUtils.copyPage(noticeIPage, NoticeVO.class);
    }
}
