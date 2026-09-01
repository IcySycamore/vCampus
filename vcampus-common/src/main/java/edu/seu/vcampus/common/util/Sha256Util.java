package edu.seu.vcampus.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 哈希工具（Java 7 兼容）。
 *
 * <p>
 * 用于密码加盐哈希：{@code sha256(salt + password)}。
 */
public final class Sha256Util {

    /**
     * 私有构造器，禁止实例化工具类。
     */
    private Sha256Util() {
    }

    /**
     * 计算字符串的 SHA-256 十六进制摘要。
     *
     * @param input 输入字符串
     * @return 64 位十六进制哈希
     */
    public static String sha256Hex(String input) {
        return sha256Hex("", input);
    }

    /**
     * 用盐计算 SHA-256 十六进制摘要。
     *
     * @param salt  盐
     * @param input 输入字符串
     * @return 64 位十六进制哈希
     */
    public static String sha256Hex(String salt, String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((salt + input).getBytes(
                    StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String part = Integer.toHexString(b & 0xff);
                if (part.length() < 2) {
                    hex.append('0');
                }
                hex.append(part);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}