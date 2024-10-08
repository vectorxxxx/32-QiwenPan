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

/**
 * @author VectorX
 * @version 1.0
 * @description: TODO
 * @date 2022/1/12 14:41
 */
@Data
@Table(name = "commonfile")
@Entity
@TableName("commonfile")
@Accessors(chain = true)
public class CommonFile
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @TableId(type = IdType.AUTO)
    @Column(columnDefinition = "varchar(20)")
    public String commonFileId;
    @Column(columnDefinition = "varchar(20) comment '用户文件id'")
    public String userFileId;
    //    @Column(columnDefinition="int(2) comment '文件权限'")
    //    public Integer filePermission;
}
