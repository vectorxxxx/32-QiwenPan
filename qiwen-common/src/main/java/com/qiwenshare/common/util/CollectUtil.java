package com.qiwenshare.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * 通信工具类
 *
 * @author VectorX
 */
@Slf4j
public class CollectUtil
{

    /**
     * java 后台获取访问客户端ip地址
     *
     * @param request HttpServletRequest请求
     * @return IP地址
     */
    public String getClientIpAddress(HttpServletRequest request) {
        String clientIp = request.getHeader("x-forwarded-for");
        if (StringUtils.isEmpty(clientIp) || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isEmpty(clientIp) || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isEmpty(clientIp) || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp;
    }

    /**
     * 获取本地IP
     *
     * @return IP地址
     */
    public String getLocalIp() {
        InetAddress addr = null;
        String ip = "";
        try {
            addr = InetAddress.getLocalHost();
        }
        catch (UnknownHostException e) {
            log.error("获取本地IP失败");
        }
        if (Objects.nonNull(addr)) {
            ip = addr
                    .getHostAddress()
                    .toString();
        }
        return ip;
    }

}
