package com.qiwenshare.common.util;

/**
 * @author VectorX
 * @version 1.0
 */
public class IdUtils
{

    public static String GenerateRevisionId(String expectedKey) {
        if (expectedKey.length() > 20) {
            expectedKey = Integer.toString(expectedKey.hashCode());
        }

        String key = expectedKey.replace("[^0-9-.a-zA-Z_=]", "_");

        return key.substring(0, Math.min(key.length(), 20));
    }
}
