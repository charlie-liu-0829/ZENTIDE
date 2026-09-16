package com.zentide.controller;

import com.zentide.annotation.GlobalInterceptor;
import com.zentide.component.RedisComponent;
import com.zentide.constants.Constants;
import com.zentide.entity.dto.TokenUserInfoDTO;
import com.zentide.entity.vo.CheckCodeVO;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.exception.BusinessException;
import com.zentide.service.account.UserAccountService;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController("accountController")
@RequestMapping("/account")
@Validated
public class AccountController extends ABaseController {

    @Resource
    private UserAccountService accountService;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 验证码
     */
    @PostMapping("/checkCode")
    @GlobalInterceptor
    public ResponseVO checkCode() {
        SpecCaptcha captcha = new SpecCaptcha(100, 42, 4);
        captcha.setCharType(Captcha.TYPE_ONLY_NUMBER);
        String code = captcha.text();
        String checkCodeKey = redisComponent.saveCheckCode(code);
        String checkCodeBase64 = captcha.toBase64();
        CheckCodeVO checkCodeVO = new CheckCodeVO(checkCodeBase64, checkCodeKey);
        return getSuccessResponseVO(checkCodeVO);
    }

    @PostMapping("/register")
    @GlobalInterceptor
    public ResponseVO register(@NotEmpty @Email @Size(max = 150) String email,
                               @NotEmpty @Size(max = 20) String nickName,
                               @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD) String registerPassword,
                               @NotEmpty String checkCodeKey, @NotEmpty String checkCode,
                               @Size(max = 200) String securityQuestion, @Size(max = 200) String securityAnswer) {
        try {
            if (!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
                throw new BusinessException("图片验证码不正确");
            }
            accountService.register(email, nickName, registerPassword, securityQuestion, securityAnswer);
            return getSuccessResponseVO(null);
        } finally {
            redisComponent.cleanCheckCode(checkCodeKey);
        }
    }

    @PostMapping("/login")
    @GlobalInterceptor
    public ResponseVO login(@NotEmpty @Email String email, @NotEmpty String password, @NotEmpty String checkCodeKey, @NotEmpty String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
                throw new BusinessException("图片验证码不正确");
            }
            String ip = getIpAddr();
            TokenUserInfoDTO tokenUserInfoDto = accountService.login(email, password, ip);
            return getSuccessResponseVO(tokenUserInfoDto);
        } finally {
            redisComponent.cleanCheckCode(checkCodeKey);
        }
    }

    @PostMapping("/autoLogin")
    @GlobalInterceptor
    public ResponseVO autoLogin() {
        TokenUserInfoDTO tokenUserInfoDto = getTokenUserInfo();
        if (tokenUserInfoDto == null) {
            return getSuccessResponseVO(null);
        }
        redisComponent.refreshTokenInfo(tokenUserInfoDto);
        return getSuccessResponseVO(tokenUserInfoDto);
    }

    @PostMapping("/logout")
    @GlobalInterceptor
    public ResponseVO logout(HttpServletResponse response, @RequestHeader("token") String token) {
        redisComponent.cleanToken(token);
        return getSuccessResponseVO(null);
    }


    @PostMapping("/updatePassword")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO updatePassword(@NotEmpty String oldPassword,
                                     @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD) String password) {
        TokenUserInfoDTO tokenUserInfoDto = getTokenUserInfo();
        accountService.updatePassword(tokenUserInfoDto.getUserId(), oldPassword, password);
        return getSuccessResponseVO(null);
    }

    @PostMapping("/security-question")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO updateSecurityQuestion(@NotEmpty @Size(min = 4, max = 200) String securityQuestion,
                                             @NotEmpty @Size(min = 2, max = 200) String securityAnswer) {
        accountService.updateSecurityQuestion(getTokenUserInfo().getUserId(), securityQuestion, securityAnswer);
        return getSuccessResponseVO(null);
    }

    @PostMapping("/forgot-password/question")
    @GlobalInterceptor
    public ResponseVO securityQuestion(@NotEmpty @Email @Size(max = 150) String email) {
        return getSuccessResponseVO(accountService.securityQuestion(email));
    }

    @PostMapping("/forgot-password/reset")
    @GlobalInterceptor
    public ResponseVO resetPassword(@NotEmpty @Email @Size(max = 150) String email,
                                    @NotEmpty @Size(max = 200) String securityAnswer,
                                    @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD) String newPassword) {
        accountService.resetPassword(email, securityAnswer, newPassword);
        return getSuccessResponseVO(null);
    }

}
