package edu.seu.vcampus.server.auth;

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

    @Override
    public void save(String username, String salt, String hash, String role) {
        users.put(username, new Credential(salt, hash, role));
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