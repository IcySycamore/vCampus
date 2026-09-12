package edu.seu.vcampus.server.user;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 【内存版】用户凭证存储
 *
 * <p>
 * 线程安全（ConcurrentHashMap）。
 */
public class InMemoryUserRepository implements UserRepository {

    /** 用户名 → 凭证。 */
    private final Map<String, Credential> users = new ConcurrentHashMap<String, Credential>();

    /** uuid → 凭证（学籍等模块只拿得到 uuid，需要按 uuid 反查姓名）。 */
    private final Map<String, Credential> byUuid =
            new ConcurrentHashMap<String, Credential>();

    /** 单例实例。 */
    private static InMemoryUserRepository instance;

    /**
     * 获取内存版凭证存储单例。
     *
     * @return 凭证存储
     */
    public static synchronized InMemoryUserRepository getInstance() {
        if (instance == null) {
            instance = new InMemoryUserRepository();
        }
        return instance;
    }

    @Override
    public void save(String username, String uuid, String salt, String hash, String role) {
        save(username, uuid, salt, hash, role, null);
    }

    @Override
    public void save(String username, String uuid, String salt, String hash, String role,
            String realName) {
        Credential credential = new Credential(uuid, salt, hash, role, realName);
        users.put(username, credential);
        byUuid.put(uuid, credential);
    }

    @Override
    public Credential findByUuid(String uuid) {
        return uuid == null ? null : byUuid.get(uuid);
    }

    @Override
    public Credential findByUsername(String username) {
        return users.get(username);
    }

    @Override
    public boolean exists(String username) {
        return users.containsKey(username);
    }
}