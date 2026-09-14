package com.zentide.interceptor;

import com.zentide.component.RedisComponent;
import com.zentide.constants.Constants;
import com.zentide.entity.enums.ResponseCodeEnum;
import com.zentide.exception.BusinessException;
import com.zentide.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class AppInterceptor implements HandlerInterceptor {

    private final static String URL_ACCOUNT = "/account";
    private final static String URL_FILE = "/file";
    /** Agent 控制面使用独立的内部令牌，不依赖管理员浏览器会话。 */
    private final static String URL_GOVERNANCE_INTERNAL = "/zentide/v1/admin/governance/internal";

    @Resource
    private RedisComponent redisComponent;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (null == handler) {
            return false;
        }
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        if (request.getRequestURI().contains(URL_ACCOUNT) || request.getRequestURI().contains(URL_FILE)
                || request.getRequestURI().contains(URL_GOVERNANCE_INTERNAL)) {
            return true;
        }
        String token = request.getHeader(Constants.TOKEN_ADMIN);
        if (StringTools.isEmpty(token)) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        Object sessionObj = redisComponent.getLoginInfo4Admin(token);
        if (sessionObj == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
