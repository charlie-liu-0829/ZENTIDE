package com.zentide.component;

import com.zentide.constants.Constants;
import com.zentide.entity.dto.TokenUserInfoDTO;
import com.zentide.redis.RedisUtils;
import com.zentide.utils.StringTools;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RedisComponent {

    @Resource
    private RedisUtils redisUtils;


    public String saveTokenInfo4Admin(String account) {
        String token = UUID.randomUUID().toString();
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_ADMIN + token, account, Constants.REDIS_KEY_EXPIRES_DAY);
        return token;
    }

    public void cleanToken4Admin(String token) {
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_ADMIN + token);
    }

    public String getLoginInfo4Admin(String token) {
        return (String) redisUtils.get(Constants.REDIS_KEY_TOKEN_ADMIN + token);
    }

    public String saveCheckCode(String code) {
        String checkCodeKey = UUID.randomUUID().toString();
        redisUtils.setex(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey, code, 60 * 10);
        return checkCodeKey;
    }

    public String getCheckCode(String checkCodeKey) {
        return (String) redisUtils.get(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
    }

    public void cleanCheckCode(String checkCodeKey) {
        redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
    }

    public void saveTokenInfo(TokenUserInfoDTO tokenUserInfoDto) {
        cleanUserTokenInfo(tokenUserInfoDto.getUserId());
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenUserInfoDto.setToken(token);
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_WEB + token, tokenUserInfoDto, Constants.REDIS_KEY_EXPIRES_DAY * 7);

        redisUtils.setex(Constants.REDIS_KEY_TOKEN_USERID_WEB + tokenUserInfoDto.getUserId(), token, Constants.REDIS_KEY_EXPIRES_DAY * 7);
    }

    /**
     * Refresh an existing web session without rotating its token.
     *
     * Auto-login can be called by several browser loads at the same time. Rotating
     * the token for every one of those requests creates a race where an earlier
     * response returns a token that a later request has already deleted. Keeping
     * the token stable makes refresh idempotent while still extending its TTL.
     */
    public void refreshTokenInfo(TokenUserInfoDTO tokenUserInfoDto) {
        if (tokenUserInfoDto == null || StringTools.isEmpty(tokenUserInfoDto.getToken())
                || StringTools.isEmpty(tokenUserInfoDto.getUserId())) {
            return;
        }
        String token = tokenUserInfoDto.getToken();
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_WEB + token, tokenUserInfoDto, Constants.REDIS_KEY_EXPIRES_DAY * 7);
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_USERID_WEB + tokenUserInfoDto.getUserId(), token, Constants.REDIS_KEY_EXPIRES_DAY * 7);
    }

    public void cleanUserTokenInfo(String userId) {
        String token = (String) redisUtils.get(Constants.REDIS_KEY_TOKEN_USERID_WEB + userId);
        if (StringTools.isEmpty(token)) {
            return;
        }
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_WEB + token);
    }

    public void updateTokenInfo(TokenUserInfoDTO tokenUserInfoDto) {
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_WEB + tokenUserInfoDto.getToken(), tokenUserInfoDto, Constants.REDIS_KEY_EXPIRES_DAY * 7);
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_USERID_WEB + tokenUserInfoDto.getUserId(), tokenUserInfoDto.getToken(), Constants.REDIS_KEY_EXPIRES_DAY * 7);
    }

    public TokenUserInfoDTO getTokenInfo(String token) {
        return (TokenUserInfoDTO) redisUtils.get(Constants.REDIS_KEY_TOKEN_WEB + token);
    }

    public void cleanToken(String token) {
        if (StringTools.isEmpty(token)) {
            return;
        }
        TokenUserInfoDTO tokenUserInfoDTO = getTokenInfo(token);
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_WEB + token);
        if (tokenUserInfoDTO != null) {
            redisUtils.delete(Constants.REDIS_KEY_TOKEN_USERID_WEB + tokenUserInfoDTO.getUserId());
        }
    }

    public void forceLogout(String userId) {
        String token = (String) redisUtils.get(Constants.REDIS_KEY_TOKEN_USERID_WEB + userId);
        if (!StringTools.isEmpty(token)) {
            redisUtils.delete(Constants.REDIS_KEY_TOKEN_WEB + token);
        }
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_USERID_WEB + userId);
    }
}
