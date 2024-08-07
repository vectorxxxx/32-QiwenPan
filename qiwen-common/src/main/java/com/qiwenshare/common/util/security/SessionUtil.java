package com.qiwenshare.common.util.security;

import com.qiwenshare.common.exception.QiwenException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.Objects;

public class SessionUtil
{

    /**
     * 获取会话
     *
     * @return {@link JwtUser }
     */
    public static JwtUser getSession() {
        Object principal = SecurityContextHolder
                // 获取上下文
                .getContext()
                // 获取认证信息
                .getAuthentication()
                // 获取用户凭证
                .getPrincipal();
        if (principal instanceof String) {
            String userName = (String) principal;
            if ("anonymousUser".equals(userName)) {
                return new JwtUser()
                        .setUsername(userName)
                        .setUserId("");
            }
        }
        return (JwtUser) principal;
    }

    /**
     * 获取会话
     *
     * @param principal 凭证
     * @return {@link JwtUser }
     */
    public static JwtUser getSession(Principal principal) {
        if (Objects.isNull(principal)) {
            return null;
        }
        final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = (UsernamePasswordAuthenticationToken) principal;
        return (JwtUser) usernamePasswordAuthenticationToken.getPrincipal();
    }

    /**
     * 获取用户 ID
     *
     * @return {@link String }
     */
    public static String getUserId() {
        JwtUser session = getSession();
        if (Objects.isNull(session)) {
            throw new QiwenException("用户未登录");
        }
        return session.getUserId();
    }

    /**
     * 获取用户 ID
     *
     * @param teamId 团队 ID
     * @return {@link String }
     */
    public static String getUserId(String teamId) {
        if (StringUtils.isNotEmpty(teamId) && !"my_space".equals(teamId)) {
            return teamId;
        }
        return getUserId();
    }
}
