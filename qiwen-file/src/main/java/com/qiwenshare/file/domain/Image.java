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
 * @date 2021/12/7 22:05
 */
@Data
@Table(name = "image")
@Entity
@TableName("image")
@Accessors(chain = true)
public class Image
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    @Column(columnDefinition = "bigint(20)")
    private Long imageId;

    @Column(columnDefinition = "varchar(20) comment '文件id'")
    private String fileId;

    @Column(columnDefinition = "int(5) comment '图像的宽'")
    private Integer imageWidth;

    @Column(columnDefinition = "int(5) comment '图像的高'")
    private Integer imageHeight;
}
