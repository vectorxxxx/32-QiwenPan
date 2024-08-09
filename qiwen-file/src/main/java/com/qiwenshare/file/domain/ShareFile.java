package com.qiwenshare.file.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Table(name = "sharefile")
@Entity
@TableName("sharefile")
@Accessors(chain = true)
public class ShareFile
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @TableId(type = IdType.AUTO)
    private String shareFileId;

    @Column(columnDefinition = "varchar(50) comment '分享批次号'")
    private String shareBatchNum;

    @Column(columnDefinition = "varchar(20) comment '用户文件id'")
    private String userFileId;

    @Column(columnDefinition = "varchar(100) comment '分享文件路径'")
    private String shareFilePath;

}
