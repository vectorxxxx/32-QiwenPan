package com.qiwenshare.file.component;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiwenshare.common.util.DateUtil;
import com.qiwenshare.common.util.MusicUtils;
import com.qiwenshare.common.util.security.SessionUtil;
import com.qiwenshare.file.api.IShareFileService;
import com.qiwenshare.file.api.IShareService;
import com.qiwenshare.file.api.IUserService;
import com.qiwenshare.file.config.es.FileSearch;
import com.qiwenshare.file.constant.FileDeleteFlagEnum;
import com.qiwenshare.file.constant.FileDirEnum;
import com.qiwenshare.file.domain.FileBean;
import com.qiwenshare.file.domain.Music;
import com.qiwenshare.file.domain.Share;
import com.qiwenshare.file.domain.ShareFile;
import com.qiwenshare.file.domain.UserFile;
import com.qiwenshare.file.io.QiwenFile;
import com.qiwenshare.file.mapper.FileMapper;
import com.qiwenshare.file.mapper.MusicMapper;
import com.qiwenshare.file.mapper.UserFileMapper;
import com.qiwenshare.file.util.QiwenFileUtil;
import com.qiwenshare.file.util.TreeNode;
import com.qiwenshare.ufop.factory.UFOPFactory;
import com.qiwenshare.ufop.operation.copy.Copier;
import com.qiwenshare.ufop.operation.copy.domain.CopyFile;
import com.qiwenshare.ufop.operation.download.Downloader;
import com.qiwenshare.ufop.operation.download.domain.DownloadFile;
import com.qiwenshare.ufop.operation.write.Writer;
import com.qiwenshare.ufop.operation.write.domain.WriteFile;
import com.qiwenshare.ufop.util.UFOPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.flac.FlacFileReader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.framebody.FrameBodyAPIC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 文件逻辑处理组件
 */
@Slf4j
@Component
public class FileDealComp
{
    @Resource
    private UserFileMapper userFileMapper;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private IUserService userService;
    @Resource
    private IShareService shareService;
    @Resource
    private IShareFileService shareFileService;
    @Resource
    private UFOPFactory ufopFactory;
    @Resource
    private MusicMapper musicMapper;
    @Autowired
    private ElasticsearchClient elasticsearchClient;

    public static Executor exec = Executors.newFixedThreadPool(20);

    /**
     * 获取重复文件名
     * <p>
     * 场景1: 文件还原时，在 savefilePath 路径下，保存 测试.txt 文件重名，则会生成 测试(1).txt 场景2： 上传文件时，在 savefilePath 路径下，保存 测试.txt 文件重名，则会生成 测试(1).txt
     *
     * @param userFile
     * @param savefilePath
     * @return
     */
    public String getRepeatFileName(UserFile userFile, String savefilePath) {
        final String fileName = userFile.getFileName();
        final String extendName = userFile.getExtendName();
        final String userId = userFile.getUserId();
        final int isDir = userFile.getIsDir();

        // 查询当前用户下，文件名、扩展名、是否是目录、删除标识为0的文件
        List<UserFile> list = userFileMapper.selectList(new LambdaQueryWrapper<UserFile>()
                .eq(UserFile::getUserId, userId)
                .eq(UserFile::getFilePath, savefilePath)
                .eq(UserFile::getFileName, fileName)
                .eq(userFile.isFile(), UserFile::getExtendName, extendName)
                .eq(UserFile::getIsDir, isDir)
                .eq(UserFile::getDeleteFlag, FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag()));
        if (CollectionUtils.isEmpty(list)) {
            return fileName;
        }

        int i = 0;
        while (CollectionUtils.isNotEmpty(list)) {
            i++;
            list = userFileMapper.selectList(new LambdaQueryWrapper<UserFile>()
                    .eq(UserFile::getUserId, userId)
                    .eq(UserFile::getFilePath, savefilePath)
                    .eq(UserFile::getFileName, fileName + "(" + i + ")")
                    .eq(userFile.isFile(), UserFile::getExtendName, extendName)
                    .eq(UserFile::getIsDir, isDir)
                    .eq(UserFile::getDeleteFlag, FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag()));
        }

        return fileName
                .concat("(")
                .concat(String.valueOf(i))
                .concat(")");
    }

    /**
     * 还原父文件路径
     * <p>
     * 1、回收站文件还原操作会将文件恢复到原来的路径下,当还原文件的时候，如果父目录已经不存在了，则需要把父母录给还原 2、上传目录
     *
     * @param sessionUserId
     */
    public void restoreParentFilePath(QiwenFile qiwenFile, String sessionUserId) {
        // 如果是文件，则获取父目录
        if (qiwenFile.isFile()) {
            qiwenFile = qiwenFile.getParentFile();
        }

        // 循环判断父目录是否存在
        while (StringUtils.isNotEmpty(qiwenFile.getParent())) {
            final String fileName = qiwenFile.getName();
            final String parentFilePath = qiwenFile.getParent();

            // 根据文件路径和文件名查询用户文件
            final List<UserFile> userFileList = userFileMapper.selectList(new LambdaQueryWrapper<UserFile>()
                    .eq(UserFile::getUserId, sessionUserId)
                    .eq(UserFile::getFilePath, parentFilePath)
                    .eq(UserFile::getFileName, fileName)
                    .eq(UserFile::getDeleteFlag, FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag())
                    .eq(UserFile::getIsDir, FileDirEnum.DIR.getType()));

            // 如果用户文件不存在，则插入
            if (CollectionUtils.isEmpty(userFileList)) {
                UserFile userFile = QiwenFileUtil.getQiwenDir(sessionUserId, parentFilePath, fileName);
                try {
                    userFileMapper.insert(userFile);
                }
                catch (Exception e) {
                    //ignore
                }
            }
            qiwenFile = new QiwenFile(parentFilePath, true);
        }
    }

    /**
     * 删除重复的子目录文件
     * <p>
     * 当还原目录的时候，如果其子目录在文件系统中已存在，则还原之后进行去重操作
     *
     * @param filePath
     * @param sessionUserId
     */
    public void deleteRepeatSubDirFile(String filePath, String sessionUserId) {
        log.debug("删除子目录：" + filePath);

        // 获取重复的子目录
        final LambdaQueryWrapper<UserFile> userFileLambdaQueryWrapper = new LambdaQueryWrapper<>();
        List<UserFile> repeatList = userFileMapper.selectList(userFileLambdaQueryWrapper
                .select(UserFile::getFileName, UserFile::getFilePath)
                .eq(UserFile::getUserId, sessionUserId)
                .eq(UserFile::getIsDir, FileDirEnum.DIR.getType())
                .eq(UserFile::getDeleteFlag, FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag())
                .likeRight(UserFile::getFilePath, QiwenFileUtil.formatLikePath(filePath))
                .groupBy(UserFile::getFilePath, UserFile::getFileName)
                .having("count(fileName) >= 2"));

        // 删除重复的子目录
        for (UserFile userFile : repeatList) {
            List<UserFile> userFiles = userFileMapper.selectList(userFileLambdaQueryWrapper
                    .eq(UserFile::getFilePath, userFile.getFilePath())
                    .eq(UserFile::getFileName, userFile.getFileName())
                    .eq(UserFile::getDeleteFlag, FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag()));
            for (int i = 0; i < userFiles.size() - 1; i++) {
                userFileMapper.deleteById(userFiles
                        .get(i)
                        .getUserFileId());
            }
        }
    }

    /**
     * 组织一个树目录节点，文件移动的时候使用
     *
     * @param treeNode
     * @param id
     * @param filePath
     * @param nodeNameQueue
     * @return
     */
    public TreeNode insertTreeNode(TreeNode treeNode, long id, String filePath, Queue<String> nodeNameQueue) {

        List<TreeNode> childrenTreeNodes = treeNode.getChildren();
        String currentNodeName = nodeNameQueue.peek();
        if (StringUtils.isEmpty(currentNodeName)) {
            return treeNode;
        }

        QiwenFile qiwenFile = new QiwenFile(filePath, currentNodeName, true);
        filePath = qiwenFile.getPath();

        if (!isExistPath(childrenTreeNodes, currentNodeName)) {  //1、判断有没有该子节点，如果没有则插入
            //插入
            TreeNode resultTreeNode = new TreeNode();

            resultTreeNode.setFilePath(filePath);
            resultTreeNode.setLabel(nodeNameQueue.poll());
            resultTreeNode.setId(++id);

            childrenTreeNodes.add(resultTreeNode);

        }
        else {  //2、如果有，则跳过
            nodeNameQueue.poll();
        }

        if (nodeNameQueue.size() != 0) {
            for (int i = 0; i < childrenTreeNodes.size(); i++) {

                TreeNode childrenTreeNode = childrenTreeNodes.get(i);
                if (currentNodeName.equals(childrenTreeNode.getLabel())) {
                    childrenTreeNode = insertTreeNode(childrenTreeNode, id * 10, filePath, nodeNameQueue);
                    childrenTreeNodes.remove(i);
                    childrenTreeNodes.add(childrenTreeNode);
                    treeNode.setChildren(childrenTreeNodes);
                }

            }
        }
        else {
            treeNode.setChildren(childrenTreeNodes);
        }

        return treeNode;

    }

    /**
     * 判断该路径在树节点中是否已经存在
     *
     * @param childrenTreeNodes
     * @param path
     * @return
     */
    public boolean isExistPath(List<TreeNode> childrenTreeNodes, String path) {
        boolean isExistPath = false;

        try {
            for (TreeNode childrenTreeNode : childrenTreeNodes) {
                if (path.equals(childrenTreeNode.getLabel())) {
                    isExistPath = true;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return isExistPath;
    }

    public void uploadESByUserFileId(String userFileId) {
        exec.execute(() -> {
            try {
                UserFile userfile = userFileMapper.selectOne(new LambdaQueryWrapper<UserFile>().eq(UserFile::getUserFileId, userFileId));
                if (Objects.nonNull(userfile)) {
                    FileSearch fileSearch = new FileSearch();
                    BeanUtil.copyProperties(userfile, fileSearch);
                /*if (fileSearch.getIsDir() == 0) {

                    Reader reader = ufopFactory.getReader(fileSearch.getStorageType());
                    ReadFile readFile = new ReadFile();
                    readFile.setFileUrl(fileSearch.getFileUrl());
                    String content = reader.read(readFile);
                    //全文搜索
                    fileSearch.setContent(content);

                }*/
                    elasticsearchClient.index(i -> i
                            .index("filesearch")
                            .id(fileSearch.getUserFileId())
                            .document(fileSearch));
                }
            }
            catch (Exception e) {
                log.debug("ES更新操作失败，请检查配置");
            }
        });
    }

    public void deleteESByUserFileId(String userFileId) {
        exec.execute(() -> {
            try {
                elasticsearchClient.delete(d -> d
                        .index("filesearch")
                        .id(userFileId));
            }
            catch (Exception e) {
                log.debug("ES删除操作失败，请检查配置");
            }
        });

    }

    /**
     * 根据用户传入的参数，判断是否有下载或者预览权限
     *
     * @return
     */
    public boolean checkAuthDownloadAndPreview(String shareBatchNum, String extractionCode, String token, String userFileIds, Integer platform) {
        log.debug("权限检查开始：shareBatchNum:{}, extractionCode:{}, token:{}, userFileIds{}", shareBatchNum, extractionCode, token, userFileIds);
        if (Objects.nonNull(platform) && platform == 2) {
            return true;
        }
        final String[] userFileIdArr = userFileIds.split(",");
        for (String userFileId : userFileIdArr) {
            // 获取用户文件信息
            UserFile userFile = userFileMapper.selectById(userFileId);
            log.debug(JSON.toJSONString(userFile));

            // 1、分享批次号为空
            if ("undefined".equals(shareBatchNum) || StringUtils.isEmpty(shareBatchNum)) {
                // 根据token获取用户id
                String userId = userService.getUserIdByToken(token);
                log.debug(JSON.toJSONString("当前登录session用户id：" + userId));
                // 判断用户id是否为空
                if (StringUtils.isEmpty(userId)) {
                    return false;
                }
                log.debug("文件所属用户id：" + userFile.getUserId());
                log.debug("登录用户id:" + userId);
                // 判断用户id是否一致
                if (!userFile
                        .getUserId()
                        .equals(userId)) {
                    log.info("用户id不一致，权限校验失败");
                    return false;
                }
            }
            // 2、分享批次号不为空
            else {
                // 根据批次号查询分享信息
                List<Share> shareList = shareService.list(new LambdaQueryWrapper<Share>().eq(Share::getShareBatchNum, shareBatchNum));
                // 批次号不存在
                if (shareList.size() <= 0) {
                    log.info("分享批次号不存在，权限校验失败");
                    return false;
                }

                // 判断分享类型
                Integer shareType = shareList
                        .get(0)
                        .getShareType();
                if (1 == shareType) {
                    // 判断提取码是否匹配
                    if (!shareList
                            .get(0)
                            .getExtractionCode()
                            .equals(extractionCode)) {
                        log.info("提取码错误，权限校验失败");
                        return false;
                    }
                }

                // 判断用户id和分享批次号是否匹配
                List<ShareFile> shareFileList = shareFileService.list(new LambdaQueryWrapper<ShareFile>().eq(ShareFile::getUserFileId, userFileId));
                if (shareFileList.size() <= 0) {
                    log.info("用户id和分享批次号不匹配，权限校验失败");
                    return false;
                }

            }

        }
        return true;
    }

    /**
     * 拷贝文件 场景：修改的文件被多处引用时，需要重新拷贝一份，然后在新的基础上修改
     *
     * @param fileBean
     * @param userFile
     * @return
     */
    public String copyFile(FileBean fileBean, UserFile userFile) {
        Copier copier = ufopFactory.getCopier();
        Downloader downloader = ufopFactory.getDownloader(fileBean.getStorageType());
        DownloadFile downloadFile = new DownloadFile();
        downloadFile.setFileUrl(fileBean.getFileUrl());
        CopyFile copyFile = new CopyFile();
        copyFile.setExtendName(userFile.getExtendName());
        String fileUrl = copier.copy(downloader.getInputStream(downloadFile), copyFile);
        if (Objects.nonNull(downloadFile.getOssClient())) {
            downloadFile
                    .getOssClient()
                    .shutdown();
        }
        fileBean.setFileUrl(fileUrl);
        fileBean.setFileId(IdUtil.getSnowflakeNextIdStr());
        fileMapper.insert(fileBean);
        userFile.setFileId(fileBean.getFileId());
        userFile.setUploadTime(DateUtil.getCurrentTime());
        userFile.setModifyTime(DateUtil.getCurrentTime());
        userFile.setModifyUserId(SessionUtil.getUserId());
        userFileMapper.updateById(userFile);
        return fileUrl;
    }

    public String getIdentifierByFile(String fileUrl, int storageType) throws IOException {
        DownloadFile downloadFile = new DownloadFile();
        downloadFile.setFileUrl(fileUrl);
        InputStream inputStream = ufopFactory
                .getDownloader(storageType)
                .getInputStream(downloadFile);
        return DigestUtils.md5Hex(inputStream);
    }

    public void saveFileInputStream(int storageType, String fileUrl, InputStream inputStream) throws IOException {
        Writer writer1 = ufopFactory.getWriter(storageType);
        WriteFile writeFile = new WriteFile();
        writeFile.setFileUrl(fileUrl);
        int fileSize = inputStream.available();
        writeFile.setFileSize(fileSize);
        writer1.write(inputStream, writeFile);
    }

    public boolean isDirExist(String fileName, String filePath, String userId) {
        final Long count = userFileMapper.selectCount(new LambdaQueryWrapper<UserFile>()
                .eq(UserFile::getFileName, fileName)
                .eq(UserFile::getFilePath, QiwenFile.formatPath(filePath))
                .eq(UserFile::getUserId, userId)
                .eq(UserFile::getDeleteFlag, FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag())
                .eq(UserFile::getIsDir, FileDirEnum.DIR.getType()));
        return count > 0;
    }

    public void parseMusicFile(String extendName, int storageType, String fileUrl, String fileId) {
        File outFile = null;
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            if ("mp3".equalsIgnoreCase(extendName) || "flac".equalsIgnoreCase(extendName)) {
                /** 通过下载器根据文件链接下载文件并保存为临时文件，并生成与之对应的Music对象。 **/
                // 创建DownloadFile对象，用于配置下载的相关信息
                DownloadFile downloadFile = new DownloadFile();
                // 设置文件的下载链接
                downloadFile.setFileUrl(fileUrl);
                inputStream = ufopFactory
                        // 根据存储类型获取下载器实例
                        .getDownloader(storageType)
                        // 通过下载器获取文件的输入流
                        .getInputStream(downloadFile);
                // 根据文件链接获取临时文件对象
                outFile = UFOPUtils.getTempFile(fileUrl);
                // 如果临时文件不存在，则创建新文件
                if (!outFile.exists()) {
                    outFile.createNewFile();
                }
                // 创建文件输出流，用于将下载的文件内容写入到临时文件中
                fileOutputStream = new FileOutputStream(outFile);
                // 将输入流中的内容复制到输出流，完成文件下载
                IOUtils.copy(inputStream, fileOutputStream);
                // 创建Music对象，用于存储音乐相关信息
                Music music = new Music()
                        // 设置音乐ID，使用雪花算法生成的唯一ID
                        .setMusicId(IdUtil.getSnowflakeNextIdStr())
                        // 设置音乐对应的文件ID
                        .setFileId(fileId);

                Tag tag;
                AudioHeader audioHeader = null;
                // 检查文件扩展名是否为mp3，以进行相应的处理
                if ("mp3".equalsIgnoreCase(extendName)) {
                    // 读取MP3文件以获取标签和音频信息
                    MP3File f = (MP3File) AudioFileIO.read(outFile);
                    tag = f.getTag();
                    audioHeader = f.getAudioHeader();
                    // 检查是否存在ID3v2标签，如果存在，则尝试获取专辑封面
                    MP3File mp3file = new MP3File(outFile);
                    if (mp3file.hasID3v2Tag()) {
                        AbstractID3v2Tag id3v2Tag = mp3file.getID3v2TagAsv24();
                        AbstractID3v2Frame frame = (AbstractID3v2Frame) id3v2Tag.getFrame("APIC");
                        if (Objects.nonNull(frame) && !frame.isEmpty()) {
                            FrameBodyAPIC body = (FrameBodyAPIC) frame.getBody();
                            byte[] imageData = body.getImageData();
                            music.setAlbumImage(Base64
                                    .getEncoder()
                                    .encodeToString(imageData));
                        }
                        // 设置MP3文件的元数据，如艺术家、标题、专辑等
                        if (Objects.nonNull(tag)) {
                            music
                                    .setArtist(tag.getFirst(FieldKey.ARTIST))
                                    .setTitle(tag.getFirst(FieldKey.TITLE))
                                    .setAlbum(tag.getFirst(FieldKey.ALBUM))
                                    .setYear(tag.getFirst(FieldKey.YEAR));
                            try {
                                music.setTrack(tag.getFirst(FieldKey.TRACK));
                            }
                            catch (Exception e) {
                                // ignore
                            }

                            music
                                    .setGenre(tag.getFirst(FieldKey.GENRE))
                                    .setComment(tag.getFirst(FieldKey.COMMENT))
                                    .setLyrics(tag.getFirst(FieldKey.LYRICS))
                                    .setComposer(tag.getFirst(FieldKey.COMPOSER))
                                    .setAlbumArtist(tag.getFirst(FieldKey.ALBUM_ARTIST))
                                    .setEncoder(tag.getFirst(FieldKey.ENCODER));
                        }
                    }
                }
                // 检查文件扩展名是否为flac，以进行相应的处理
                else if ("flac".equalsIgnoreCase(extendName)) {
                    // 读取FLAC文件以获取标签和音频信息
                    AudioFile f = new FlacFileReader().read(outFile);
                    tag = f.getTag();
                    audioHeader = f.getAudioHeader();
                    // 设置FLAC文件的元数据，如艺术家、标题、专辑等，并处理专辑封面
                    if (Objects.nonNull(tag)) {
                        music
                                .setArtist(StringUtils.join(tag.getFields(FieldKey.ARTIST), ","))
                                .setTitle(StringUtils.join(tag.getFields(FieldKey.TITLE), ","))
                                .setAlbum(StringUtils.join(tag.getFields(FieldKey.ALBUM), ","))
                                .setYear(StringUtils.join(tag.getFields(FieldKey.YEAR), ","))
                                .setTrack(StringUtils.join(tag.getFields(FieldKey.TRACK), ","))
                                .setGenre(StringUtils.join(tag.getFields(FieldKey.GENRE), ","))
                                .setComment(StringUtils.join(tag.getFields(FieldKey.COMMENT), ","))
                                .setLyrics(StringUtils.join(tag.getFields(FieldKey.LYRICS), ","))
                                .setComposer(StringUtils.join(tag.getFields(FieldKey.COMPOSER), ","))
                                .setAlbumArtist(StringUtils.join(tag.getFields(FieldKey.ALBUM_ARTIST), ","))
                                .setEncoder(StringUtils.join(tag.getFields(FieldKey.ENCODER), ","));
                        List<Artwork> artworkList = tag.getArtworkList();
                        if (CollectionUtils.isNotEmpty(artworkList)) {
                            Artwork artwork = artworkList.get(0);
                            byte[] binaryData = artwork.getBinaryData();
                            music.setAlbumImage(Base64
                                    .getEncoder()
                                    .encodeToString(binaryData));
                        }
                    }

                }

                if (Objects.nonNull(audioHeader)) {
                    music.setTrackLength(Float.parseFloat(audioHeader.getTrackLength() + ""));
                }

                if (StringUtils.isEmpty(music.getLyrics())) {
                    try {
                        String lyc = MusicUtils.getLyc(music.getArtist(), music.getTitle(), music.getAlbum());
                        music.setLyrics(lyc);
                    }
                    catch (Exception e) {
                        log.info(e.getMessage());
                    }
                }
                musicMapper.insert(music);
            }
        }
        catch (Exception e) {
            log.error("解析音乐信息失败！", e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(fileOutputStream);
            if (Objects.nonNull(outFile)) {
                if (outFile.exists()) {
                    outFile.delete();
                }
            }
        }
    }

}
