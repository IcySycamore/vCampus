package edu.seu.vcampus.server.auth;

/**
 * 用户凭证存储（用户名 → 盐 + 加盐哈希 + 角色）。
 *
 * <p>
 * 接口化以便后续接入 MySQL DAO；当前使用内存实现。
 */
public interface UserRepository {

    /**
     * 保存用户凭证。
     *
     * @param username 用户名
     * @param salt     盐
     * @param hash     加盐哈希 sha256(salt + password)
     * @param role     角色
     */
    void save(String username, String salt, String hash, String role);

    /**
     * 按用户名查询凭证。
     *
     * @param username 用户名
     * @return 凭证；不存在返回 null
     */
    Credential findByUsername(String username);

    /**
     * 判断用户名是否已存在。
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean exists(String username);

    /**
     * 用户凭证记录。
     */
    class Credential {
        private final String salt;
        private final String hash;
        private final String role;

        Credential(String salt, String hash, String role) {
            this.salt = salt;
            this.hash = hash;
            this.role = role;
        }

        /** @return 盐 */
        public String getSalt() {
            return salt;
        }

        /** @return 加盐哈希 */
        public String getHash() {
            return hash;
        }

        /** @return 角色 */
        public String getRole() {
            return role;
        }
    }
}