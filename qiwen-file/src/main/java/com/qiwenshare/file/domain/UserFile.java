package com.qiwenshare.file.domain;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiwenshare.common.util.DateUtil;
import com.qiwenshare.file.constant.FileDeleteFlagEnum;
import com.qiwenshare.file.io.QiwenFile;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Data
@Table(name = "userfile",
       uniqueConstraints = {
               @UniqueConstraint(name = "fileindex",
                                 columnNames = {"userId", "filePath", "fileName", "extendName", "deleteFlag", "isDir"})
       })
@Entity
@TableName("userfile")
@NoArgsConstructor
@Accessors(chain = true)
public class UserFile
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @TableId(type = IdType.AUTO)
    @Column(nullable = false,
            columnDefinition = "varchar(20)")
    private String userFileId;

    @Column(columnDefinition = "bigint(20) comment '用户id'")
    private String userId;

    @Column(columnDefinition = "varchar(20) comment '文件id'")
    private String fileId;

    @Column(columnDefinition = "varchar(100) comment '文件名'")
    private String fileName;

    @Column(columnDefinition = "varchar(500) comment '文件路径'")
    private String filePath;

    @Column(columnDefinition = "varchar(100) NULL DEFAULT '' comment '扩展名'")
    private String extendName;

    @Column(columnDefinition = "int(1) comment '是否是目录(0-否,1-是)'")
    private Integer isDir;

    @Column(columnDefinition = "varchar(25) comment '修改时间'")
    private String uploadTime;

    @Column(columnDefinition = "int(11) comment '删除标识(0-未删除，1-已删除)'")
    private Integer deleteFlag;

    @Column(columnDefinition = "varchar(25) comment '删除时间'")
    private String deleteTime;

    @Column(columnDefinition = "varchar(50) comment '删除批次号'")
    private String deleteBatchNum;
    @Column(columnDefinition = "varchar(30) comment '创建时间'")
    private String createTime;
    @Column(columnDefinition = "varchar(20) comment '创建用户id'")
    private String createUserId;
    @Column(columnDefinition = "varchar(30) comment '修改时间'")
    private String modifyTime;
    @Column(columnDefinition = "varchar(20) comment '修改用户id'")
    private String modifyUserId;

    public UserFile(QiwenFile qiwenFile, String userId, String fileId) {
        this.userFileId = IdUtil.getSnowflakeNextIdStr();
        this.userId = userId;
        this.fileId = fileId;
        this.filePath = qiwenFile.getParent();
        this.fileName = qiwenFile.getNameNotExtend();
        this.extendName = qiwenFile.getExtendName();
        this.isDir = qiwenFile.isDirectory() ?
                     1 :
                     0;
        String currentTime = DateUtil.getCurrentTime();
        this.setUploadTime(currentTime);
        this.setCreateUserId(userId);
        this.setCreateTime(currentTime);
        this.deleteFlag = FileDeleteFlagEnum.NOT_DELETED.getDeleteFlag();
    }

    public boolean isDirectory() {
        return this.isDir == 1;
    }

    public boolean isFile() {
        return this.isDir == 0;
    }

}
