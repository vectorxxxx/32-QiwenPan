package com.qiwenshare.file.controller;

import com.qiwenshare.common.anno.MyLog;
import com.qiwenshare.common.result.RestResult;
import com.qiwenshare.common.util.MimeUtils;
import com.qiwenshare.common.util.security.JwtUser;
import com.qiwenshare.common.util.security.SessionUtil;
import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.api.IFiletransferService;
import com.qiwenshare.file.api.IUserFileService;
import com.qiwenshare.file.component.FileDealComp;
import com.qiwenshare.file.domain.FileBean;
import com.qiwenshare.file.domain.StorageBean;
import com.qiwenshare.file.domain.UserFile;
import com.qiwenshare.file.dto.file.BatchDownloadFileDTO;
import com.qiwenshare.file.dto.file.DownloadFileDTO;
import com.qiwenshare.file.dto.file.PreviewDTO;
import com.qiwenshare.file.dto.file.UploadFileDTO;
import com.qiwenshare.file.io.QiwenFile;
import com.qiwenshare.file.service.StorageService;
import com.qiwenshare.file.vo.file.UploadFileVo;
import com.qiwenshare.ufop.factory.UFOPFactory;
import com.qiwenshare.ufop.operation.download.Downloader;
import com.qiwenshare.ufop.operation.download.domain.DownloadFile;
import com.qiwenshare.ufop.operation.download.domain.Range;
import com.qiwenshare.ufop.util.UFOPUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "filetransfer",
     description = "该接口为文件传输接口，主要用来做文件的上传、下载和预览")
@RestController
@RequestMapping("/filetransfer")
public class FiletransferController
{

    @Resource
    private IFiletransferService filetransferService;

    @Resource
    private IFileService fileService;
    @Resource
    private IUserFileService userFileService;
    @Resource
    private FileDealComp fileDealComp;
    @Resource
    private StorageService storageService;
    @Resource
    private UFOPFactory ufopFactory;

    public static final String CURRENT_MODULE = "文件传输接口";

    @Operation(summary = "极速上传",
               description = "校验文件MD5判断文件是否存在，如果存在直接上传成功并返回skipUpload=true，如果不存在返回skipUpload=false需要再次调用该接口的POST方法",
               tags = {"filetransfer"})
    @RequestMapping(value = "/uploadfile",
                    method = RequestMethod.GET)
    @MyLog(operation = "极速上传",
           module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<UploadFileVo> uploadFileSpeed(UploadFileDTO uploadFileDto) {
        boolean isCheckSuccess = storageService.checkStorage(SessionUtil.getUserId(), uploadFileDto.getTotalSize());
        if (!isCheckSuccess) {
            return RestResult
                    .<UploadFileVo>fail()
                    .message("存储空间不足");
        }
        UploadFileVo uploadFileVo = filetransferService.uploadFileSpeed(uploadFileDto);
        return RestResult
                .<UploadFileVo>success()
                .data(uploadFileVo);
    }

    @Operation(summary = "上传文件",
               description = "真正的上传文件接口",
               tags = {"filetransfer"})
    @RequestMapping(value = "/uploadfile",
                    method = RequestMethod.POST)
    @MyLog(operation = "上传文件",
           module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<UploadFileVo> uploadFile(HttpServletRequest request, UploadFileDTO uploadFileDto) {
        filetransferService.uploadFile(request, uploadFileDto, SessionUtil.getUserId());

        UploadFileVo uploadFileVo = new UploadFileVo();
        return RestResult
                .<UploadFileVo>success()
                .data(uploadFileVo);
    }

    @Operation(summary = "下载文件",
               description = "下载文件接口",
               tags = {"filetransfer"})
    @MyLog(operation = "下载文件",
           module = CURRENT_MODULE)
    @RequestMapping(value = "/downloadfile",
                    method = RequestMethod.GET)
    public void downloadFile(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, DownloadFileDTO downloadFileDTO) {
        // 获取请求中的所有Cookie
        Cookie[] cookieArr = httpServletRequest.getCookies();
        String token = "";
        if (ArrayUtils.isNotEmpty(cookieArr)) {
            // 从Cookie数组中查找名为"token"的Cookie
            token = Arrays
                    .stream(cookieArr)
                    .filter(cookie -> "token".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse("");
        }
        // 判断是否有下载权限
        boolean authResult = fileDealComp.checkAuthDownloadAndPreview(downloadFileDTO.getShareBatchNum(), downloadFileDTO.getExtractionCode(), token,
                downloadFileDTO.getUserFileId(), null);
        if (!authResult) {
            log.error("没有权限下载！！！");
            return;
        }

        // 获取用户文件信息
        UserFile userFile = userFileService.getById(downloadFileDTO.getUserFileId());
        // 获取文件名
        String fileName = "";
        if (userFile.isDirectory()) {
            fileName = userFile
                    .getFileName()
                    .concat(".zip");
        }
        else {
            fileName = userFile
                    .getFileName()
                    .concat(".")
                    .concat(userFile.getExtendName());
        }
        fileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);

        // 设置内容类型、响应头
        // 设置强制下载不打开
        httpServletResponse.setContentType("application/force-download");
        // 设置文件名
        httpServletResponse.addHeader("Content-Disposition", "attachment;fileName=".concat(fileName));

        filetransferService.downloadFile(httpServletResponse, downloadFileDTO);
    }

    @Operation(summary = "批量下载文件",
               description = "批量下载文件",
               tags = {"filetransfer"})
    @RequestMapping(value = "/batchDownloadFile",
                    method = RequestMethod.GET)
    @MyLog(operation = "批量下载文件",
           module = CURRENT_MODULE)
    @ResponseBody
    public void batchDownloadFile(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, BatchDownloadFileDTO batchDownloadFileDTO) {
        Cookie[] cookieArr = httpServletRequest.getCookies();
        String token = "";
        if (ArrayUtils.isNotEmpty(cookieArr)) {
            // 从Cookie数组中查找名为"token"的Cookie
            token = Arrays
                    .stream(cookieArr)
                    .filter(cookie -> "token".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse("");
        }

        // 判断是否有下载权限
        boolean authResult = fileDealComp.checkAuthDownloadAndPreview(batchDownloadFileDTO.getShareBatchNum(), batchDownloadFileDTO.getExtractionCode(), token,
                batchDownloadFileDTO.getUserFileIds(), null);
        if (!authResult) {
            log.error("没有权限下载！！！");
            return;
        }

        String files = batchDownloadFileDTO.getUserFileIds();
        String[] userFileIdStrs = files.split(",");
        List<String> userFileIds = new ArrayList<>();
        for (String userFileId : userFileIdStrs) {
            // 获取用户文件信息
            UserFile userFile = userFileService.getById(userFileId);
            // 1、文件
            if (userFile.isFile()) {
                userFileIds.add(userFileId);
            }
            // 2、文件夹
            else {
                // 获取文件夹下的所有文件
                QiwenFile qiwenFile = new QiwenFile(userFile.getFilePath(), userFile.getFileName(), true);
                List<String> userFileIds1 = userFileService
                        .selectUserFileByLikeRightFilePath(qiwenFile.getPath(), userFile.getUserId())
                        .stream()
                        .map(UserFile::getUserFileId)
                        .collect(Collectors.toList());
                userFileIds.add(userFile.getUserFileId());
                userFileIds.addAll(userFileIds1);
            }

        }

        UserFile userFile = userFileService.getById(userFileIdStrs[0]);
        Date date = new Date();
        String fileName = String.valueOf(date.getTime());

        // 设置内容类型、响应头
        httpServletResponse.setContentType("application/force-download");// 设置强制下载不打开
        httpServletResponse.addHeader("Content-Disposition", "attachment;fileName="
                .concat(fileName)
                .concat(".zip"));// 设置文件名
        filetransferService.downloadUserFileList(httpServletResponse, userFile.getFilePath(), fileName, userFileIds);
    }

    @Operation(summary = "预览文件",
               description = "用于文件预览",
               tags = {"filetransfer"})
    @GetMapping("/preview")
    public void preview(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, PreviewDTO previewDTO) throws IOException {
        // 图片预览
        if (Objects.nonNull(previewDTO.getPlatform()) && previewDTO.getPlatform() == 2) {
            filetransferService.previewPictureFile(httpServletResponse, previewDTO);
            return;
        }

        // 获取token
        String token = "";
        if (StringUtils.isNotEmpty(previewDTO.getToken())) {
            token = previewDTO.getToken();
        }
        else {
            Cookie[] cookieArr = httpServletRequest.getCookies();
            if (ArrayUtils.isNotEmpty(cookieArr)) {
                // 从Cookie数组中查找名为"token"的Cookie
                token = Arrays
                        .stream(cookieArr)
                        .filter(cookie -> "token".equals(cookie.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse("");
            }
        }

        // 判断是否有下载权限
        UserFile userFile = userFileService.getById(previewDTO.getUserFileId());
        boolean authResult = fileDealComp.checkAuthDownloadAndPreview(previewDTO.getShareBatchNum(), previewDTO.getExtractionCode(), token, previewDTO.getUserFileId(),
                previewDTO.getPlatform());
        if (!authResult) {
            log.error("没有权限预览！！！");
            return;
        }

        // 设置响应头：文件名、文件类型、缓存控制
        String fileName = userFile
                .getFileName()
                .concat(".")
                .concat(userFile.getExtendName());
        fileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        String mime = MimeUtils.getMime(userFile.getExtendName());
        httpServletResponse.addHeader("Content-Disposition", "fileName=".concat(fileName));// 设置文件名
        httpServletResponse.setHeader("Content-Type", mime);
        if (UFOPUtils.isImageFile(userFile.getExtendName())) {
            httpServletResponse.setHeader("cache-control", "public");
        }

        // 获取文件信息
        FileBean fileBean = fileService.getById(userFile.getFileId());
        // 视频、音频、flac
        if (UFOPUtils.isVideoFile(userFile.getExtendName()) || "mp3".equalsIgnoreCase(userFile.getExtendName()) || "flac".equalsIgnoreCase(userFile.getExtendName())) {
            //获取从那个字节开始读取文件
            String rangeString = httpServletRequest.getHeader("Range");
            int start = 0;
            if (StringUtils.isNotBlank(rangeString)) {
                start = Integer.parseInt(rangeString.substring(rangeString.indexOf("=") + 1, rangeString.indexOf("-")));
            }

            // 获取下载器
            Downloader downloader = ufopFactory.getDownloader(fileBean.getStorageType());
            DownloadFile downloadFile = new DownloadFile();
            downloadFile.setFileUrl(fileBean.getFileUrl());
            // 设置Range
            Range range = new Range();
            range.setStart(start);
            if (start + 1024 * 1024 * 1 >= fileBean
                    .getFileSize()
                    .intValue()) {
                range.setLength(fileBean
                        .getFileSize()
                        .intValue() - start);
            }
            else {
                range.setLength(1024 * 1024 * 1);
            }
            downloadFile.setRange(range);

            // 获取文件流
            try (InputStream inputStream = downloader.getInputStream(downloadFile);
                 OutputStream outputStream = httpServletResponse.getOutputStream()) {

                // 返回码需要为206，代表只处理了部分请求，响应了部分数据
                httpServletResponse.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

                // 每次请求只返回1MB的视频流
                httpServletResponse.setHeader("Accept-Ranges", "bytes");
                // 设置此次相应返回的数据范围
                httpServletResponse.setHeader("Content-Range", "bytes " + start + "-" + (fileBean.getFileSize() - 1) + "/" + fileBean.getFileSize());

                IOUtils.copy(inputStream, outputStream);
            }
            finally {
                if (Objects.nonNull(downloadFile.getOssClient())) {
                    downloadFile
                            .getOssClient()
                            .shutdown();
                }
            }

        }
        else {
            filetransferService.previewFile(httpServletResponse, previewDTO);
        }

    }

    @Operation(summary = "获取存储信息",
               description = "获取存储信息",
               tags = {"filetransfer"})
    @RequestMapping(value = "/getstorage",
                    method = RequestMethod.GET)
    @ResponseBody
    public RestResult<StorageBean> getStorage() {

        JwtUser sessionUserBean = SessionUtil.getSession();
        StorageBean storageBean = new StorageBean();

        storageBean.setUserId(sessionUserBean.getUserId());

        Long storageSize = filetransferService.selectStorageSizeByUserId(sessionUserBean.getUserId());
        StorageBean storage = new StorageBean();
        storage.setUserId(sessionUserBean.getUserId());
        storage.setStorageSize(storageSize);
        Long totalStorageSize = storageService.getTotalStorageSize(sessionUserBean.getUserId());
        storage.setTotalStorageSize(totalStorageSize);
        return RestResult
                .<StorageBean>success()
                .data(storage);

    }

}
