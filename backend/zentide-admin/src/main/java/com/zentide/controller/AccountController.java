package com.zentide.controller;

import com.zentide.component.RedisComponent;
import com.zentide.constants.Constants;
import com.zentide.entity.config.AppConfig;
import com.zentide.entity.vo.CheckCodeVO;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.exception.BusinessException;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController("accountController")
@RequestMapping("/account")
@Validated
public class AccountController extends ABaseController {

    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 验证码
     */
    @PostMapping("/checkCode")
    public ResponseVO checkCode() {
        SpecCaptcha captcha = new SpecCaptcha(100, 42, 4);
        captcha.setCharType(Captcha.TYPE_ONLY_NUMBER);
        String code = captcha.text();
        String checkCodeKey = redisComponent.saveCheckCode(code);
        String checkCodeBase64 = captcha.toBase64();
        CheckCodeVO checkCodeVO = new CheckCodeVO(checkCodeBase64, checkCodeKey);
        return getSuccessResponseVO(checkCodeVO);
    }

    @PostMapping("/login")
    public ResponseVO login(
            @NotEmpty String account,
            @NotEmpty String password,
            @NotEmpty String checkCode,
            @NotEmpty String checkCodeKey) {
        try {
            if (!checkCode.equalsIgnoreCase((String) redisComponent.getCheckCode(checkCodeKey))) {
                throw new BusinessException("图片验证码不正确");
            }
            boolean passwordMatches = MessageDigest.isEqual(
                    password.getBytes(StandardCharsets.UTF_8),
                    appConfig.getAdminPassword().getBytes(StandardCharsets.UTF_8));
            if (!account.equals(appConfig.getAdminAccount()) || !passwordMatches) {
                throw new BusinessException("账号或者密码错误");
            }
            String token = redisComponent.saveTokenInfo4Admin(account);
            return getSuccessResponseVO(token);
        } finally {
            redisComponent.cleanCheckCode(checkCodeKey);
        }
    }

    @PostMapping("/logout")
    public ResponseVO logout(@RequestHeader(Constants.TOKEN_ADMIN) String token) {
        redisComponent.cleanToken4Admin(token);
        return getSuccessResponseVO(null);
    }
}
