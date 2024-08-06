package com.qiwenshare.common.util.security;

import com.qiwenshare.common.exception.QiwenException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;

public class SessionUtil
{

    public static JwtUser getSession() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            String userName = (String) principal;
            if ("anonymousUser".equals(userName)) {
                JwtUser userBean = new JwtUser();
                userBean.setUsername(userName);
                userBean.setUserId("");
                return userBean;
            }
        }
        JwtUser userBean = (JwtUser) authentication.getPrincipal();
        return userBean;
    }

    public static JwtUser getSession(Principal principal) {
        if (principal == null) {
            return null;
        }
        JwtUser userBean = (JwtUser) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
        return userBean;
    }

    public static String getUserId() {
        JwtUser session = getSession();
        if (session == null) {
            throw new QiwenException("用户未登录");
        }
        return session.getUserId();
    }

    public static String getUserId(String teamId) {
        if (StringUtils.isNotEmpty(teamId) && !"my_space".equals(teamId)) {
            return teamId;
        }
        else {
            return getUserId();
        }
    }
}
