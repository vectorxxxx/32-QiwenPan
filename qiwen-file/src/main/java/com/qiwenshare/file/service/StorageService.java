package com.qiwenshare.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiwenshare.file.api.IStorageService;
import com.qiwenshare.file.domain.StorageBean;
import com.qiwenshare.file.domain.SysParam;
import com.qiwenshare.file.mapper.StorageMapper;
import com.qiwenshare.file.mapper.SysParamMapper;
import com.qiwenshare.file.mapper.UserFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StorageService extends ServiceImpl<StorageMapper, StorageBean> implements IStorageService
{
    @Resource
    private StorageMapper storageMapper;
    @Resource
    private SysParamMapper sysParamMapper;
    @Resource
    private UserFileMapper userFileMapper;

    @Override
    public Long getTotalStorageSize(String userId) {
        // 获取用户总存储空间
        StorageBean storageBean = storageMapper.selectOne(new LambdaQueryWrapper<StorageBean>().eq(StorageBean::getUserId, userId));
        Long totalStorageSize = Objects
                .requireNonNull(storageBean)
                .getTotalStorageSize();
        if (Objects.isNull(totalStorageSize)) {
            SysParam sysParam = sysParamMapper.selectOne(new LambdaQueryWrapper<SysParam>().eq(SysParam::getSysParamKey, "totalStorageSize"));
            totalStorageSize = Long.parseLong(sysParam.getSysParamValue());
            storageBean = new StorageBean();
            storageBean.setUserId(userId);
            storageBean.setTotalStorageSize(totalStorageSize);
            storageMapper.insert(storageBean);
        }
        totalStorageSize *= 1024 * 1024;
        return totalStorageSize;
    }

    @Override
    public boolean checkStorage(String userId, Long fileSize) {
        // 获取用户总存储空间
        Long totalStorageSize = getTotalStorageSize(userId);

        // 获取用户已使用存储空间
        Long storageSize = userFileMapper.selectStorageSizeByUserId(userId);
        if (Objects.isNull(storageSize)) {
            storageSize = 0L;
        }

        // 判断用户存储空间是否足够
        return storageSize + fileSize <= totalStorageSize;
    }
}
