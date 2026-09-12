package edu.seu.vcampus.server.user;

import java.util.List;

/**
 * 用户凭证存储（用户名 → uuid + 姓名 + 盐 + 加盐哈希 + 角色 + 启用位）。
 *
 * <p>
 * 接口化以便后续接入 MySQL DAO；当前使用内存实现。uuid 在注册时由服务端 生成，作为账户跨模块引用标识。
 *
 * <p>
 * 与 `sql/vCampus.sql` 的对应：{@code tblUser(uId, uName, uAge, uSex, uPwd, uRole)}，
 * 其中 uId=登录名、uName=姓名、uRole=角色显示名；uuid 需要建表时补列（见 ADR-0009 D7）。
 */
public interface UserRepository {

    /**
     * 保存用户凭证（姓名默认取登录名、启用位默认 true）。
     *
     * @param username 用户名
     * @param uuid 账户全局唯一标识（注册时生成）
     * @param salt 盐
     * @param hash 加盐哈希 sha256(salt + password)
     * @param role 角色
     */
    void save(String username, String uuid, String salt, String hash, String role);

    /**
     * 保存完整凭证（含姓名与启用位），已存在则覆盖。
     *
     * @param credential 凭证
     */
    void save(Credential credential);

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
     * 列出全部凭证（按登录名升序，保证分页结果稳定）。
     *
     * @return 凭证快照
     */
    List<Credential> findAll();

    /**
     * 修改姓名。
     *
     * @param username 用户名
     * @param displayName 新姓名
     */
    void update(String username, String displayName);

    /**
     * 设置启用位。
     *
     * @param username 用户名
     * @param enabled 是否启用
     */
    void setEnabled(String username, boolean enabled);

    /**
     * 替换凭证的盐与哈希（改密时换盐）。
     *
     * @param username 用户名
     * @param salt 新盐
     * @param hash 新哈希
     */
    void updateCredential(String username, String salt, String hash);

    /**
     * 删除账户（注销）。
     *
     * @param username 用户名
     */
    void delete(String username);

    /**
     * 用户凭证记录（可变：姓名/盐/哈希/启用位会随管理操作变化）。
     */
    class Credential {
        private final String username;
        private final String uuid;
        private String displayName;
        private String salt;
        private String hash;
        private final String role;
        private boolean enabled;

        /**
         * 兼容构造：姓名取登录名、启用位为 true。
         *
         * @param uuid 账户 uuid
         * @param salt 盐
         * @param hash 加盐哈希
         * @param role 角色显示名
         */
        Credential(String uuid, String salt, String hash, String role) {
            this(null, uuid, null, salt, hash, role, true);
        }

        /**
         * 完整构造。
         *
         * @param username 登录名
         * @param uuid 账户 uuid
         * @param displayName 姓名
         * @param salt 盐
         * @param hash 加盐哈希
         * @param role 角色显示名
         * @param enabled 是否启用
         */
        public Credential(String username, String uuid, String displayName, String salt,
                String hash, String role, boolean enabled) {
            this.username = username;
            this.uuid = uuid;
            this.displayName = displayName;
            this.salt = salt;
            this.hash = hash;
            this.role = role;
            this.enabled = enabled;
        }

        /** @return 登录名 */
        public String getUsername() {
            return username;
        }

        /** @return 账户全局唯一标识 */
        public String getUuid() {
            return uuid;
        }

        /** @return 姓名 */
        public String getDisplayName() {
            return displayName;
        }

        /** @param displayName 姓名 */
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        /** @return 盐 */
        public String getSalt() {
            return salt;
        }

        /** @param salt 新盐 */
        public void setSalt(String salt) {
            this.salt = salt;
        }

        /** @return 加盐哈希 */
        public String getHash() {
            return hash;
        }

        /** @param hash 新哈希 */
        public void setHash(String hash) {
            this.hash = hash;
        }

        /** @return 角色显示名 */
        public String getRole() {
            return role;
        }

        /** @return 是否启用 */
        public boolean isEnabled() {
            return enabled;
        }

        /** @param enabled 是否启用 */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}