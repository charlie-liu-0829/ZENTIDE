package com.zentide.controller;
import com.zentide.component.RedisComponent;
import com.zentide.constants.Constants;
import com.zentide.entity.enums.ResponseCodeEnum;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.exception.BusinessException;
import com.zentide.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


public class ABaseController {

    @Resource
    private RedisComponent redisComponent;

    protected static final String STATUC_SUCCESS = "success";

    protected static final String STATUC_ERROR = "error";

    protected <T> ResponseVO<T> getSuccessResponseVO(T t) {
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUC_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }

    protected <T> ResponseVO<T> getBusinessErrorResponseVO(BusinessException e, T t) {
        ResponseVO<T> vo = new ResponseVO<>();
        vo.setStatus(STATUC_ERROR);
        if (e.getCode() == null) {
            vo.setCode(ResponseCodeEnum.CODE_600.getCode());
        } else {
            vo.setCode(e.getCode());
        }
        vo.setInfo(e.getMessage());
        vo.setData(t);
        return vo;
    }

    protected <T> ResponseVO<T> getServerErrorResponseVO(T t) {
        ResponseVO<T> vo = new ResponseVO<>();
        vo.setStatus(STATUC_ERROR);
        vo.setCode(ResponseCodeEnum.CODE_500.getCode());
        vo.setInfo(ResponseCodeEnum.CODE_500.getMsg());
        vo.setData(t);
        return vo;
    }

    protected String getAdminAccount() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String token = request.getHeader(Constants.TOKEN_ADMIN);
        if (StringTools.isEmpty(token)) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        String account = redisComponent.getLoginInfo4Admin(token);
        if (StringTools.isEmpty(account)) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        return account;
    }
}
