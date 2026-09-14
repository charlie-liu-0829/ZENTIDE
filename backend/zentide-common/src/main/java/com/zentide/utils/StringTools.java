package com.zentide.utils;

import com.zentide.constants.Constants;
import com.zentide.entity.enums.DateTimePatternEnum;
import com.zentide.exception.BusinessException;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Random;


public class StringTools {
    private static final Logger log = LoggerFactory.getLogger(StringTools.class);

    private static final String DECIMAL_FORMAT = "0.00";


    public static void checkParam(Object param) {
        try {
            Field[] fields = param.getClass().getDeclaredFields();
            boolean notEmpty = false;
            for (Field field : fields) {
                String methodName = "get" + StringTools.upperCaseFirstLetter(field.getName());
                Method method = param.getClass().getMethod(methodName);
                Object object = method.invoke(param);
                if (object != null && object instanceof String && !StringTools.isEmpty(object.toString())
                        || object != null && !(object instanceof String)) {
                    notEmpty = true;
                    break;
                }
            }
            if (!notEmpty) {
                throw new BusinessException("多参数更新，删除，必须有非空条件");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("校验参数是否为空失败", e);
            throw new BusinessException("校验参数是否为空失败");
        }
    }

    public static String upperCaseFirstLetter(String field) {
        if (isEmpty(field)) {
            return field;
        }
        //如果第二个字母是大写，第一个字母不大写
        if (field.length() > 1 && Character.isUpperCase(field.charAt(1))) {
            return field;
        }
        return field.substring(0, 1).toUpperCase() + field.substring(1);
    }

    public static boolean isEmpty(String str) {
        if (null == str || "".equals(str) || "null".equals(str) || "\u0000".equals(str)) {
            return true;
        } else if ("".equals(str.trim())) {
            return true;
        }
        return false;
    }

    public static final String getRandomString(Integer count) {
        return RandomStringUtils.random(count, true, true);
    }

    public static final String getRandomNumber(Integer count) {
        return RandomStringUtils.random(count, false, true);
    }

    public static String getFileSuffix(String fileName) {
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        return suffix;
    }

    public static boolean pathIsOk(String path) {
        if (StringTools.isEmpty(path)) {
            return true;
        }
        if (path.contains("../") || path.contains("..\\")) {
            return false;
        }
        return true;
    }

    public static BigDecimal convertYuan2fen4BigDecimal(BigDecimal amount) {
        BigDecimal fen = amount.multiply(new BigDecimal(100)).setScale(0, BigDecimal.ROUND_DOWN);
        return fen;
    }

    public static final String createProductOrderId() {
        return DateUtil.format(new Date(), DateTimePatternEnum.YYYYMMDDHHMMSS.getPattern()) + getRandomString(16).toUpperCase();
    }

    public static final String createPayOrderId() {
        return StringTools.getRandomNumber(Constants.LENGTH_30);
    }

    public static Integer getRandomNumberRange(Integer min, Integer max) {
        return new Random().nextInt(max - min + 1) + min;
    }
}
