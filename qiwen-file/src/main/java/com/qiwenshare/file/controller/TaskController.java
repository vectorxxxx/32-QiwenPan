package com.qiwenshare.file.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiwenshare.file.api.IShareFileService;
import com.qiwenshare.file.component.FileDealComp;
import com.qiwenshare.file.constant.FileDeleteFlagEnum;
import com.qiwenshare.file.domain.ShareFile;
import com.qiwenshare.file.domain.UserFile;
import com.qiwenshare.file.io.QiwenFile;
import com.qiwenshare.file.service.UserFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Controller
public class TaskController
{

    @Resource
    private UserFileService userFileService;
    @Resource
    private FileDealComp fileDealComp;
    @Resource
    private IShareFileService shareFileService;
    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Scheduled(fixedRate = 1000 * 60 * 60 * 24)
    public void updateElasticSearch() {
        List<UserFile> userfileList = userFileService.list(new LambdaQueryWrapper<UserFile>().eq(UserFile::getDeleteFlag, FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag()));
        for (int i = 0; i < userfileList.size(); i++) {
            try {
                final UserFile userFile = userfileList.get(i);
                QiwenFile ufopFile = new QiwenFile(userFile.getFilePath(), userFile.getFileName(), userFile.isDirectory());
                fileDealComp.restoreParentFilePath(ufopFile, userFile.getUserId());
                if (i % 1000 == 0 || i == userfileList.size() - 1) {
                    log.info("目录健康检查进度：" + (i + 1) + "/" + userfileList.size());
                }

            }
            catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        userfileList = userFileService.list(new LambdaQueryWrapper<UserFile>().eq(UserFile::getDeleteFlag, FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag()));
        for (UserFile userFile : userfileList) {
            fileDealComp.uploadESByUserFileId(userFile.getUserFileId());
        }

    }

    @Scheduled(fixedRate = Long.MAX_VALUE)
    public void updateFilePath() {
        List<UserFile> list = userFileService.list();
        for (UserFile userFile : list) {
            try {
                String path = QiwenFile.formatPath(userFile.getFilePath());
                if (!userFile
                        .getFilePath()
                        .equals(path)) {
                    userFile.setFilePath(path);
                    userFileService.updateById(userFile);
                }
            }
            catch (Exception e) {
                // ignore
            }
        }
    }

    @Scheduled(fixedRate = Long.MAX_VALUE)
    public void updateShareFilePath() {
        List<ShareFile> list = shareFileService.list();
        for (ShareFile shareFile : list) {
            try {
                String path = QiwenFile.formatPath(shareFile.getShareFilePath());
                shareFile.setShareFilePath(path);
                shareFileService.updateById(shareFile);
            }
            catch (Exception e) {
                //ignore
            }
        }
    }
}
