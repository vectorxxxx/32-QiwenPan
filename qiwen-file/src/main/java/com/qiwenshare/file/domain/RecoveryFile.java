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
import javax.persistence.UniqueConstraint;

@Data
@Table(name = "recoveryfile",
       uniqueConstraints = {
               @UniqueConstraint(name = "user_file_id_index3",
                                 columnNames = {"userFileId"})
       })
@Entity
@TableName("recoveryfile")
@Accessors(chain = true)
public class RecoveryFile
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    @Column(columnDefinition = "bigint(20)")
    private Long recoveryFileId;
    @Column(columnDefinition = "varchar(20) comment '用户文件id'")
    private String userFileId;
    @Column(columnDefinition = "varchar(25) comment '删除时间'")
    private String deleteTime;
    @Column(columnDefinition = "varchar(50) comment '删除批次号'")
    private String deleteBatchNum;
}
