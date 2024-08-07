package com.qiwenshare.common.util;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author VectorX
 * @version 1.0.0
 * @description 日期实用程序
 * @date 2024/08/06
 */
public class DateUtil
{

    /**
     * 获取系统当前时间
     *
     * @return 系统当前时间
     */
    public static String getCurrentTime() {
        Date date = new Date();
        // %tF：表示日期的格式，等同于 yyyy-MM-dd。它会输出当前日期的年、月、日部分。
        // %<tT：表示时间的格式，等同于 HH:mm:ss。这里的 < 符号表示使用前面格式化的对象（在这个例子中是 date），而 tT 则表示时间的格式。
        // %tF %<tT：表示输出当前的日期和时间，格式为 yyyy-MM-dd HH:mm:ss。
        return String.format("%tF %<tT", date);
    }

    /**
     * @param stringDate   日期字符串，如"2000-03-19"
     * @param formatString 格式，如"yyyy-MM-dd"
     * @return 日期
     * @throws ParseException 解析异常
     */
    public static Date getDateByFormatString(String stringDate, String formatString) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(formatString);
        return dateFormat.parse(stringDate);
    }

    /**
     * 两个日期相差天数
     *
     * @param preDate   第一个时间日期
     * @param afterDate 第二个时间十七
     * @return 相差的天数
     */
    public static int getDifferentDays(Date preDate, Date afterDate) {

        int preYear = getYear(preDate);
        int afterYear = getYear(afterDate);
        int preDayOfYear = getDayOfYear(preDate);
        int afterDayOfYear = getDayOfYear(afterDate);

        if (afterYear - preYear == 0) {
            return afterDayOfYear - preDayOfYear;
        }
        else {
            int diffDay = 0;
            while (preYear < afterYear) {
                if (diffDay == 0 && isLeapYear(preYear)) {
                    diffDay = 366 - preDayOfYear;
                }
                else if (diffDay == 0 && !isLeapYear(preYear)) {
                    diffDay = 365 - preDayOfYear;
                }
                else if (isLeapYear(preYear)) {
                    diffDay += 366;
                }
                else {
                    diffDay += 365;
                }
                preYear++;
            }

            diffDay += afterDayOfYear;
            return diffDay;

        }

    }

    /**
     * 一年中的第几天
     *
     * @param date 日期
     * @return 第几天
     */
    public static int getDayOfYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * 获取年份 jdk推荐写法，date.getYear()已被废弃
     *
     * @param date 日期
     * @return 年份
     */
    public static int getYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    /**
     * 判断是否是闰年
     *
     * @param year 年，如2010
     * @return 是否闰年
     */
    public static boolean isLeapYear(int year) {
        if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) {
            return true;
        }
        return false;
    }

    /**
     * 获取时间戳
     *
     * @return long
     */
    public static long getTime() {
        return new Date().getTime();
    }

    public static List<String> getRecent30DateList() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        Date today = new Date();
        String date = fmt.format(today);
        String maxDateStr = date;
        String minDateStr = "";
        Calendar calc = Calendar.getInstance();
        List<String> datefor30List = new ArrayList<>();
        try {
            for (int i = 0; i < 30; i++) {
                calc.setTime(fmt.parse(maxDateStr));
                calc.add(calc.DATE, -i);
                Date minDate = calc.getTime();
                minDateStr = fmt.format(minDate);
                datefor30List.add(minDateStr);

            }
        }
        catch (ParseException e1) {
            e1.printStackTrace();
        }
        Collections.reverse(datefor30List);
        return datefor30List;

    }

}
