package com.qiwenshare.file.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiwenshare.common.anno.MyLog;
import com.qiwenshare.common.result.RestResult;
import com.qiwenshare.common.util.security.JwtUser;
import com.qiwenshare.common.util.security.SessionUtil;
import com.qiwenshare.file.api.IRecoveryFileService;
import com.qiwenshare.file.component.AsyncTaskComp;
import com.qiwenshare.file.domain.RecoveryFile;
import com.qiwenshare.file.dto.file.DeleteRecoveryFileDTO;
import com.qiwenshare.file.dto.recoveryfile.BatchDeleteRecoveryFileDTO;
import com.qiwenshare.file.dto.recoveryfile.RestoreFileDTO;
import com.qiwenshare.file.vo.file.RecoveryFileListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Tag(name = "recoveryfile",
     description = "文件删除后会进入回收站，该接口主要是对回收站文件进行管理")
@RestController
@Slf4j
@RequestMapping("/recoveryfile")
public class RecoveryFileController
{
    private static final String CURRENT_MODULE = "回收站文件接口";

    @Resource
    private IRecoveryFileService recoveryFileService;

    @Resource
    private AsyncTaskComp asyncTaskComp;

    @Operation(summary = "删除回收文件",
               description = "删除回收文件",
               tags = {"recoveryfile"})
    @MyLog(operation = "删除回收文件",
           module = CURRENT_MODULE)
    @RequestMapping(value = "/deleterecoveryfile",
                    method = RequestMethod.POST)
    @ResponseBody
    public RestResult<String> deleteRecoveryFile(
            @RequestBody
                    DeleteRecoveryFileDTO deleteRecoveryFileDTO) {
        // 获取回收站文件
        RecoveryFile recoveryFile = recoveryFileService.getOne(new LambdaQueryWrapper<RecoveryFile>().eq(RecoveryFile::getUserFileId, deleteRecoveryFileDTO.getUserFileId()));

        // 删除文件
        asyncTaskComp.deleteUserFile(recoveryFile.getUserFileId());

        // 删除回收站文件
        recoveryFileService.removeById(recoveryFile.getRecoveryFileId());
        return RestResult
                .<String>success()
                .data("删除成功");
    }

    @Operation(summary = "批量删除回收文件",
               description = "批量删除回收文件",
               tags = {"recoveryfile"})
    @RequestMapping(value = "/batchdelete",
                    method = RequestMethod.POST)
    @MyLog(operation = "批量删除回收文件",
           module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<String> batchDeleteRecoveryFile(
            @RequestBody
                    BatchDeleteRecoveryFileDTO batchDeleteRecoveryFileDTO) {
        String[] userFileIdList = batchDeleteRecoveryFileDTO
                .getUserFileIds()
                .split(",");
        for (String userFileId : userFileIdList) {
            // 获取回收站文件
            RecoveryFile recoveryFile = recoveryFileService.getOne(new LambdaQueryWrapper<RecoveryFile>().eq(RecoveryFile::getUserFileId, userFileId));
            if (Objects.nonNull(recoveryFile)) {
                // 删除文件
                asyncTaskComp.deleteUserFile(recoveryFile.getUserFileId());
                // 删除回收站文件
                recoveryFileService.removeById(recoveryFile.getRecoveryFileId());
            }
        }
        return RestResult
                .<String>success()
                .data("批量删除成功");
    }

    @Operation(summary = "回收文件列表",
               description = "回收文件列表",
               tags = {"recoveryfile"})
    @RequestMapping(value = "/list",
                    method = RequestMethod.GET)
    @ResponseBody
    public RestResult<RecoveryFileListVo> getRecoveryFileList() {
        JwtUser sessionUserBean = SessionUtil.getSession();
        List<RecoveryFileListVo> recoveryFileList = recoveryFileService.selectRecoveryFileList(sessionUserBean.getUserId());
        return RestResult
                .<RecoveryFileListVo>success()
                .dataList(recoveryFileList, recoveryFileList.size());
    }

    @Operation(summary = "还原文件",
               description = "还原文件",
               tags = {"recoveryfile"})
    @RequestMapping(value = "/restorefile",
                    method = RequestMethod.POST)
    @MyLog(operation = "还原文件",
           module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<String> restoreFile(
            @RequestBody
                    RestoreFileDTO restoreFileDto) {
        recoveryFileService.restorefile(restoreFileDto.getDeleteBatchNum(), restoreFileDto.getFilePath(), SessionUtil.getUserId());
        return RestResult
                .<String>success()
                .message("还原成功！");
    }

}









