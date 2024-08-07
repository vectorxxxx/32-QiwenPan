package com.qiwenshare.file.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiwenshare.common.result.RestResult;
import com.qiwenshare.file.api.INoticeService;
import com.qiwenshare.file.domain.Notice;
import com.qiwenshare.file.dto.notice.NoticeListDTO;
import com.qiwenshare.file.vo.notice.NoticeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Tag(name = "公告管理")
@RestController
@RequestMapping("/notice")
public class NoticeController
{
    public static final String CURRENT_MODULE = "公告管理";
    @Resource
    private INoticeService noticeService;

    /**
     * 得到所有的公告
     *
     * @return
     */
    @Operation(summary = "得到所有的公告列表",
               tags = {CURRENT_MODULE})
    @RequestMapping(value = "/list",
                    method = RequestMethod.GET)
    @ResponseBody
    public RestResult<NoticeVO> selectUserList(
            @Parameter(description = "当前页，从1开始")
            @RequestParam(defaultValue = "1")
                    int page,
            @Parameter(description = "页大小")
            @RequestParam(defaultValue = "10")
                    int pageSize,
            @Parameter(description = "标题")
            @RequestParam(required = false)
                    String title,
            @Parameter(description = "发布者")
            @RequestParam(required = false)
                    Long publisher,
            @Parameter(description = "开始发布时间")
            @RequestParam(required = false)
                    String beginTime,
            @Parameter(description = "开始发布时间")
            @RequestParam(required = false)
                    String endTime) {
        NoticeListDTO noticeListDTO = new NoticeListDTO()
                .setPage(page)
                .setPageSize(pageSize)
                .setTitle(title)
                .setPlatform(3)
                .setPublisher(publisher)
                .setBeginTime(beginTime)
                .setEndTime(endTime);
        IPage<NoticeVO> noticeIPage = noticeService.selectUserPage(noticeListDTO);
        return RestResult
                .<NoticeVO>success()
                .dataList(noticeIPage.getRecords(), noticeIPage.getTotal());
    }

    @Operation(summary = "查询公告详情",
               tags = {CURRENT_MODULE})
    @RequestMapping(value = "/detail",
                    method = RequestMethod.GET)
    @ResponseBody
    public RestResult<Notice> getNoticeDetail(
            @Parameter(description = "公告id",
                       required = true)
                    long noticeId) {
        Notice notice = noticeService.getById(noticeId);
        return RestResult
                .<Notice>success()
                .data(notice);
    }

}
