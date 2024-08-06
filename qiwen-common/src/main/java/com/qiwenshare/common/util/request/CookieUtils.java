package com.qiwenshare.common.util.request;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * @author MAC
 * @version 1.0
 * <p>
 * cookie工具类
 */
public class CookieUtils
{

    /**
     * 从请求头里面获取token
     *
     * @param httpServletRequest 请求
     * @return 返回token字符串
     */
    public static String getTokenByRequest(HttpServletRequest httpServletRequest) {
        Cookie[] cookieArr = httpServletRequest.getCookies();
        String token = "";
        for (Cookie cookie : cookieArr) {
            if ("token".equals(cookie.getName())) {
                token = cookie.getValue();
            }
        }
        return token;
    }
}
