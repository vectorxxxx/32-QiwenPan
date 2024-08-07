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
    StorageMapper storageMapper;
    @Resource
    SysParamMapper sysParamMapper;
    @Resource
    UserFileMapper userFileMapper;

    @Override
    public Long getTotalStorageSize(String userId) {
        LambdaQueryWrapper<StorageBean> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(StorageBean::getUserId, userId);

        StorageBean storageBean = storageMapper.selectOne(lambdaQueryWrapper);
        Long totalStorageSize = null;
        if (Objects.isNull(storageBean) || Objects.isNull(storageBean.getTotalStorageSize())) {
            LambdaQueryWrapper<SysParam> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
            lambdaQueryWrapper1.eq(SysParam::getSysParamKey, "totalStorageSize");
            SysParam sysParam = sysParamMapper.selectOne(lambdaQueryWrapper1);
            totalStorageSize = Long.parseLong(sysParam.getSysParamValue());
            storageBean = new StorageBean();
            storageBean.setUserId(userId);
            storageBean.setTotalStorageSize(totalStorageSize);
            storageMapper.insert(storageBean);
        }
        else {
            totalStorageSize = storageBean.getTotalStorageSize();
        }

        if (Objects.nonNull(totalStorageSize)) {
            totalStorageSize = totalStorageSize * 1024 * 1024;
        }
        return totalStorageSize;
    }

    @Override
    public boolean checkStorage(String userId, Long fileSize) {
        LambdaQueryWrapper<StorageBean> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(StorageBean::getUserId, userId);

        StorageBean storageBean = storageMapper.selectOne(lambdaQueryWrapper);
        Long totalStorageSize = null;
        if (Objects.isNull(storageBean) || Objects.isNull(storageBean.getTotalStorageSize())) {
            LambdaQueryWrapper<SysParam> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
            lambdaQueryWrapper1.eq(SysParam::getSysParamKey, "totalStorageSize");
            SysParam sysParam = sysParamMapper.selectOne(lambdaQueryWrapper1);
            totalStorageSize = Long.parseLong(sysParam.getSysParamValue());
            storageBean = new StorageBean();
            storageBean.setUserId(userId);
            storageBean.setTotalStorageSize(totalStorageSize);
            storageMapper.insert(storageBean);
        }
        else {
            totalStorageSize = storageBean.getTotalStorageSize();
        }

        if (Objects.nonNull(totalStorageSize)) {
            totalStorageSize = totalStorageSize * 1024 * 1024;
        }

        Long storageSize = userFileMapper.selectStorageSizeByUserId(userId);
        if (Objects.isNull(storageSize)) {
            storageSize = 0L;
        }
        if (storageSize + fileSize > totalStorageSize) {
            return false;
        }
        return true;

    }
}
