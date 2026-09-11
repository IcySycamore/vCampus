package edu.seu.vcampus.server.user;

/**
 * 用户凭证存储（用户名 → uuid + 盐 + 加盐哈希 + 角色）。
 *
 * <p>
 * 接口化以便后续接入 MySQL DAO；当前使用内存实现。uuid 在注册时由服务端 生成，作为账户跨模块引用标识。
 */
public interface UserRepository {

    /**
     * 保存用户凭证。
     *
     * @param username 用户名
     * @param uuid 账户全局唯一标识（注册时生成）
     * @param salt 盐
     * @param hash 加盐哈希 sha256(salt + password)
     * @param role 角色
     */
    void save(String username, String uuid, String salt, String hash, String role);

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
        private final String uuid;
        private final String salt;
        private final String hash;
        private final String role;

        Credential(String uuid, String salt, String hash, String role) {
            this.uuid = uuid;
            this.salt = salt;
            this.hash = hash;
            this.role = role;
        }

        /** @return 账户全局唯一标识 */
        public String getUuid() {
            return uuid;
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