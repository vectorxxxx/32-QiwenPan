package com.qiwenshare.file.vo.notice;

import lombok.Data;

/**
 * @author VectorX
 * @version 1.0
 * @description: 公告
 * @date 2021/11/22 22:16
 */
@Data
public class NoticeVO
{
    private Long noticeId;

    private String title;
    private Integer platform;

    private String markdownContent;
    private String content;
    private String validDateTime;
    private int isLongValidData;

    private String createTime;
    private Long createUserId;
    private String modifyTime;
    private Long modifyUserId;
}
