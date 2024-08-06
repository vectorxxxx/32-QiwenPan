/*
 * Copyright (c) 2022. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.qiwenshare.common.util;

import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author MAC
 * @version 1.0
 */
public class HashUtils
{

    public static MessageDigest getDigest(String algorithmName) {
        try {
            return MessageDigest.getInstance(algorithmName);
        }
        catch (NoSuchAlgorithmException var4) {
            String msg = "No native '" + algorithmName + "' MessageDigest instance available on the current JVM.";

        }
        return null;
    }

    public static String hashHex(String algorithmName, String source, String salt, int hashIterations) {
        if (salt == null) {
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
        if (salt != null) {
            digest.reset();
            digest.update(salt);
        }

        byte[] hashed = digest.digest(bytes);
        int iterations = hashIterations - 1;

        for (int i = 0; i < iterations; ++i) {
            digest.reset();
            hashed = digest.digest(hashed);
        }

        return hashed;
    }

}
