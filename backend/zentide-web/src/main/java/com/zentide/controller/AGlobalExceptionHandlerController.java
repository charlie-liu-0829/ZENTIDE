package com.zentide.controller;

import com.zentide.entity.enums.ResponseCodeEnum;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class AGlobalExceptionHandlerController extends ABaseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AGlobalExceptionHandlerController.class);

    @ExceptionHandler(BusinessException.class)
    ResponseVO<Void> handleBusinessException(BusinessException exception) {
        return error(exception.getCode() == null ? ResponseCodeEnum.CODE_600.getCode() : exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(
            {MethodArgumentNotValidException.class, BindException.class,
            MethodArgumentTypeMismatchException.class, ConstraintViolationException.class,
            MissingServletRequestParameterException.class})
    ResponseVO<Void> handleValidationException(Exception exception) {
        String message = ResponseCodeEnum.CODE_600.getMsg();
        if (exception instanceof MethodArgumentNotValidException invalid && invalid.getBindingResult().getFieldError() != null) {
            message = invalid.getBindingResult().getFieldError().getDefaultMessage();
        } else if (exception instanceof BindException bind && bind.getBindingResult().getFieldError() != null) {
            message = bind.getBindingResult().getFieldError().getDefaultMessage();
        }
        return error(ResponseCodeEnum.CODE_600.getCode(), message);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    ResponseVO<Void> handleNotFound(NoHandlerFoundException exception) {
        return error(ResponseCodeEnum.CODE_404.getCode(), ResponseCodeEnum.CODE_404.getMsg());
    }

    @ExceptionHandler({NoResourceFoundException.class, DuplicateKeyException.class})
    ResponseVO<Void> handleKnownInfrastructureException(Exception exception) {
        if (exception instanceof DuplicateKeyException) {
            return error(ResponseCodeEnum.CODE_601.getCode(), ResponseCodeEnum.CODE_601.getMsg());
        }
        return error(ResponseCodeEnum.CODE_404.getCode(), ResponseCodeEnum.CODE_404.getMsg());
    }

    @ExceptionHandler(Exception.class)
    ResponseVO<Void> handleException(Exception exception, HttpServletRequest request) {
        LOGGER.error("ZENTIDE request failed: {} {}", request.getMethod(), request.getRequestURI(), exception);
        return error(ResponseCodeEnum.CODE_500.getCode(), ResponseCodeEnum.CODE_500.getMsg());
    }

    private ResponseVO<Void> error(Integer code, String message) {
        ResponseVO<Void> response = new ResponseVO<>();
        response.setCode(code);
        response.setInfo(message);
        response.setStatus(STATUC_ERROR);
        return response;
    }
}
