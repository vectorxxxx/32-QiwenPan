package com.qiwenshare.file.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qiwenshare.file.domain.Notice;
import com.qiwenshare.file.dto.notice.NoticeListDTO;
import com.qiwenshare.file.vo.notice.NoticeVO;

public interface INoticeService extends IService<Notice>
{

    IPage<NoticeVO> selectUserPage(NoticeListDTO noticeListDTO);

}
