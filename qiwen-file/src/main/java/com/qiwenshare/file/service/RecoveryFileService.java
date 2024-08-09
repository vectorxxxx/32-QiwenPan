package com.qiwenshare.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiwenshare.file.api.IRecoveryFileService;
import com.qiwenshare.file.component.FileDealComp;
import com.qiwenshare.file.constant.FileDeleteFlagEnum;
import com.qiwenshare.file.domain.RecoveryFile;
import com.qiwenshare.file.domain.UserFile;
import com.qiwenshare.file.io.QiwenFile;
import com.qiwenshare.file.mapper.RecoveryFileMapper;
import com.qiwenshare.file.mapper.UserFileMapper;
import com.qiwenshare.file.vo.file.RecoveryFileListVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class RecoveryFileService extends ServiceImpl<RecoveryFileMapper, RecoveryFile> implements IRecoveryFileService
{
    @Resource
    UserFileMapper userFileMapper;
    @Resource
    RecoveryFileMapper recoveryFileMapper;
    @Resource
    FileDealComp fileDealComp;

    @Override
    public void deleteUserFileByDeleteBatchNum(String deleteBatchNum) {
        userFileMapper.delete(new LambdaQueryWrapper<UserFile>().eq(UserFile::getDeleteBatchNum, deleteBatchNum));
    }

    @Override
    public void restorefile(String deleteBatchNum, String filePath, String sessionUserId) {
        // 查询回收站的文件
        List<UserFile> restoreUserFileList = userFileMapper.selectList(new LambdaQueryWrapper<UserFile>().eq(UserFile::getDeleteBatchNum, deleteBatchNum));
        for (UserFile restoreUserFile : restoreUserFileList) {
            // 文件重命名
            restoreUserFile
                    .setDeleteFlag(FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag())
                    .setDeleteBatchNum(deleteBatchNum);
            String fileName = fileDealComp.getRepeatFileName(restoreUserFile, restoreUserFile.getFilePath());
            // 判断是否是目录
            if (restoreUserFile.isDirectory()) {
                if (!StringUtils.equals(fileName, restoreUserFile.getFileName())) {
                    userFileMapper.deleteById(restoreUserFile);
                }
                else {
                    userFileMapper.updateById(restoreUserFile);
                }
            }
            // 判断是否是文件
            else if (restoreUserFile.isFile()) {
                restoreUserFile.setFileName(fileName);
                userFileMapper.updateById(restoreUserFile);
            }
        }

        // 恢复父级目录
        QiwenFile qiwenFile = new QiwenFile(filePath, true);
        fileDealComp.restoreParentFilePath(qiwenFile, sessionUserId);

        // 删除回收站文件
        recoveryFileMapper.delete(new LambdaQueryWrapper<RecoveryFile>().eq(RecoveryFile::getDeleteBatchNum, deleteBatchNum));
    }

    @Override
    public List<RecoveryFileListVo> selectRecoveryFileList(String userId) {
        return recoveryFileMapper.selectRecoveryFileList(userId);
    }
}
