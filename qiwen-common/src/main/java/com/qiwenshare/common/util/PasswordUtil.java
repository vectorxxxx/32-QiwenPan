package com.qiwenshare.common.util;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author VectorX
 * @version 1.0.0
 * @description 密码实用程序
 * @date 2024/08/06
 */
public class PasswordUtil
{
    @Deprecated
    public static String getSaltValueOld() {
        Random r = new Random();

        StringBuilder sb = new StringBuilder(16);
        sb
                .append(r.nextInt(99999999))
                .append(r.nextInt(99999999));
        int len = sb.length();
        if (len < 16) {
            for (int i = 0; i < 16 - len; i++) {
                sb.append("0");
            }
        }
        return sb.toString();
    }

    /**
     * 获取盐值
     *
     * @return {@link String }
     */
    public static String getSaltValue() {
        final Random r = new Random();
        return IntStream
                // 循环 16 次
                .range(0, 16)
                // 每一次循环生成一位 0~9 的随机数（能弥补最终位数不足 16 位的情况）
                .map(i -> r.nextInt(10))
                // 数字转字符串
                .mapToObj(String::valueOf)
                // 拼接字符串
                .collect(Collectors.joining());
    }
}
