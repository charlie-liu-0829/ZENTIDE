package com.zentide.utils;


import com.zentide.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DateUtil {
    private static final Logger log = LoggerFactory.getLogger(DateUtil.class);

    /**
     *
     * @param dateString 日期时间字符串
     * @param format     格式字符串（如"yyyy-MM-dd HH:mm:ss"）
     * @return 转换后的Date对象，失败返回null
     */
    public static Date parse(String dateString, String format) {
        if (dateString == null || dateString.trim().isEmpty() || format == null || format.trim().isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            LocalDateTime localDateTime;
            // 根据格式判断是日期、时间还是日期时间
            Boolean hasDate = format.contains("y") || format.contains("M") || format.contains("d");
            Boolean hasTime = format.contains("H") || format.contains("h") || format.contains("m") || format.contains("s");
            if (hasDate && hasTime) {
                localDateTime = LocalDateTime.parse(dateString, formatter);
            } else if (hasDate) {
                LocalDate localDate = LocalDate.parse(dateString, formatter);
                localDateTime = localDate.atStartOfDay(); // 补充时间为00:00:00
            } else {
                LocalTime localTime = LocalTime.parse(dateString, formatter);
                localDateTime = LocalDate.now().atTime(localTime); // 补充当前日期
            }
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException e) {
            log.error("转换日期异常", e);
            return null;
        }
    }

    /**
     * 将Date对象按指定格式转换为字符串
     * 内部先转换为LocalDateTime，再进行格式化
     *
     * @param date   要转换的Date对象
     * @param format 格式字符串（如"yyyy-MM-dd"）
     * @return 格式化后的日期字符串，参数无效返回空字符串
     */
    public static String format(Date date, String format) {
        if (date == null || format == null || format.trim().isEmpty()) {
            return null;
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            return localDateTime.atZone(ZoneId.systemDefault()).format(formatter);
        } catch (IllegalArgumentException e) {
            log.error("日期格式化异常", e);
            return null;
        }
    }


    public static String getMinAfter(int min, String pattern) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, min);
        return format(c.getTime(), pattern);
    }

    public static String getBeforeDay(int day, String pattern) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -day);
        return format(c.getTime(), pattern);
    }

    public static List<String> getDateRange(String startDateStr, String endDateStr, String formatter) {
        List<String> dates = new ArrayList<>();
        LocalDate startDate = LocalDate.parse(startDateStr, DateTimeFormatter.ofPattern(formatter));
        LocalDate endDate = LocalDate.parse(endDateStr, DateTimeFormatter.ofPattern(formatter));
        // 确保开始日期不晚于结束日期
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate.format(DateTimeFormatter.ofPattern(formatter)));
            currentDate = currentDate.plusDays(1);
        }
        return dates;
    }
}
