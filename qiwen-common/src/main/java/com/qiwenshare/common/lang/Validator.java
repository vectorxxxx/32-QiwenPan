package com.qiwenshare.common.lang;

import cn.hutool.core.exceptions.ValidateException;

import java.util.regex.Pattern;

public class Validator extends cn.hutool.core.lang.Validator
{
    public static Pattern DIR_NAME = Pattern.compile("[^\\\\/:*?\"<>|]+");
    public static Pattern DIR_NAME_RESERVED_WORDS = Pattern.compile("(^(con)$)|^(prn)$|^(aux)$|^(nul)$|(^(com)[1-9]$)|(^(lpt)[1-9]$)");

    public static void checkDirName(String fileName) {
        if (!isMatchRegex(DIR_NAME, fileName)) {
            throw new ValidateException("文件名不能包含下列任何字符：\\/:*?\"<>|");
        }
        if (isMatchRegex(DIR_NAME_RESERVED_WORDS, fileName)) {
            throw new ValidateException("指定的设备名无效！");
        }

    }

    public static void main(String[] args) {
        checkDirName("com1");
    }
}
