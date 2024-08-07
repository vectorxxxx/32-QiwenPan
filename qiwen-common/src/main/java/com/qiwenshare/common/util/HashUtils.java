/*
 * Copyright (c) 2022. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.qiwenshare.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Objects;

/**
 * @author VectorX
 * @version 1.0
 */
@Slf4j
public class HashUtils
{

    public static MessageDigest getDigest(String algorithmName) {
        try {
            return MessageDigest.getInstance(algorithmName);
        }
        catch (NoSuchAlgorithmException var4) {
            String msg = MessageFormat.format("No native {0} MessageDigest instance available on the current JVM.", algorithmName);
            log.error(msg, var4);
        }
        return null;
    }

    public static String hashHex(String algorithmName, String source, String salt, int hashIterations) {
        if (StringUtils.isBlank(salt)) {
            return hashHex(algorithmName, source.getBytes(StandardCharsets.UTF_8), null, hashIterations);
        }
        else {
            return hashHex(algorithmName, source.getBytes(StandardCharsets.UTF_8), salt.getBytes(StandardCharsets.UTF_8), hashIterations);
        }

    }

    public static String hashHex(String algorithmName, byte[] bytes, byte[] salt, int hashIterations) {
        byte[] hash = hash(bytes, algorithmName, salt, hashIterations);
        return Hex.encodeHexString(hash);
    }

    public static byte[] hash(byte[] bytes, String algorithmName, byte[] salt, int hashIterations) {
        MessageDigest digest = getDigest(algorithmName);

        // 若有盐值
        if (Objects.nonNull(salt)) {
            digest.reset();
            digest.update(salt);
        }

        // 初次哈希
        byte[] hashed = digest.digest(bytes);

        // 迭代哈希（增强哈希的复杂性和安全性）
        for (int i = 1; i < hashIterations; i++) {
            digest.reset();
            hashed = digest.digest(hashed);
        }

        return hashed;
    }

}
