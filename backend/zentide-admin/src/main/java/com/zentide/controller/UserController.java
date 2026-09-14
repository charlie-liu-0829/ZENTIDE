package com.zentide.controller;

import com.zentide.component.RedisComponent;
import com.zentide.entity.enums.ResponseCodeEnum;
import com.zentide.entity.enums.UserStatusEnum;
import com.zentide.entity.po.UserInfo;
import com.zentide.entity.query.UserInfoQuery;
import com.zentide.entity.vo.PaginationResultVO;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.exception.BusinessException;
import com.zentide.service.account.UserAccountService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Validated
public class UserController extends ABaseController {

    @Resource
    private UserAccountService accountService;

    @Resource
    private RedisComponent redisComponent;

    @RequestMapping("/loadUser")
    public ResponseVO loadUser(UserInfoQuery userInfoQuery) {
        userInfoQuery.setOrderBy("u.join_time desc");
        PaginationResultVO resultVO = accountService.findUsers(userInfoQuery);
        return getSuccessResponseVO(resultVO);
    }


    @RequestMapping("/changeStatus")
    public ResponseVO changeStatus(@NotEmpty String userId, @NotNull Integer status) {
        UserStatusEnum userStatusEnum = UserStatusEnum.getByStatus(status);
        if (null == userStatusEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setStatus(status);
        accountService.updateUser(userId, userInfo);
        if (UserStatusEnum.DISABLE == userStatusEnum) {
            //强制退出
            redisComponent.forceLogout(userId);
        }
        return getSuccessResponseVO(null);
    }
}
