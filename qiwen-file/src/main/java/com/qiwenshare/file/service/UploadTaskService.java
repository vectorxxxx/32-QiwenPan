package com.qiwenshare.file.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiwenshare.file.api.IUploadTaskService;
import com.qiwenshare.file.domain.UploadTask;
import com.qiwenshare.file.mapper.UploadTaskMapper;
import org.springframework.stereotype.Service;

@Service
public class UploadTaskService extends ServiceImpl<UploadTaskMapper, UploadTask> implements IUploadTaskService
{

}
