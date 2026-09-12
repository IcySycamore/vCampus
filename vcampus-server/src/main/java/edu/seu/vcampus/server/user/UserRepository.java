package edu.seu.vcampus.server.user;

/**
 * 用户凭证存储（用户名 → uuid + 盐 + 加盐哈希 + 角色）。
 *
 * <p>
 * 接口化以便后续接入 MySQL DAO；当前使用内存实现。uuid 在注册时由服务端 生成，作为账户跨模块引用标识。
 */
public interface UserRepository {

    /**
     * 保存用户凭证（不采集姓名）。
     *
     * @param username 用户名
     * @param uuid 账户全局唯一标识（注册时生成）
     * @param salt 盐
     * @param hash 加盐哈希 sha256(salt + password)
     * @param role 角色
     */
    void save(String username, String uuid, String salt, String hash, String role);

    /**
     * 保存用户凭证（含姓名）。
     *
     * @param username 用户名
     * @param uuid 账户全局唯一标识（注册时生成）
     * @param salt 盐
     * @param hash 加盐哈希 sha256(salt + password)
     * @param role 角色
     * @param realName 真实姓名（管理员账号可为 null）
     */
    void save(String username, String uuid, String salt, String hash, String role,
            String realName);

    /**
     * 按用户名查询凭证。
     *
     * @param username 用户名
     * @return 凭证；不存在返回 null
     */
    Credential findByUsername(String username);

    /**
     * 按账户 uuid 查询凭证。
     *
     * <p>
     * 学籍等业务模块只拿得到 uuid（不存登录名），需要在返回档案时补上姓名，就靠本方法反查。
     *
     * @param uuid 账户全局唯一标识
     * @return 凭证；不存在返回 null
     */
    Credential findByUuid(String uuid);

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
        private final String realName;

        Credential(String uuid, String salt, String hash, String role) {
            this(uuid, salt, hash, role, null);
        }

        Credential(String uuid, String salt, String hash, String role, String realName) {
            this.uuid = uuid;
            this.salt = salt;
            this.hash = hash;
            this.role = role;
            this.realName = realName;
        }

        /** @return 真实姓名；未采集返回 null */
        public String getRealName() {
            return realName;
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