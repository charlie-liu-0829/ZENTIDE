package com.zentide.service.account;

import com.zentide.component.RedisComponent;
import com.zentide.constants.Constants;
import com.zentide.entity.dto.TokenUserInfoDTO;
import com.zentide.entity.enums.PageSize;
import com.zentide.entity.enums.ResponseCodeEnum;
import com.zentide.entity.enums.UserSexEnum;
import com.zentide.entity.enums.UserStatusEnum;
import com.zentide.entity.po.UserInfo;
import com.zentide.entity.query.SimplePage;
import com.zentide.entity.query.UserInfoQuery;
import com.zentide.entity.vo.PaginationResultVO;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.UserAccountMapper;
import com.zentide.utils.CopyTools;
import com.zentide.utils.PasswordTools;
import com.zentide.utils.StringTools;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Account application service shared by the user and administration APIs.
 */
@Service
public class UserAccountService {

    private final UserAccountMapper userAccountMapper;
    private final RedisComponent redisComponent;

    public UserAccountService(UserAccountMapper userAccountMapper,
                              RedisComponent redisComponent) {
        this.userAccountMapper = userAccountMapper;
        this.redisComponent = redisComponent;
    }

    public PaginationResultVO<UserInfo> findUsers(UserInfoQuery query) {
        int count = userAccountMapper.count(query);
        int pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<UserInfo> users = userAccountMapper.list(query);
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), users);
    }

    public UserInfo findById(String userId) {
        return userAccountMapper.findPublicById(userId);
    }

    public int updateUser(String userId, UserInfo changes) {
        return userAccountMapper.updateById(changes, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(String email, String nickName, String password) {
        register(email, nickName, password, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(String email, String nickName, String password, String securityQuestion, String securityAnswer) {
        if (userAccountMapper.countByEmail(email) > 0) {
            throw new BusinessException("邮箱账号已经存在");
        }
        if (userAccountMapper.countByNickname(nickName) > 0) {
            throw new BusinessException("昵称已经存在");
        }

        UserInfo user = new UserInfo();
        user.setUserId(StringTools.getRandomNumber(Constants.LENGTH_10));
        user.setNickName(nickName);
        user.setEmail(email);
        user.setPassword(PasswordTools.encode(password));
        if (securityQuestion != null && !securityQuestion.isBlank() && securityAnswer != null && !securityAnswer.isBlank()) {
            user.setSecurityQuestion(securityQuestion.trim());
            user.setSecurityAnswerHash(PasswordTools.encode(securityAnswer.trim()));
        }
        user.setJoinTime(new Date());
        user.setStatus(UserStatusEnum.ENABLE.getStatus());
        user.setSex(UserSexEnum.SECRECY.getType());
        userAccountMapper.insert(user);
    }

    public String securityQuestion(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (normalized.isBlank()) throw new BusinessException("请输入邮箱");
        String question = userAccountMapper.findSecurityQuestionByEmail(normalized);
        if (question == null || question.isBlank()) throw new BusinessException("该账号尚未设置密保问题，请联系管理员");
        return question;
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String email, String answer, String newPassword) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        UserInfo user = userAccountMapper.findCredentialsByEmail(normalized);
        if (user == null || user.getSecurityAnswerHash() == null || !PasswordTools.matches(answer == null ? "" : answer.trim(), user.getSecurityAnswerHash())) {
            throw new BusinessException("邮箱或密保答案不正确");
        }
        if (userAccountMapper.resetPasswordByEmail(normalized, PasswordTools.encode(newPassword)) != 1) {
            throw new BusinessException("密码重置失败，请稍后再试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateSecurityQuestion(String userId, String question, String answer) {
        String cleanQuestion = question == null ? "" : question.trim();
        String cleanAnswer = answer == null ? "" : answer.trim();
        if (cleanQuestion.length() < 4 || cleanQuestion.length() > 200) throw new BusinessException("密保问题需要是 4 到 200 个字符");
        if (cleanAnswer.length() < 2 || cleanAnswer.length() > 200) throw new BusinessException("密保答案需要是 2 到 200 个字符");
        if (userAccountMapper.updateSecurityQuestion(userId, cleanQuestion, PasswordTools.encode(cleanAnswer)) != 1) throw new BusinessException("密保设置失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public TokenUserInfoDTO login(String email, String password, String ip) {
        UserInfo user = userAccountMapper.findCredentialsByEmail(email);
        if (user == null || !PasswordTools.matches(password, user.getPassword())) {
            throw new BusinessException("账号或者密码错误");
        }
        if (UserStatusEnum.DISABLE.getStatus().equals(user.getStatus())) {
            throw new BusinessException("账号已禁用");
        }

        UserInfo loginChanges = new UserInfo();
        if (PasswordTools.needsUpgrade(user.getPassword())) {
            loginChanges.setPassword(PasswordTools.encode(password));
        }
        loginChanges.setLastLoginTime(new Date());
        loginChanges.setLastLoginIp(ip);
        userAccountMapper.updateById(loginChanges, user.getUserId());

        TokenUserInfoDTO tokenUser = CopyTools.copy(user, TokenUserInfoDTO.class);
        redisComponent.saveTokenInfo(tokenUser);
        return tokenUser;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(String userId, String oldPassword, String newPassword) {
        UserInfo user = userAccountMapper.findCredentialsById(userId);
        if (user == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (!PasswordTools.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原始密码错误");
        }

        UserInfo changes = new UserInfo();
        changes.setPassword(PasswordTools.encode(newPassword));
        userAccountMapper.updateById(changes, userId);
    }
}
