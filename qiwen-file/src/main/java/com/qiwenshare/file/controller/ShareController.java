package com.qiwenshare.file.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiwenshare.common.anno.MyLog;
import com.qiwenshare.common.result.RestResult;
import com.qiwenshare.common.util.DateUtil;
import com.qiwenshare.common.util.security.SessionUtil;
import com.qiwenshare.file.api.IShareFileService;
import com.qiwenshare.file.api.IShareService;
import com.qiwenshare.file.api.IUserFileService;
import com.qiwenshare.file.component.FileDealComp;
import com.qiwenshare.file.domain.Share;
import com.qiwenshare.file.domain.ShareFile;
import com.qiwenshare.file.domain.UserFile;
import com.qiwenshare.file.dto.sharefile.CheckEndTimeDTO;
import com.qiwenshare.file.dto.sharefile.CheckExtractionCodeDTO;
import com.qiwenshare.file.dto.sharefile.SaveShareFileDTO;
import com.qiwenshare.file.dto.sharefile.ShareFileDTO;
import com.qiwenshare.file.dto.sharefile.ShareFileListDTO;
import com.qiwenshare.file.dto.sharefile.ShareListDTO;
import com.qiwenshare.file.dto.sharefile.ShareTypeDTO;
import com.qiwenshare.file.io.QiwenFile;
import com.qiwenshare.file.vo.share.ShareFileListVO;
import com.qiwenshare.file.vo.share.ShareFileVO;
import com.qiwenshare.file.vo.share.ShareListVO;
import com.qiwenshare.file.vo.share.ShareTypeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Tag(name = "share",
     description = "该接口为文件分享接口")
@RestController
@Slf4j
@RequestMapping("/share")
public class ShareController
{
    private static final String CURRENT_MODULE = "文件分享";

    @Resource
    private IShareFileService shareFileService;
    @Resource
    private IShareService shareService;
    @Resource
    private IUserFileService userFileService;
    @Resource
    private FileDealComp fileDealComp;

    @Operation(summary = "分享文件",
               description = "分享文件统一接口",
               tags = {"share"})
    @PostMapping(value = "/sharefile")
    @MyLog(operation = "分享文件",
           module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<ShareFileVO> shareFile(
            @RequestBody
                    ShareFileDTO shareFileDTO) {
        final String userId = SessionUtil.getUserId();
        ShareFileVO shareSecretVO = new ShareFileVO();

        // 保存分享信息
        String uuid = UUID
                .randomUUID()
                .toString()
                .replace("-", "");
        Share share = new Share();
        BeanUtil.copyProperties(shareFileDTO, share);
        share
                .setShareId(IdUtil.getSnowflakeNextIdStr())
                .setShareTime(DateUtil.getCurrentTime())
                .setUserId(userId)
                .setShareStatus(0);
        if (shareFileDTO.getShareType() == 1) {
            String extractionCode = RandomUtil.randomNumbers(6);
            share.setExtractionCode(extractionCode);
            shareSecretVO.setExtractionCode(share.getExtractionCode());
        }
        share.setShareBatchNum(uuid);
        shareService.save(share);

        // 保存分享文件信息
        List<ShareFile> saveFileList = new ArrayList<>();
        String[] userFileIdList = shareFileDTO
                .getUserFileIds()
                .split(",");
        for (String userFileId : userFileIdList) {
            UserFile userFile = userFileService.getById(userFileId);
            if (userFile
                    .getUserId()
                    .compareTo(userId) != 0) {
                return RestResult
                        .<ShareFileVO>fail()
                        .message("您只能分享自己的文件");
            }

            // 判断是否是文件夹
            if (userFile.isDirectory()) {
                // 获取文件夹下的所有文件
                QiwenFile qiwenFile = new QiwenFile(userFile.getFilePath(), userFile.getFileName(), true);
                List<UserFile> userfileList = userFileService.selectUserFileByLikeRightFilePath(qiwenFile.getPath(), userId);
                for (UserFile userFile1 : userfileList) {
                    final String shareFilePath = userFile1
                            .getFilePath()
                            .replaceFirst(userFile
                                                  .getFilePath()
                                                  .equals("/") ?
                                          "" :
                                          userFile.getFilePath(), "");
                    saveFileList.add(new ShareFile()
                            .setShareFileId(IdUtil.getSnowflakeNextIdStr())
                            .setUserFileId(userFile1.getUserFileId())
                            .setShareBatchNum(uuid)
                            .setShareFilePath(shareFilePath));
                }
            }

            ShareFile shareFile = new ShareFile();
            shareFile
                    .setShareFileId(IdUtil.getSnowflakeNextIdStr())
                    .setUserFileId(userFileId)
                    .setShareFilePath("/")
                    .setShareBatchNum(uuid);
            saveFileList.add(shareFile);
        }
        shareFileService.saveBatch(saveFileList);
        shareSecretVO.setShareBatchNum(uuid);

        return RestResult
                .<ShareFileVO>success()
                .data(shareSecretVO);
    }

    @Operation(summary = "保存分享文件",
               description = "用来将别人分享的文件保存到自己的网盘中",
               tags = {"share"})
    @PostMapping(value = "/savesharefile")
    @MyLog(operation = "保存分享文件",
           module = CURRENT_MODULE)
    @Transactional(rollbackFor = Exception.class)
    @ResponseBody
    public RestResult<String> saveShareFile(
            @RequestBody
                    SaveShareFileDTO saveShareFileDTO) {
        final String userId = SessionUtil.getUserId();

        //        List<ShareFile> fileList = JSON.parseArray(saveShareFileDTO.getFiles(), ShareFile.class);
        String savefilePath = saveShareFileDTO.getFilePath();
        String[] userFileIdArr = saveShareFileDTO
                .getUserFileIds()
                .split(",");
        List<UserFile> saveUserFileList = new ArrayList<>();
        for (String userFileId : userFileIdArr) {
            // 获取文件
            UserFile userFile = userFileService.getById(userFileId);
            String fileName = userFile.getFileName();
            String filePath = userFile.getFilePath();

            UserFile userFile2 = new UserFile();
            BeanUtil.copyProperties(userFile, userFile2);

            // 判断文件名是否重复
            String savefileName = fileDealComp.getRepeatFileName(userFile, savefilePath);

            // 是否目录
            if (userFile.isDirectory()) {
                // 获取分享目录
                ShareFile shareFile = shareFileService.getOne(new LambdaQueryWrapper<ShareFile>()
                        .eq(ShareFile::getUserFileId, userFileId)
                        .eq(ShareFile::getShareBatchNum, saveShareFileDTO.getShareBatchNum()));
                // 获取分享目录下的所有文件
                List<ShareFile> shareFileList = shareFileService.list(new LambdaQueryWrapper<ShareFile>()
                        .eq(ShareFile::getShareBatchNum, saveShareFileDTO.getShareBatchNum())
                        .likeRight(ShareFile::getShareFilePath, QiwenFile.formatPath(shareFile.getShareFilePath() + "/" + fileName)));

                // 遍历分享目录下的所有文件
                for (ShareFile shareFile1 : shareFileList) {
                    UserFile userFile1 = userFileService.getById(shareFile1.getUserFileId());
                    final String filePath1 = userFile1
                            .getFilePath()
                            .replaceFirst(QiwenFile.formatPath(filePath + "/" + fileName), QiwenFile.formatPath(savefilePath + "/" + savefileName));
                    userFile1
                            .setUserFileId(IdUtil.getSnowflakeNextIdStr())
                            .setUserId(userId)
                            .setFilePath(filePath1);
                    saveUserFileList.add(userFile1);
                    log.info("当前文件：" + JSON.toJSONString(userFile1));
                }
            }
            userFile2.setUserFileId(IdUtil.getSnowflakeNextIdStr());
            userFile2.setUserId(userId);
            userFile2.setFilePath(savefilePath);
            userFile2.setFileName(savefileName);
            saveUserFileList.add(userFile2);
        }
        log.info("----------" + JSON.toJSONString(saveUserFileList));
        userFileService.saveBatch(saveUserFileList);

        return RestResult.<String>success();
    }

    @Operation(summary = "查看已分享列表",
               description = "查看已分享列表",
               tags = {"share"})
    @GetMapping(value = "/shareList")
    @ResponseBody
    public RestResult<ShareListVO> shareList(ShareListDTO shareListDTO) {
        final String userId = SessionUtil.getUserId();
        // 获取分享列表
        List<ShareListVO> shareList = shareService.selectShareList(shareListDTO, userId);
        // 获取分享列表总数
        int total = shareService.selectShareListTotalCount(shareListDTO, userId);
        return RestResult
                .<ShareListVO>success()
                .dataList(shareList, total);
    }

    @Operation(summary = "分享文件列表",
               description = "分享列表",
               tags = {"share"})
    @GetMapping(value = "/sharefileList")
    @ResponseBody
    public RestResult<ShareFileListVO> shareFileList(ShareFileListDTO shareFileListBySecretDTO) {
        String shareBatchNum = shareFileListBySecretDTO.getShareBatchNum();
        String shareFilePath = shareFileListBySecretDTO.getShareFilePath();
        // 获取分享列表
        List<ShareFileListVO> list = shareFileService.selectShareFileList(shareBatchNum, shareFilePath);
        for (ShareFileListVO shareFileListVO : list) {
            shareFileListVO.setShareFilePath(shareFilePath);
        }
        return RestResult
                .<ShareFileListVO>success()
                .dataList(list, list.size());
    }

    @Operation(summary = "分享类型",
               description = "可用此接口判断是否需要提取码",
               tags = {"share"})
    @GetMapping(value = "/sharetype")
    @ResponseBody
    public RestResult<ShareTypeVO> shareType(ShareTypeDTO shareTypeDTO) {
        Share share = shareService.getOne(new LambdaQueryWrapper<Share>().eq(Share::getShareBatchNum, shareTypeDTO.getShareBatchNum()));
        ShareTypeVO shareTypeVO = new ShareTypeVO();
        shareTypeVO.setShareType(share.getShareType());
        return RestResult
                .<ShareTypeVO>success()
                .data(shareTypeVO);
    }

    @Operation(summary = "校验提取码",
               description = "校验提取码",
               tags = {"share"})
    @GetMapping(value = "/checkextractioncode")
    @ResponseBody
    public RestResult<String> checkExtractionCode(CheckExtractionCodeDTO checkExtractionCodeDTO) {
        // 获取分享列表
        List<Share> list = shareService.list(new LambdaQueryWrapper<Share>()
                .eq(Share::getShareBatchNum, checkExtractionCodeDTO.getShareBatchNum())
                .eq(Share::getExtractionCode, checkExtractionCodeDTO.getExtractionCode()));
        if (CollectionUtils.isEmpty(list)) {
            return RestResult
                    .<String>fail()
                    .message("校验失败");
        }
        return RestResult.success();
    }

    @Operation(summary = "校验过期时间",
               description = "校验过期时间",
               tags = {"share"})
    @GetMapping(value = "/checkendtime")
    @ResponseBody
    public RestResult<String> checkEndTime(CheckEndTimeDTO checkEndTimeDTO) {
        // 获取分享列表
        Share share = shareService.getOne(new LambdaQueryWrapper<Share>().eq(Share::getShareBatchNum, checkEndTimeDTO.getShareBatchNum()));
        if (Objects.isNull(share)) {
            return RestResult
                    .<String>fail()
                    .message("文件不存在！");
        }

        // 获取结束时间
        String endTime = share.getEndTime();
        Date endTimeDate = null;
        try {
            endTimeDate = DateUtil.getDateByFormatString(endTime, "yyyy-MM-dd HH:mm:ss");
        }
        catch (ParseException e) {
            log.error("日期解析失败：{}", e.getMessage(), e);
        }

        // 判断是否过期
        if (new Date().after(endTimeDate)) {
            return RestResult
                    .<String>fail()
                    .message("分享已过期");
        }
        return RestResult.success();
    }
}
