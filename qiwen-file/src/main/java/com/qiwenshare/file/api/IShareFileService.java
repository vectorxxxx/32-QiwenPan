package com.qiwenshare.file.api;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qiwenshare.file.domain.ShareFile;
import com.qiwenshare.file.vo.share.ShareFileListVO;

import java.util.List;

public interface IShareFileService extends IService<ShareFile>
{

    List<ShareFileListVO> selectShareFileList(String shareBatchNum, String filePath);
}
