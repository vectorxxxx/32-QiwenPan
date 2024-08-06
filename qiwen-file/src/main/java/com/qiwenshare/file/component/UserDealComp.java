package com.qiwenshare.file.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiwenshare.common.constant.RegexConstant;
import com.qiwenshare.file.domain.user.UserBean;
import com.qiwenshare.file.mapper.UserMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.regex.Pattern;

@Component
public class UserDealComp
{
    @Resource
    private UserMapper userMapper;

    /**
     * 检测用户名是否存在
     *
     * @param userBean
     */
    public Boolean isUserNameExit(UserBean userBean) {
        final Long count = userMapper.selectCount(new LambdaQueryWrapper<UserBean>().eq(UserBean::getUsername, userBean.getUsername()));
        return count > 0;
    }

    /**
     * 检测手机号是否存在
     *
     * @param userBean
     * @return
     */
    public Boolean isPhoneExit(UserBean userBean) {
        final Long count = userMapper.selectCount(new LambdaQueryWrapper<UserBean>().eq(UserBean::getTelephone, userBean.getTelephone()));
        return count > 0;
    }

    public Boolean isPhoneFormatRight(String phone) {
        return Pattern.matches(RegexConstant.PASSWORD_REGEX, phone);
    }
}
