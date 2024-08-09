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
 * @date 2022/4/27 23:44
 */
@Data
@Table(name = "music")
@Entity
@TableName("music")
@Accessors(chain = true)
public class Music
{
    // 数据库主键，使用自增策略
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    @Column(columnDefinition = "bigint(20)")
    private String musicId;

    // 文件ID，用于唯一标识音乐文件
    @Column(columnDefinition = "bigint(20) comment '文件id'")
    private String fileId;

    // 音轨编号，用于标识音乐在专辑或播放列表中的顺序
    private String track;

    // 艺术家名称，表示音乐的演唱者或演奏者
    @Column
    private String artist;

    // 音乐标题，直接反映歌曲的名称
    @Column
    private String title;

    // 专辑名称，标识音乐所属的专辑
    @Column
    private String album;

    // 发行年份，表示音乐的出版或发布年份
    @Column
    private String year;

    // 音乐流派，如摇滚、流行等
    @Column
    private String genre;

    // 歌曲评论，用于存储关于音乐的评论信息
    @Column
    private String comment;

    // 歌词内容，详细歌词文本
    @Column(columnDefinition = "varchar(10000) comment '歌词'")
    private String lyrics;

    // 作曲家，表示音乐的创作人
    @Column
    private String composer;

    // 发行者，表示音乐的出版或发行公司
    @Column
    private String publisher;

    // 原唱艺术家，对于翻唱歌曲，标识原来的演唱者
    @Column
    private String originalArtist;

    // 专辑艺术家，标识专辑的主艺术家或乐队
    @Column
    private String albumArtist;

    // 版权信息，表示音乐的版权归属
    @Column
    private String copyright;

    // 音乐的URL，可用于在线播放或下载
    @Column
    private String url;

    // 编码器信息，表示用于编码音乐文件的软件或工具
    @Column
    private String encoder;

    // 专辑封面图片，以二进制大对象形式存储
    @Column(columnDefinition = "mediumblob")
    private String albumImage;

    // 音轨长度，以浮点数表示，单位通常为秒
    @Column
    private Float trackLength;

}
