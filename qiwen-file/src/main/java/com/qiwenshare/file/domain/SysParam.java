package com.qiwenshare.file.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Table(name = "sysparam")
@Entity
@TableName("sysparam")
public class SysParam
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "bigint(20)")
    @TableId(type = IdType.AUTO)
    private Long sysParamId;
    @Column(columnDefinition = "varchar(50)")
    private String groupName;
    @Column(columnDefinition = "varchar(50)")
    private String sysParamKey;
    @Column(columnDefinition = "varchar(50)")
    private String sysParamValue;
    @Column(columnDefinition = "varchar(50)")
    private String sysParamDesc;
}
