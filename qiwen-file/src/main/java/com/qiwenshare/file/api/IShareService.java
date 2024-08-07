package com.qiwenshare.file.api;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qiwenshare.file.domain.Share;
import com.qiwenshare.file.dto.sharefile.ShareListDTO;
import com.qiwenshare.file.vo.share.ShareListVO;

import java.util.List;

public interface IShareService extends IService<Share>
{
    List<ShareListVO> selectShareList(ShareListDTO shareListDTO, String userId);

    int selectShareListTotalCount(ShareListDTO shareListDTO, String userId);
}
