package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.random.RandomGen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一次性 nonce 管理（挑战-应答登录用）。
 *
 * <p>
 * nonce 由服务器生成并绑定用户名（单槽），一次性使用， 5 分钟过期惰性删除。线程安全（ConcurrentHashMap）。
 */
public class NonceManager {
    /**
     * nonce 记录。
     */
    private static final class NonceEntry {
        final String m_nonce;
        final long m_expiry;

        NonceEntry(String username, long expiry) {
            this.m_nonce = username;
            this.m_expiry = expiry;
        }
    }

    /** 过期时长 5 * 60 * 1000 毫秒 = 5 分钟。 */
    private static final long EXPIRY_MILLIS = 5 * 60 * 1000L;

    /** map(username,(nonce,expiry))。 */
    private final Map<String, NonceEntry> m_nonces_map;

    /** 随机源。 */
    private final RandomGen random = new RandomGen();

    /** 构造 nonce 管理器。 */
    public NonceManager() {
        m_nonces_map = new ConcurrentHashMap<String, NonceEntry>();
    }

    /** 单例实例。 */
    private static NonceManager instance;

    /**
     * 获取全局 nonce 管理器单例。
     *
     * @return nonce 管理器
     */
    public static synchronized NonceManager getInstance() {
        if (instance == null) {
            instance = new NonceManager();
        }
        return instance;
    }

    /**
     * 查询
     * 
     * @details 根据用户名查询nonce，若没有为其分配一个
     * @param username 用户名
     * @return nonce
     */
    public String issue(String username) {
        NonceEntry result = m_nonces_map.get(username);

        if (!(result == null)) {// 存在
            return result.m_nonce;
        }
        // 不存在分配一个
        String new_nonce = random.randomHex(16);
        m_nonces_map.put(username,
                new NonceEntry(new_nonce, System.currentTimeMillis() + EXPIRY_MILLIS));
        return new_nonce;
    }

    /**
     * 校验并消费 nonce
     * 
     * @details 存在未过期的对应nonce则删除并返回。无效返回null
     *
     * @param username 用户名
     * @return 有效nonce。无效时为null
     */
    public String consume(String username) {

        NonceEntry result = m_nonces_map.remove(username);
        if (!(result == null) && result.m_expiry >= System.currentTimeMillis()) {
            // 存在且有效 成功
            return result.m_nonce;
        }
        // 不存在或无效 失败
        return null;
    }

}
