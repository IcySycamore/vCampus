package edu.seu.vcampus.server.auth;

import edu.seu.vcampus.common.random.RandomGen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一次性 nonce 存储（挑战-应答登录用）。
 *
 * <p>
 * nonce 由服务器生成、绑定用户名，一次性使用，
 * 5 分钟过期惰性删除。线程安全（ConcurrentHashMap）。
 */
public class NonceStore {

    /** 过期时长（毫秒）：5 分钟。 */
    private static final long EXPIRY_MILLIS = 5 * 60 * 1000L;

    /** nonce → 记录。 */
    private final Map<String, NonceEntry> nonces = new ConcurrentHashMap<String, NonceEntry>();

    /** 随机源。 */
    private final RandomGen random = new RandomGen();

    /**
     * 为指定用户生成一个一次性 nonce。
     *
     * @param username 用户名
     * @return 新 nonce
     */
    public String issue(String username) {
        String nonce = random.randomHex(16);
        nonces.put(nonce, new NonceEntry(username,
                System.currentTimeMillis() + EXPIRY_MILLIS));
        return nonce;
    }

    /**
     * 校验并消费 nonce：存在、未过期、属于该用户则删除并返回 true。
     *
     * @param nonce    nonce
     * @param username 用户名
     * @return 是否有效
     */
    public boolean verifyAndConsume(String nonce, String username) {
        NonceEntry entry = nonces.remove(nonce);// 预删除
        if (entry == null) {// 不存在nonce
            return false;
        }
        if (System.currentTimeMillis() > entry.expiry) {
            return false;
        }
        return entry.username.equals(username);
    }

    /**
     * nonce 记录。
     */
    private static final class NonceEntry {
        final String username;
        final long expiry;

        NonceEntry(String username, long expiry) {
            this.username = username;
            this.expiry = expiry;
        }
    }
}