package edu.seu.vcampus.common.random;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * 随机数生成工具：唯一标识 {@link UUID} 与安全随机十六进制串。
 *
 * <p>
 * randomHex 用于生成 salt / nonce / token（不可预测）。
 */
public class RandomGen {

    /** 安全随机源。 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成 UUID 唯一标识。
     *
     * @return UUID
     */
    public UUID getUuid() {
        return UUID.randomUUID();
    }

    /**
     * 生成 n 字节安全随机数的十六进制串（长度 2*n）。
     *
     * @param bytes 字节数
     * @return 十六进制串
     */
    public String randomHex(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        StringBuilder hex = new StringBuilder();
        for (byte b : buf) {
            String part = Integer.toHexString(b & 0xff);
            if (part.length() < 2) {
                hex.append('0');
            }
            hex.append(part);
        }
        return hex.toString();
    }
}
