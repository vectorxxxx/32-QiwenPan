package com.qiwenshare.ufop.factory;

import com.qiwenshare.ufop.autoconfiguration.UFOPProperties;
import com.qiwenshare.ufop.config.AliyunConfig;
import com.qiwenshare.ufop.config.MinioConfig;
import com.qiwenshare.ufop.config.QiniuyunConfig;
import com.qiwenshare.ufop.constant.StorageTypeEnum;
import com.qiwenshare.ufop.domain.ThumbImage;
import com.qiwenshare.ufop.operation.copy.Copier;
import com.qiwenshare.ufop.operation.copy.product.AliyunOSSCopier;
import com.qiwenshare.ufop.operation.copy.product.FastDFSCopier;
import com.qiwenshare.ufop.operation.copy.product.LocalStorageCopier;
import com.qiwenshare.ufop.operation.copy.product.MinioCopier;
import com.qiwenshare.ufop.operation.copy.product.QiniuyunKodoCopier;
import com.qiwenshare.ufop.operation.delete.Deleter;
import com.qiwenshare.ufop.operation.delete.product.AliyunOSSDeleter;
import com.qiwenshare.ufop.operation.delete.product.FastDFSDeleter;
import com.qiwenshare.ufop.operation.delete.product.LocalStorageDeleter;
import com.qiwenshare.ufop.operation.delete.product.MinioDeleter;
import com.qiwenshare.ufop.operation.delete.product.QiniuyunKodoDeleter;
import com.qiwenshare.ufop.operation.download.Downloader;
import com.qiwenshare.ufop.operation.download.product.AliyunOSSDownloader;
import com.qiwenshare.ufop.operation.download.product.FastDFSDownloader;
import com.qiwenshare.ufop.operation.download.product.LocalStorageDownloader;
import com.qiwenshare.ufop.operation.download.product.MinioDownloader;
import com.qiwenshare.ufop.operation.download.product.QiniuyunKodoDownloader;
import com.qiwenshare.ufop.operation.preview.Previewer;
import com.qiwenshare.ufop.operation.preview.product.AliyunOSSPreviewer;
import com.qiwenshare.ufop.operation.preview.product.FastDFSPreviewer;
import com.qiwenshare.ufop.operation.preview.product.LocalStoragePreviewer;
import com.qiwenshare.ufop.operation.preview.product.MinioPreviewer;
import com.qiwenshare.ufop.operation.preview.product.QiniuyunKodoPreviewer;
import com.qiwenshare.ufop.operation.read.Reader;
import com.qiwenshare.ufop.operation.read.product.AliyunOSSReader;
import com.qiwenshare.ufop.operation.read.product.FastDFSReader;
import com.qiwenshare.ufop.operation.read.product.LocalStorageReader;
import com.qiwenshare.ufop.operation.read.product.MinioReader;
import com.qiwenshare.ufop.operation.read.product.QiniuyunKodoReader;
import com.qiwenshare.ufop.operation.upload.Uploader;
import com.qiwenshare.ufop.operation.upload.product.AliyunOSSUploader;
import com.qiwenshare.ufop.operation.upload.product.FastDFSUploader;
import com.qiwenshare.ufop.operation.upload.product.LocalStorageUploader;
import com.qiwenshare.ufop.operation.upload.product.MinioUploader;
import com.qiwenshare.ufop.operation.upload.product.QiniuyunKodoUploader;
import com.qiwenshare.ufop.operation.write.Writer;
import com.qiwenshare.ufop.operation.write.product.AliyunOSSWriter;
import com.qiwenshare.ufop.operation.write.product.FastDFSWriter;
import com.qiwenshare.ufop.operation.write.product.LocalStorageWriter;
import com.qiwenshare.ufop.operation.write.product.MinioWriter;
import com.qiwenshare.ufop.operation.write.product.QiniuyunKodoWriter;

import javax.annotation.Resource;

public class UFOPFactory
{
    private String storageType;
    private AliyunConfig aliyunConfig;
    private ThumbImage thumbImage;
    private MinioConfig minioConfig;
    private QiniuyunConfig qiniuyunConfig;
    @Resource
    private FastDFSCopier fastDFSCopier;
    @Resource
    private FastDFSUploader fastDFSUploader;
    @Resource
    private FastDFSDownloader fastDFSDownloader;
    @Resource
    private FastDFSDeleter fastDFSDeleter;
    @Resource
    private FastDFSReader fastDFSReader;
    @Resource
    private FastDFSPreviewer fastDFSPreviewer;
    @Resource
    private FastDFSWriter fastDFSWriter;
    @Resource
    private AliyunOSSUploader aliyunOSSUploader;
    @Resource
    private MinioUploader minioUploader;
    @Resource
    private QiniuyunKodoUploader qiniuyunKodoUploader;

    public UFOPFactory() {
    }

    public UFOPFactory(UFOPProperties ufopProperties) {
        this.storageType = ufopProperties.getStorageType();
        this.aliyunConfig = ufopProperties.getAliyun();
        this.thumbImage = ufopProperties.getThumbImage();
        this.minioConfig = ufopProperties.getMinio();
        this.qiniuyunConfig = ufopProperties.getQiniuyun();
    }

    public Uploader getUploader() {
        // 简单工厂模式
        int type = Integer.parseInt(storageType);
        Uploader uploader = null;
        if (StorageTypeEnum.LOCAL.getCode() == type) {
            uploader = new LocalStorageUploader();
        }
        else if (StorageTypeEnum.ALIYUN_OSS.getCode() == type) {
            uploader = aliyunOSSUploader;
        }
        else if (StorageTypeEnum.FAST_DFS.getCode() == type) {
            uploader = fastDFSUploader;
        }
        else if (StorageTypeEnum.MINIO.getCode() == type) {
            uploader = minioUploader;
        }
        else if (StorageTypeEnum.QINIUYUN_KODO.getCode() == type) {
            uploader = qiniuyunKodoUploader;
        }
        return uploader;
    }

    public Downloader getDownloader(int storageType) {
        // 简单工厂模式
        Downloader downloader = null;
        if (StorageTypeEnum.LOCAL.getCode() == storageType) {
            downloader = new LocalStorageDownloader();
        }
        else if (StorageTypeEnum.ALIYUN_OSS.getCode() == storageType) {
            downloader = new AliyunOSSDownloader(aliyunConfig);
        }
        else if (StorageTypeEnum.FAST_DFS.getCode() == storageType) {
            downloader = fastDFSDownloader;
        }
        else if (StorageTypeEnum.MINIO.getCode() == storageType) {
            downloader = new MinioDownloader(minioConfig);
        }
        else if (StorageTypeEnum.QINIUYUN_KODO.getCode() == storageType) {
            downloader = new QiniuyunKodoDownloader(qiniuyunConfig);
        }
        return downloader;
    }

    public Deleter getDeleter(int storageType) {
        // 简单工厂模式
        Deleter deleter = null;
        if (StorageTypeEnum.LOCAL.getCode() == storageType) {
            deleter = new LocalStorageDeleter();
        }
        else if (StorageTypeEnum.ALIYUN_OSS.getCode() == storageType) {
            deleter = new AliyunOSSDeleter(aliyunConfig);
        }
        else if (StorageTypeEnum.FAST_DFS.getCode() == storageType) {
            deleter = fastDFSDeleter;
        }
        else if (StorageTypeEnum.MINIO.getCode() == storageType) {
            deleter = new MinioDeleter(minioConfig);
        }
        else if (StorageTypeEnum.QINIUYUN_KODO.getCode() == storageType) {
            deleter = new QiniuyunKodoDeleter(qiniuyunConfig);
        }
        return deleter;
    }

    public Reader getReader(int storageType) {
        // 简单工厂模式
        Reader reader = null;
        if (StorageTypeEnum.LOCAL.getCode() == storageType) {
            reader = new LocalStorageReader();
        }
        else if (StorageTypeEnum.ALIYUN_OSS.getCode() == storageType) {
            reader = new AliyunOSSReader(aliyunConfig);
        }
        else if (StorageTypeEnum.FAST_DFS.getCode() == storageType) {
            reader = fastDFSReader;
        }
        else if (StorageTypeEnum.MINIO.getCode() == storageType) {
            reader = new MinioReader(minioConfig);
        }
        else if (StorageTypeEnum.QINIUYUN_KODO.getCode() == storageType) {
            reader = new QiniuyunKodoReader(qiniuyunConfig);
        }
        return reader;
    }

    public Writer getWriter(int storageType) {
        // 简单工厂模式
        Writer writer = null;
        if (StorageTypeEnum.LOCAL.getCode() == storageType) {
            writer = new LocalStorageWriter();
        }
        else if (StorageTypeEnum.ALIYUN_OSS.getCode() == storageType) {
            writer = new AliyunOSSWriter(aliyunConfig);
        }
        else if (StorageTypeEnum.FAST_DFS.getCode() == storageType) {
            writer = fastDFSWriter;
        }
        else if (StorageTypeEnum.MINIO.getCode() == storageType) {
            writer = new MinioWriter(minioConfig);
        }
        else if (StorageTypeEnum.QINIUYUN_KODO.getCode() == storageType) {
            writer = new QiniuyunKodoWriter(qiniuyunConfig);
        }
        return writer;
    }

    public Previewer getPreviewer(int storageType) {
        // 简单工厂模式
        Previewer previewer = null;
        if (StorageTypeEnum.LOCAL.getCode() == storageType) {
            previewer = new LocalStoragePreviewer(thumbImage);
        }
        else if (StorageTypeEnum.ALIYUN_OSS.getCode() == storageType) {
            previewer = new AliyunOSSPreviewer(aliyunConfig, thumbImage);
        }
        else if (StorageTypeEnum.FAST_DFS.getCode() == storageType) {
            previewer = fastDFSPreviewer;
        }
        else if (StorageTypeEnum.MINIO.getCode() == storageType) {
            previewer = new MinioPreviewer(minioConfig, thumbImage);
        }
        else if (StorageTypeEnum.QINIUYUN_KODO.getCode() == storageType) {
            previewer = new QiniuyunKodoPreviewer(qiniuyunConfig, thumbImage);
        }
        return previewer;
    }

    public Copier getCopier() {
        // 简单工厂模式
        int type = Integer.parseInt(storageType);
        Copier copier = null;
        if (StorageTypeEnum.LOCAL.getCode() == type) {
            copier = new LocalStorageCopier();
        }
        else if (StorageTypeEnum.ALIYUN_OSS.getCode() == type) {
            copier = new AliyunOSSCopier(aliyunConfig);
        }
        else if (StorageTypeEnum.FAST_DFS.getCode() == type) {
            copier = fastDFSCopier;
        }
        else if (StorageTypeEnum.MINIO.getCode() == type) {
            copier = new MinioCopier(minioConfig);
        }
        else if (StorageTypeEnum.QINIUYUN_KODO.getCode() == type) {
            copier = new QiniuyunKodoCopier(qiniuyunConfig);
        }
        return copier;
    }
}
