package com.qiwenshare.file.service;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiwenshare.common.result.RestResult;
import com.qiwenshare.common.util.DateUtil;
import com.qiwenshare.common.util.HashUtils;
import com.qiwenshare.common.util.PasswordUtil;
import com.qiwenshare.common.util.security.JwtUser;
import com.qiwenshare.file.api.IUserService;
import com.qiwenshare.file.component.JwtComp;
import com.qiwenshare.file.component.UserDealComp;
import com.qiwenshare.file.constant.RoleType;
import com.qiwenshare.file.controller.UserController;
import com.qiwenshare.file.domain.user.Role;
import com.qiwenshare.file.domain.user.UserBean;
import com.qiwenshare.file.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class UserService extends ServiceImpl<UserMapper, UserBean> implements IUserService, UserDetailsService
{

    @Resource
    UserMapper userMapper;
    @Resource
    UserDealComp userDealComp;
    @Resource
    JwtComp jwtComp;

    @Override
    public String getUserIdByToken(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }

        // 去掉 Bearer
        token = token
                .replace("Bearer ", "")
                .replace("Bearer%20", "");

        // 解码
        Claims claims;
        try {
            claims = jwtComp.parseJWT(token);
        }
        catch (Exception e) {
            log.error("解码异常：{}", e.getMessage(), e);
            return null;
        }
        if (Objects.isNull(claims)) {
            log.info("解码为空");
            return null;
        }
        String subject = claims.getSubject();
        log.debug("解析结果：{}", subject);

        // 获取用户信息
        UserBean tokenUserBean = JSON.parseObject(subject, UserBean.class);
        UserBean user = userMapper.selectById(tokenUserBean.getUserId());
        if (Objects.nonNull(user)) {
            return user.getUserId();
        }

        return null;
    }

    @Override
    public RestResult<String> registerUser(UserBean userBean) {
        // 判断验证码
        String telephone = userBean.getTelephone();

        UserController.verificationCodeMap.remove(telephone);

        // 注册校验
        if (userDealComp.isUserNameExist(userBean.getUsername())) {
            return RestResult
                    .<String>fail()
                    .message("用户名已存在！");
        }
        if (!userDealComp.isPhoneFormatRight(telephone)) {
            return RestResult
                    .<String>fail()
                    .message("手机号格式不正确！");
        }
        if (userDealComp.isPhoneExist(telephone)) {
            return RestResult
                    .<String>fail()
                    .message("手机号已存在！");
        }

        // 密码加盐
        String salt = PasswordUtil.getSaltValue();
        String newPassword = HashUtils.hashHex("MD5", userBean.getPassword(), salt, 1024);

        // 用户入库
        userBean.setSalt(salt);
        userBean.setPassword(newPassword);
        userBean.setRegisterTime(DateUtil.getCurrentTime());
        userBean.setUserId(IdUtil.getSnowflakeNextIdStr());
        int result = userMapper.insertUser(userBean);

        // 角色入库
        /**
         * 角色 ID
         *
         * 1: 超级管理员
         * 2: 普通用户
         */
        userMapper.insertUserRole(userBean.getUserId(), RoleType.USER.getRoleId());

        if (result == 1) {
            return RestResult.success();
        }
        else {
            return RestResult
                    .<String>fail()
                    .message("注册用户失败，请检查输入信息！");
        }
    }

    @Override
    public UserBean findUserInfoByTelephone(String telephone) {
        return userMapper.selectOne(new LambdaQueryWrapper<UserBean>().eq(UserBean::getTelephone, telephone));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 根据用户名查询用户信息
        UserBean user = userMapper.selectById(Long.valueOf(username));
        if (Objects.isNull(user)) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 根据用户 ID 查询角色信息
        final List<Role> roleList = selectRoleListByUserId(user.getUserId());

        // 根据角色信息查询权限信息
        final List<SimpleGrantedAuthority> authorities = roleList
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_".concat(role.getRoleName())))
                .collect(Collectors.toList());

        // 封装成 UserDetails
        return new JwtUser(user.getUserId(), user.getUsername(), user.getPassword(), user.getAvailable(), authorities);
    }

    @Override
    public List<Role> selectRoleListByUserId(String userId) {
        return userMapper.selectRoleListByUserId(userId);
    }

    @Override
    public String getSaltByTelephone(String telephone) {

        return userMapper.selectSaltByTelephone(telephone);
    }

    @Override
    public UserBean selectUserByTelephoneAndPassword(String username, String password) {
        return userMapper.selectUserByTelephoneAndPassword(username, password);
    }

}
