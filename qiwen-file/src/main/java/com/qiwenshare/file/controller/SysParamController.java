package com.qiwenshare.file.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiwenshare.common.result.RestResult;
import com.qiwenshare.file.api.ISysParamService;
import com.qiwenshare.file.domain.SysParam;
import com.qiwenshare.file.dto.param.QueryGroupParamDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "系统参数管理")
@RestController
@RequestMapping("/param")
public class SysParamController
{
    @Resource
    private ISysParamService sysParamService;

    @Operation(summary = "查询系统参数组",
               tags = {"系统参数管理"})
    @RequestMapping(value = "/grouplist",
                    method = RequestMethod.GET)
    @ResponseBody
    public RestResult<Map<String, Object>> groupList(
            @Parameter(description = "查询参数dto",
                       required = false)
                    QueryGroupParamDTO queryGroupParamDTO) {
        // 查询系统参数，封装成键值对形式
        Map<String, Object> result = sysParamService
                .list(new LambdaQueryWrapper<SysParam>().eq(SysParam::getGroupName, queryGroupParamDTO.getGroupName()))
                .stream()
                .collect(Collectors.toMap(SysParam::getSysParamKey, SysParam::getSysParamValue));

        // 返回结果
        return RestResult
                .<Map<String, Object>>success()
                .data(result);
    }

}
