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
     * @param userName 用户名
     * @return {@link Boolean }
     */
    public Boolean isUserNameExist(String userName) {
        final Long count = userMapper.selectCount(new LambdaQueryWrapper<UserBean>().eq(UserBean::getUsername, userName));
        return count > 0;
    }

    /**
     * 检测手机号是否存在
     *
     * @param telephone 电话
     * @return
     */
    public Boolean isPhoneExist(String telephone) {
        final Long count = userMapper.selectCount(new LambdaQueryWrapper<UserBean>().eq(UserBean::getTelephone, telephone));
        return count > 0;
    }

    public Boolean isPhoneFormatRight(String phone) {
        return Pattern.matches(RegexConstant.PASSWORD_REGEX, phone);
    }
}
