package edu.seu.vcampus.server.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
        users.put(username, new Credential(username, uuid, username, salt, hash, role, true));
    }

    @Override
    public void save(Credential credential) {
        if (credential == null || credential.getUsername() == null) {
            throw new IllegalArgumentException("credential and its username must not be null");
        }
        users.put(credential.getUsername(), credential);
    }

    @Override
    public Credential findByUsername(String username) {
        return username == null ? null : users.get(username);
    }

    @Override
    public Credential findByUuid(String uuid) {
        if (uuid == null) {
            return null;
        }
        for (Credential credential : users.values()) {
            if (uuid.equals(credential.getUuid())) {
                return credential;
            }
        }
        return null;
    }

    @Override
    public boolean exists(String username) {
        return username != null && users.containsKey(username);
    }

    @Override
    public List<Credential> findAll() {
        List<Credential> all = new ArrayList<Credential>(users.values());
        Collections.sort(all, new Comparator<Credential>() {
            @Override
            public int compare(Credential left, Credential right) {
                String first = left.getUsername() == null ? "" : left.getUsername();
                String second = right.getUsername() == null ? "" : right.getUsername();
                return first.compareTo(second);
            }
        });
        return all;
    }

    @Override
    public void update(String username, String displayName) {
        Credential credential = findByUsername(username);
        if (credential != null) {
            credential.setDisplayName(displayName);
        }
    }

    @Override
    public void setEnabled(String username, boolean enabled) {
        Credential credential = findByUsername(username);
        if (credential != null) {
            credential.setEnabled(enabled);
        }
    }

    @Override
    public void updateCredential(String username, String salt, String hash) {
        Credential credential = findByUsername(username);
        if (credential != null) {
            credential.setSalt(salt);
            credential.setHash(hash);
        }
    }

    @Override
    public void delete(String username) {
        if (username != null) {
            users.remove(username);
        }
    }
}