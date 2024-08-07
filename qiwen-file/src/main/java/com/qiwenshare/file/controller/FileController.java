package com.qiwenshare.file.controller;

import cn.hutool.core.util.IdUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlighterEncoder;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiwenshare.common.anno.MyLog;
import com.qiwenshare.common.exception.QiwenException;
import com.qiwenshare.common.result.RestResult;
import com.qiwenshare.common.util.DateUtil;
import com.qiwenshare.common.util.security.JwtUser;
import com.qiwenshare.common.util.security.SessionUtil;
import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.api.IUserFileService;
import com.qiwenshare.file.component.AsyncTaskComp;
import com.qiwenshare.file.component.FileDealComp;
import com.qiwenshare.file.config.es.FileSearch;
import com.qiwenshare.file.constant.FileDeleteFlagEnum;
import com.qiwenshare.file.constant.FileExtendTemplatePathEnum;
import com.qiwenshare.file.domain.FileBean;
import com.qiwenshare.file.domain.UserFile;
import com.qiwenshare.file.dto.file.BatchDeleteFileDTO;
import com.qiwenshare.file.dto.file.BatchMoveFileDTO;
import com.qiwenshare.file.dto.file.CopyFileDTO;
import com.qiwenshare.file.dto.file.CreateFileDTO;
import com.qiwenshare.file.dto.file.CreateFoldDTO;
import com.qiwenshare.file.dto.file.DeleteFileDTO;
import com.qiwenshare.file.dto.file.MoveFileDTO;
import com.qiwenshare.file.dto.file.RenameFileDTO;
import com.qiwenshare.file.dto.file.SearchFileDTO;
import com.qiwenshare.file.dto.file.UnzipFileDTO;
import com.qiwenshare.file.dto.file.UpdateFileDTO;
import com.qiwenshare.file.io.QiwenFile;
import com.qiwenshare.file.util.BeanCopyUtils;
import com.qiwenshare.file.util.QiwenFileUtil;
import com.qiwenshare.file.util.TreeNode;
import com.qiwenshare.file.vo.file.FileDetailVO;
import com.qiwenshare.file.vo.file.FileListVO;
import com.qiwenshare.file.vo.file.SearchFileVO;
import com.qiwenshare.ufop.factory.UFOPFactory;
import com.qiwenshare.ufop.operation.copy.Copier;
import com.qiwenshare.ufop.operation.copy.domain.CopyFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Tag(name = "file",
     description = "该接口为文件接口，主要用来做一些文件的基本操作，如创建目录，删除，移动，复制等。")
@RestController
@Slf4j
@RequestMapping("/file")
public class FileController
{
    private static final String CURRENT_MODULE = "文件接口";

    private static final String PATH_STATIC = "static";

    public static Executor executor = Executors.newFixedThreadPool(20);

    @Resource
    private IFileService fileService;
    @Resource
    private IUserFileService userFileService;
    @Resource
    private UFOPFactory ufopFactory;
    @Resource
    private FileDealComp fileDealComp;
    @Resource
    private AsyncTaskComp asyncTaskComp;
    @Autowired
    private ElasticsearchClient elasticsearchClient;
    @Value("${ufop.storage-type}")
    private Integer storageType;

    @Operation(summary = "创建文件",
               description = "创建文件",
               tags = {"file"})
    @ResponseBody
    @RequestMapping(value = "/createFile",
                    method = RequestMethod.POST)
    public RestResult<Object> createFile(@Valid
                                         @RequestBody
                                                 CreateFileDTO createFileDTO) {

        try {
            final String userId = SessionUtil.getUserId();
            List<UserFile> userFiles = userFileService.selectSameUserFile(createFileDTO, userId);
            if (!CollectionUtils.isEmpty(userFiles)) {
                return RestResult
                        .fail()
                        .message("同名文件已存在");
            }

            final String filePath = createFileDTO.getFilePath();
            final String fileName = createFileDTO.getFileName();
            final String extendName = createFileDTO.getExtendName();
            final String templateFilePath = Paths
                    .get(PATH_STATIC, FileExtendTemplatePathEnum.getTemplateFilePath(extendName))
                    .toString();
            final URL resource = Objects
                    .requireNonNull(ClassUtils.getDefaultClassLoader())
                    .getResource(templateFilePath);
            String url = Objects
                    .requireNonNull(resource)
                    .getPath();
            url = URLDecoder.decode(url, "UTF-8");
            FileInputStream fileInputStream = new FileInputStream(url);
            Copier copier = ufopFactory.getCopier();
            CopyFile copyFile = new CopyFile();
            copyFile.setExtendName(extendName);
            String fileUrl = copier.copy(fileInputStream, copyFile);

            // 保存文件信息
            final String currentTime = DateUtil.getCurrentTime();
            FileBean fileBean = new FileBean()
                    .setFileId(IdUtil.getSnowflakeNextIdStr())
                    .setFileSize(0L)
                    .setFileUrl(fileUrl)
                    .setStorageType(storageType)
                    .setIdentifier(UUID
                            .randomUUID()
                            .toString()
                            .replaceAll("-", ""))
                    .setCreateUserId(userId)
                    .setCreateTime(currentTime)
                    .setFileStatus(1);
            boolean saveFlag = fileService.save(fileBean);

            // 保存用户文件信息
            if (saveFlag) {
                UserFile userFile = new UserFile()
                        .setUserFileId(IdUtil.getSnowflakeNextIdStr())
                        .setFileId(fileBean.getFileId())
                        .setFileName(fileName)
                        .setFilePath(filePath)
                        .setDeleteFlag(FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag())
                        .setIsDir(0)
                        .setExtendName(extendName)
                        .setCreateUserId(userId)
                        .setCreateTime(currentTime)
                        .setUploadTime(currentTime);
                userFileService.save(userFile);
            }
            return RestResult
                    .success()
                    .message("文件创建成功");
        }
        catch (Exception e) {
            log.error("文件创建失败：{}", e.getMessage(), e);
            return RestResult
                    .fail()
                    .message(e.getMessage());
        }
    }

    @Operation(summary = "创建文件夹",
               description = "目录(文件夹)的创建",
               tags = {"file"})
    @RequestMapping(value = "/createFold",
                    method = RequestMethod.POST)
    @MyLog(operation = "创建文件夹",
           module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<String> createFold(@Valid
                                         @RequestBody
                                                 CreateFoldDTO createFoldDto) {
        final String userId = SessionUtil.getUserId();
        final String fileName = createFoldDto.getFileName();
        final String filePath = createFoldDto.getFilePath();

        // 判断文件夹是否存在
        boolean isDirExist = fileDealComp.isDirExist(fileName, filePath, userId);
        if (isDirExist) {
            return RestResult
                    .<String>fail()
                    .message("同名文件夹已存在");
        }

        // 保存文件夹信息
        UserFile userFile = QiwenFileUtil.getQiwenDir(userId, filePath, fileName);
        userFileService.save(userFile);

        // 建立 ElasticSearch 索引
        fileDealComp.uploadESByUserFileId(userFile.getUserFileId());
        return RestResult.success();
    }

    @Operation(summary = "文件搜索",
               description = "文件搜索",
               tags = {"file"})
    @GetMapping(value = "/search")
    @MyLog(operation = "文件搜索",
           module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<SearchFileVO> searchFile(SearchFileDTO searchFileDTO) {
        final String userId = SessionUtil.getUserId();

        int currentPage = (int) searchFileDTO.getCurrentPage() - 1;
        int pageCount = (int) (searchFileDTO.getPageCount() == 0 ?
                               10 :
                               searchFileDTO.getPageCount());

        // 文件搜索
        SearchResponse<FileSearch> search = null;
        try {
            search = elasticsearchClient.search(s -> s
                    .index("filesearch")
                    .query(_1 -> _1.bool(_2 -> _2
                            .must(_3 -> _3.bool(_4 -> _4
                                    .should(_5 -> _5.match(_6 -> _6
                                            .field("fileName")
                                            .query(searchFileDTO.getFileName())))
                                    .should(_5 -> _5.wildcard(_6 -> _6
                                            .field("fileName")
                                            .wildcard("*" + searchFileDTO.getFileName() + "*")))
                                    .should(_5 -> _5.match(_6 -> _6
                                            .field("content")
                                            .query(searchFileDTO.getFileName())))
                                    .should(_5 -> _5.wildcard(_6 -> _6
                                            .field("content")
                                            .wildcard("*" + searchFileDTO.getFileName() + "*")))))
                            .must(_3 -> _3.term(_4 -> _4
                                    .field("userId")
                                    .value(userId)))))
                    .from(currentPage)
                    .size(pageCount)
                    .highlight(h -> h
                            .fields("fileName", f -> f
                                    .type("plain")
                                    .preTags("<span class='keyword'>")
                                    .postTags("</span>"))
                            .encoder(HighlighterEncoder.Html)), FileSearch.class);
        }
        catch (IOException e) {
            log.error("文件搜索失败：{}", e.getMessage(), e);
        }

        List<SearchFileVO> searchFileVOList = new ArrayList<>();
        final List<Hit<FileSearch>> hits = Objects
                .requireNonNull(search)
                .hits()
                .hits();
        for (Hit<FileSearch> hit : hits) {
            SearchFileVO searchFileVO = BeanCopyUtils.copy(hit.source(), SearchFileVO.class);
            Objects
                    .requireNonNull(searchFileVO)
                    .setHighLight(hit.highlight());
            searchFileVOList.add(searchFileVO);
            asyncTaskComp.checkESUserFileId(searchFileVO.getUserFileId());
        }
        return RestResult
                .<SearchFileVO>success()
                .dataList(searchFileVOList, searchFileVOList.size());
    }

    @Operation(summary = "文件重命名",
               description = "文件重命名",
               tags = {"file"})
    @RequestMapping(value = "/renamefile",
                    method = RequestMethod.POST)
    @MyLog(operation = "文件重命名",
           module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<String> renameFile(
            @RequestBody
                    RenameFileDTO renameFileDto) {

        JwtUser sessionUserBean = SessionUtil.getSession();
        UserFile userFile = userFileService.getById(renameFileDto.getUserFileId());

        List<UserFile> userFiles = userFileService.selectUserFileByNameAndPath(renameFileDto.getFileName(), userFile.getFilePath(), sessionUserBean.getUserId());
        if (userFiles != null && !userFiles.isEmpty()) {
            return RestResult
                    .<String>fail()
                    .message("同名文件已存在");
        }

        LambdaUpdateWrapper<UserFile> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper
                .set(UserFile::getFileName, renameFileDto.getFileName())
                .set(UserFile::getUploadTime, DateUtil.getCurrentTime())
                .eq(UserFile::getUserFileId, renameFileDto.getUserFileId());
        userFileService.update(lambdaUpdateWrapper);
        if (1 == userFile.getIsDir()) {
            List<UserFile> list = userFileService.selectUserFileByLikeRightFilePath(new QiwenFile(userFile.getFilePath(), userFile.getFileName(), true).getPath(),
                    sessionUserBean.getUserId());

            for (UserFile newUserFile : list) {
                String escapedPattern = Pattern.quote(new QiwenFile(userFile.getFilePath(), userFile.getFileName(), userFile.getIsDir() == 1).getPath());
                newUserFile.setFilePath(newUserFile
                        .getFilePath()
                        .replaceFirst(escapedPattern, new QiwenFile(userFile.getFilePath(), renameFileDto.getFileName(), userFile.getIsDir() == 1).getPath()));
                userFileService.updateById(newUserFile);
            }
        }
        fileDealComp.uploadESByUserFileId(renameFileDto.getUserFileId());
        return RestResult.success();
    }

    @Operation(summary = "获取文件列表",
               description = "用来做前台列表展示",
               tags = {"file"})
    @RequestMapping(value = "/getfilelist",
                    method = RequestMethod.GET)
    @ResponseBody
    public RestResult<FileListVO> getFileList(
            @Parameter(description = "文件类型",
                       required = true)
                    String fileType,
            @Parameter(description = "文件路径",
                       required = true)
                    String filePath,
            @Parameter(description = "当前页",
                       required = true)
                    long currentPage,
            @Parameter(description = "页面数量",
                       required = true)
                    long pageCount) {
        if ("0".equals(fileType)) {
            IPage<FileListVO> fileList = userFileService.userFileList(null, filePath, currentPage, pageCount);
            return RestResult
                    .<FileListVO>success()
                    .dataList(fileList.getRecords(), fileList.getTotal());
        }
        else {
            IPage<FileListVO> fileList = userFileService.getFileByFileType(Integer.valueOf(fileType), currentPage, pageCount, SessionUtil
                    .getSession()
                    .getUserId());
            return RestResult
                    .<FileListVO>success()
                    .dataList(fileList.getRecords(), fileList.getTotal());
        }
    }

    @Operation(summary = "批量删除文件",
               description = "批量删除文件",
               tags = {"file"})
    @RequestMapping(value = "/batchdeletefile",
                    method = RequestMethod.POST)
    @MyLog(operation = "批量删除文件",
           module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<String> deleteImageByIds(
            @RequestBody
                    BatchDeleteFileDTO batchDeleteFileDto) {
        String userFileIds = batchDeleteFileDto.getUserFileIds();
        String[] userFileIdList = userFileIds.split(",");
        userFileService.update(new UpdateWrapper<UserFile>()
                .lambda()
                .set(UserFile::getDeleteFlag, FileDeleteFlagEnum.DELETED.getDeleteFlag())
                .in(UserFile::getUserFileId, Arrays.asList(userFileIdList)));
        for (String userFileId : userFileIdList) {
            executor.execute(() -> {
                userFileService.deleteUserFile(userFileId, SessionUtil.getUserId());
            });

            fileDealComp.deleteESByUserFileId(userFileId);
        }

        return RestResult
                .<String>success()
                .message("批量删除文件成功");
    }

    @Operation(summary = "删除文件",
               description = "可以删除文件或者目录",
               tags = {"file"})
    @RequestMapping(value = "/deletefile",
                    method = RequestMethod.POST)
    @MyLog(operation = "删除文件",
           module = CURRENT_MODULE)
    @ResponseBody
    public RestResult deleteFile(
            @RequestBody
                    DeleteFileDTO deleteFileDto) {

        JwtUser sessionUserBean = SessionUtil.getSession();
        userFileService.deleteUserFile(deleteFileDto.getUserFileId(), sessionUserBean.getUserId());
        fileDealComp.deleteESByUserFileId(deleteFileDto.getUserFileId());

        return RestResult.success();

    }

    @Operation(summary = "解压文件",
               description = "解压文件。",
               tags = {"file"})
    @RequestMapping(value = "/unzipfile",
                    method = RequestMethod.POST)
    @MyLog(operation = "解压文件",
           module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<String> unzipFile(
            @RequestBody
                    UnzipFileDTO unzipFileDto) {

        try {
            fileService.unzipFile(unzipFileDto.getUserFileId(), unzipFileDto.getUnzipMode(), unzipFileDto.getFilePath());
        }
        catch (QiwenException e) {
            return RestResult
                    .<String>fail()
                    .message(e.getMessage());
        }

        return RestResult.success();

    }

    @Operation(summary = "文件复制",
               description = "可以复制文件或者目录",
               tags = {"file"})
    @RequestMapping(value = "/copyfile",
                    method = RequestMethod.POST)
    @MyLog(operation = "文件复制",
           module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<String> copyFile(
            @RequestBody
                    CopyFileDTO copyFileDTO) {
        String userId = SessionUtil.getUserId();
        String filePath = copyFileDTO.getFilePath();
        String userFileIds = copyFileDTO.getUserFileIds();
        String[] userFileIdArr = userFileIds.split(",");
        for (String userFileId : userFileIdArr) {
            UserFile userFile = userFileService.getById(userFileId);
            String oldfilePath = userFile.getFilePath();
            String fileName = userFile.getFileName();
            if (userFile.isDirectory()) {
                QiwenFile qiwenFile = new QiwenFile(oldfilePath, fileName, true);
                if (filePath.startsWith(qiwenFile.getPath() + QiwenFile.separator) || filePath.equals(qiwenFile.getPath())) {
                    return RestResult
                            .<String>fail()
                            .message("原路径与目标路径冲突，不能复制");
                }
            }

            userFileService.userFileCopy(SessionUtil.getUserId(), userFileId, filePath);
            fileDealComp.deleteRepeatSubDirFile(filePath, userId);
        }

        return RestResult.success();

    }

    @Operation(summary = "文件移动",
               description = "可以移动文件或者目录",
               tags = {"file"})
    @RequestMapping(value = "/movefile",
                    method = RequestMethod.POST)
    @MyLog(operation = "文件移动",
           module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<String> moveFile(
            @RequestBody
                    MoveFileDTO moveFileDto) {

        JwtUser sessionUserBean = SessionUtil.getSession();
        UserFile userFile = userFileService.getById(moveFileDto.getUserFileId());
        String oldfilePath = userFile.getFilePath();
        String newfilePath = moveFileDto.getFilePath();
        String fileName = userFile.getFileName();
        String extendName = userFile.getExtendName();
        if (StringUtil.isEmpty(extendName)) {
            QiwenFile qiwenFile = new QiwenFile(oldfilePath, fileName, true);
            if (newfilePath.startsWith(qiwenFile.getPath() + QiwenFile.separator) || newfilePath.equals(qiwenFile.getPath())) {
                return RestResult
                        .<String>fail()
                        .message("原路径与目标路径冲突，不能移动");
            }
        }

        userFileService.updateFilepathByUserFileId(moveFileDto.getUserFileId(), newfilePath, sessionUserBean.getUserId());

        fileDealComp.deleteRepeatSubDirFile(newfilePath, sessionUserBean.getUserId());
        return RestResult.success();

    }

    @Operation(summary = "批量移动文件",
               description = "可以同时选择移动多个文件或者目录",
               tags = {"file"})
    @RequestMapping(value = "/batchmovefile",
                    method = RequestMethod.POST)
    @MyLog(operation = "批量移动文件",
           module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<String> batchMoveFile(
            @RequestBody
                    BatchMoveFileDTO batchMoveFileDto) {

        JwtUser sessionUserBean = SessionUtil.getSession();

        String newfilePath = batchMoveFileDto.getFilePath();

        String userFileIds = batchMoveFileDto.getUserFileIds();
        String[] userFileIdArr = userFileIds.split(",");

        for (String userFileId : userFileIdArr) {
            UserFile userFile = userFileService.getById(userFileId);
            if (StringUtil.isEmpty(userFile.getExtendName())) {
                QiwenFile qiwenFile = new QiwenFile(userFile.getFilePath(), userFile.getFileName(), true);
                if (newfilePath.startsWith(qiwenFile.getPath() + QiwenFile.separator) || newfilePath.equals(qiwenFile.getPath())) {
                    return RestResult
                            .<String>fail()
                            .message("原路径与目标路径冲突，不能移动");
                }
            }
            userFileService.updateFilepathByUserFileId(userFile.getUserFileId(), newfilePath, sessionUserBean.getUserId());
        }

        return RestResult
                .<String>success()
                .data("批量移动文件成功");

    }

    @Operation(summary = "获取文件树",
               description = "文件移动的时候需要用到该接口，用来展示目录树",
               tags = {"file"})
    @RequestMapping(value = "/getfiletree",
                    method = RequestMethod.GET)
    @ResponseBody
    public RestResult<TreeNode> getFileTree() {
        RestResult<TreeNode> result = new RestResult<TreeNode>();

        JwtUser sessionUserBean = SessionUtil.getSession();

        List<UserFile> userFileList = userFileService.selectFilePathTreeByUserId(sessionUserBean.getUserId());
        TreeNode resultTreeNode = new TreeNode();
        resultTreeNode.setLabel(QiwenFile.separator);
        resultTreeNode.setId(0L);
        long id = 1;
        for (int i = 0; i < userFileList.size(); i++) {
            UserFile userFile = userFileList.get(i);
            QiwenFile qiwenFile = new QiwenFile(userFile.getFilePath(), userFile.getFileName(), false);
            String filePath = qiwenFile.getPath();

            Queue<String> queue = new LinkedList<>();

            String[] strArr = filePath.split(QiwenFile.separator);
            for (int j = 0; j < strArr.length; j++) {
                if (!"".equals(strArr[j]) && strArr[j] != null) {
                    queue.add(strArr[j]);
                }

            }
            if (queue.size() == 0) {
                continue;
            }

            resultTreeNode = fileDealComp.insertTreeNode(resultTreeNode, id++, QiwenFile.separator, queue);

        }
        List<TreeNode> treeNodeList = resultTreeNode.getChildren();
        Collections.sort(treeNodeList, (o1, o2) -> {
            long i = o1.getId() - o2.getId();
            return (int) i;
        });
        result.setSuccess(true);
        result.setData(resultTreeNode);
        return result;

    }

    @Operation(summary = "修改文件",
               description = "支持普通文本类文件的修改",
               tags = {"file"})
    @RequestMapping(value = "/update",
                    method = RequestMethod.POST)
    @ResponseBody
    public RestResult<String> updateFile(
            @RequestBody
                    UpdateFileDTO updateFileDTO) {
        JwtUser sessionUserBean = SessionUtil.getSession();
        UserFile userFile = userFileService.getById(updateFileDTO.getUserFileId());
        FileBean fileBean = fileService.getById(userFile.getFileId());
        Long pointCount = fileService.getFilePointCount(userFile.getFileId());
        String fileUrl = fileBean.getFileUrl();
        if (pointCount > 1) {
            fileUrl = fileDealComp.copyFile(fileBean, userFile);
        }
        String content = updateFileDTO.getFileContent();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content.getBytes());
        try {
            int fileSize = byteArrayInputStream.available();
            fileDealComp.saveFileInputStream(fileBean.getStorageType(), fileUrl, byteArrayInputStream);

            String md5Str = fileDealComp.getIdentifierByFile(fileUrl, fileBean.getStorageType());

            fileService.updateFileDetail(userFile.getUserFileId(), md5Str, fileSize);

        }
        catch (Exception e) {
            throw new QiwenException(999999, "修改文件异常");
        }
        finally {
            try {
                byteArrayInputStream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return RestResult
                .<String>success()
                .message("修改文件成功");
    }

    @Operation(summary = "查询文件详情",
               description = "查询文件详情",
               tags = {"file"})
    @RequestMapping(value = "/detail",
                    method = RequestMethod.GET)
    @ResponseBody
    public RestResult<FileDetailVO> queryFileDetail(
            @Parameter(description = "用户文件Id",
                       required = true)
                    String userFileId) {
        FileDetailVO vo = fileService.getFileDetail(userFileId);
        return RestResult
                .<FileDetailVO>success()
                .data(vo);
    }

}
