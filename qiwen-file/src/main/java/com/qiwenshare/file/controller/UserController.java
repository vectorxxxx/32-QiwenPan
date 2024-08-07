package com.qiwenshare.file.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiwenshare.common.anno.MyLog;
import com.qiwenshare.common.result.RestResult;
import com.qiwenshare.common.result.ResultCodeEnum;
import com.qiwenshare.common.util.DateUtil;
import com.qiwenshare.common.util.HashUtils;
import com.qiwenshare.common.util.security.JwtUser;
import com.qiwenshare.common.util.security.SessionUtil;
import com.qiwenshare.file.api.IUserLoginInfoService;
import com.qiwenshare.file.api.IUserService;
import com.qiwenshare.file.component.JwtComp;
import com.qiwenshare.file.constant.HashAlgorithmType;
import com.qiwenshare.file.domain.UserLoginInfo;
import com.qiwenshare.file.domain.user.UserBean;
import com.qiwenshare.file.dto.user.RegisterDTO;
import com.qiwenshare.file.util.BeanCopyUtils;
import com.qiwenshare.file.vo.user.UserLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Tag(name = "user",
     description = "该接口为用户接口，主要做用户登录，注册和校验token")
@RestController
@Slf4j
@RequestMapping("/user")
public class UserController
{
    @Resource
    private IUserService userService;
    @Resource
    private IUserLoginInfoService userLoginInfoService;
    @Resource
    private JwtComp jwtComp;

    public static Map<String, String> verificationCodeMap = new HashMap<>();

    public static final String CURRENT_MODULE = "用户管理";

    @Operation(summary = "用户注册",
               description = "注册账号",
               tags = {"user"})
    @PostMapping(value = "/register")
    @MyLog(operation = "用户注册",
           module = CURRENT_MODULE)
    public RestResult<String> addUser(@Valid
                                      @RequestBody
                                              RegisterDTO registerDTO) {
        UserBean userBean = BeanCopyUtils.copy(registerDTO, UserBean.class);
        return userService.registerUser(userBean);
    }

    @Operation(summary = "用户登录",
               description = "用户登录认证后才能进入系统",
               tags = {"user"})
    @GetMapping("/login")
    @MyLog(operation = "用户登录",
           module = CURRENT_MODULE)
    public RestResult<UserLoginVo> userLogin(
            @Parameter(description = "登录手机号")
                    String telephone,
            @Parameter(description = "登录密码")
                    String password) {
        // 验证手机号和密码
        String salt = userService.getSaltByTelephone(telephone);
        String hashPassword = HashUtils.hashHex(HashAlgorithmType.MD5.name(), password, salt, 1024);
        UserBean result = userService.selectUserByTelephoneAndPassword(telephone, hashPassword);
        if (Objects.isNull(result)) {
            return RestResult
                    .<UserLoginVo>fail()
                    .message("手机号或密码错误！");
        }

        // 生成 jwt
        Map<String, Object> param = new HashMap<>();
        param.put("userId", result.getUserId());
        String token = "";
        try {
            token = jwtComp.createJWT(param);
        }
        catch (Exception e) {
            log.info("登录失败：{}", e.getMessage(), e);
            return RestResult
                    .<UserLoginVo>fail()
                    .message("创建token失败！");
        }

        // 根据手机号获取用户信息
        UserBean sessionUserBean = userService.findUserInfoByTelephone(telephone);
        final Integer available = sessionUserBean.getAvailable();
        if (Objects.nonNull(available) && available.compareTo(0) == 0) {
            return RestResult
                    .<UserLoginVo>fail()
                    .message("用户已被禁用");
        }

        // 封装返回值
        UserLoginVo userLoginVo = BeanCopyUtils.copy(sessionUserBean, UserLoginVo.class);
        userLoginVo.setToken("Bearer ".concat(token));
        RestResult<UserLoginVo> restResult = new RestResult<>();
        restResult.setData(userLoginVo);
        restResult.setSuccess(true);
        restResult.setCode(ResultCodeEnum.USER_FORBIDDEN.getCode());
        return restResult;

    }

    @Operation(summary = "检查用户登录信息",
               description = "验证token的有效性",
               tags = {"user"})
    @GetMapping("/checkuserlogininfo")
    public RestResult<UserLoginVo> checkUserLoginInfo(
            @RequestHeader("token")
                    String token) {
        // 通过token获取用户id
        String userId = userService.getUserIdByToken(token);
        if (StringUtils.isEmpty(userId)) {
            return RestResult
                    .<UserLoginVo>fail()
                    .message("用户暂未登录");
        }

        // 更新用户登录信息
        userLoginInfoService.remove(new LambdaQueryWrapper<UserLoginInfo>()
                .eq(UserLoginInfo::getUserId, userId)
                .likeRight(UserLoginInfo::getUserloginDate, DateUtil
                        .getCurrentTime()
                        .substring(0, 10)));
        userLoginInfoService.save(new UserLoginInfo()
                .setUserId(userId)
                .setUserloginDate(DateUtil.getCurrentTime()));

        // 获取用户信息
        UserBean user = userService.getById(userId);
        UserLoginVo userLoginVo = Objects
                .requireNonNull(BeanCopyUtils.copy(user, UserLoginVo.class))
                .setHasWxAuth(StringUtils.isNotEmpty(user.getWxOpenId()));

        // 封装返回值
        return RestResult
                .<UserLoginVo>success()
                .data(userLoginVo);
    }

    @Operation(summary = "检查微信认证",
               description = "检查微信认证",
               tags = {"user"})
    @GetMapping("/checkWxAuth")
    public RestResult<Boolean> checkWxAuth() {
        // 获取用户 Session 信息
        JwtUser sessionUserBean = SessionUtil.getSession();
        if (sessionUserBean == null || "anonymousUser".equals(sessionUserBean.getUsername())) {
            return RestResult
                    .<Boolean>fail()
                    .message("用户暂未登录");
        }

        // 获取用户信息
        UserBean user = userService.getById(sessionUserBean.getUserId());

        // 封装返回值
        return RestResult
                .<Boolean>success()
                .data(StringUtils.isNotEmpty(user.getWxOpenId()));
    }

}
