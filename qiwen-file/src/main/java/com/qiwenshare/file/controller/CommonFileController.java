package com.qiwenshare.file.controller;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiwenshare.common.anno.MyLog;
import com.qiwenshare.common.result.RestResult;
import com.qiwenshare.common.util.security.SessionUtil;
import com.qiwenshare.file.api.ICommonFileService;
import com.qiwenshare.file.api.IFilePermissionService;
import com.qiwenshare.file.api.IUserFileService;
import com.qiwenshare.file.domain.CommonFile;
import com.qiwenshare.file.domain.FilePermission;
import com.qiwenshare.file.domain.UserFile;
import com.qiwenshare.file.dto.commonfile.CommonFileDTO;
import com.qiwenshare.file.io.QiwenFile;
import com.qiwenshare.file.vo.commonfile.CommonFileListVo;
import com.qiwenshare.file.vo.commonfile.CommonFileUser;
import com.qiwenshare.file.vo.file.FileListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "common",
     description = "该接口为文件共享接口")
@RestController
@Slf4j
@RequestMapping("/common")
public class CommonFileController
{

    private static final String CURRENT_MODULE = "文件共享";

    @Resource
    private ICommonFileService commonFileService;
    @Resource
    private IFilePermissionService filePermissionService;
    @Resource
    private IUserFileService userFileService;

    @Operation(summary = "将文件共享给他人",
               description = "共享文件统一接口",
               tags = {"common"})
    @PostMapping(value = "/commonfile")
    @MyLog(operation = "共享文件",
           module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<String> commonFile(
            @RequestBody
                    CommonFileDTO commonFileDTO) {
        // 保存共享文件
        CommonFile commonFile = new CommonFile()
                .setUserFileId(commonFileDTO.getUserFileId())
                .setCommonFileId(IdUtil.getSnowflakeNextIdStr());
        commonFileService.save(commonFile);

        // 保存共享文件权限
        List<FilePermission> filePermissionList = JSON
                .parseArray(commonFileDTO.getCommonUserList(), FilePermission.class)
                .stream()
                .peek(filePermission -> filePermission
                        .setFilePermissionId(IdUtil.getSnowflakeNextId())
                        .setCommonFileId(commonFile.commonFileId))
                .collect(Collectors.toList());
        filePermissionService.saveBatch(filePermissionList);

        return RestResult.success();
    }

    @Operation(summary = "获取共享空间的全量用户列表",
               description = "共享文件用户接口",
               tags = {"common"})
    @GetMapping(value = "/commonfileuser")
    @MyLog(operation = "共享文件用户",
           module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<List<CommonFileUser>> commonFileUserList() {
        List<CommonFileUser> list = commonFileService.selectCommonFileUser(SessionUtil.getUserId());
        return RestResult
                .<List<CommonFileUser>>success()
                .data(list);
    }

    @Operation(summary = "获取共享用户文件列表",
               description = "用来做前台列表展示",
               tags = {"file"})
    @RequestMapping(value = "/getCommonFileByUser",
                    method = RequestMethod.GET)
    @ResponseBody
    public RestResult<List<CommonFileListVo>> getCommonFileByUser(
            @Parameter(description = "用户id",
                       required = true)
                    String userId) {
        List<CommonFileListVo> commonFileVo = commonFileService.selectCommonFileByUser(userId, SessionUtil.getUserId());
        return RestResult
                .<List<CommonFileListVo>>success()
                .data(commonFileVo);

    }

    @Operation(summary = "获取共享空间中某个用户的文件列表",
               description = "用来做前台列表展示",
               tags = {"file"})
    @RequestMapping(value = "/commonFileList",
                    method = RequestMethod.GET)
    @ResponseBody
    public RestResult<IPage<FileListVO>> commonFileList(
            @Parameter(description = "用户id",
                       required = true)
                    Long commonFileId,
            @Parameter(description = "文件路径",
                       required = true)
                    String filePath,
            @Parameter(description = "当前页",
                       required = true)
                    long currentPage,
            @Parameter(description = "页面数量",
                       required = true)
                    long pageCount) {
        CommonFile commonFile = commonFileService.getById(commonFileId);
        UserFile userFile = userFileService.getById(commonFile.getUserFileId());
        QiwenFile qiwenFile = new QiwenFile(userFile.getFilePath(), filePath, true);
        IPage<FileListVO> fileList = userFileService.userFileList(userFile.getUserId(), qiwenFile.getPath(), currentPage, pageCount);

        return RestResult
                .<IPage<FileListVO>>success()
                .data(fileList);
    }

}
