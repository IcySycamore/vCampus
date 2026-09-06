package edu.seu.vcampus.common.user;

import edu.seu.vcampus.common.random.RandomGen;
import java.io.Serializable;
import java.util.UUID;

/**
 * 用户账户基类（登录认证主体），持有个人档案 {@link HumanInfo}。
 *
 * <p>
 * 抽象类：具体身份（学生/教师/管理员）以子类实现。
 */
public abstract class User implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 个人档案。 */
    private HumanInfo m_humanInfo;

    /** 唯一标识。 */
    private UUID m_uuid;

    /** 登录名。 */
    private String m_user_name;

    /** 密码（仅服务器端认证用，transient 不参与序列化传输）。 */
    private transient String m_password;

    /**
     * 构造一个空的用户账户，并自动生成唯一标识。
     */
    public User() {
        m_uuid = new RandomGen().getUuid();
    }

    /**
     * 构造并初始化用户账户。
     *
     * @param humanInfo 个人档案
     * @param userName  登录名
     * @param password  密码
     */
    public User(HumanInfo humanInfo, String userName, String password) {
        this();
        this.m_humanInfo = humanInfo;
        this.m_user_name = userName;
        this.m_password = password;
    }

    /** @return 个人档案 */
    public HumanInfo getHumanInfo() {
        return m_humanInfo;
    }

    /** @param humanInfo 个人档案 */
    public void setHumanInfo(HumanInfo humanInfo) {
        this.m_humanInfo = humanInfo;
    }

    /** @return 唯一标识 */
    public UUID getUuid() {
        return m_uuid;
    }

    /** @param uuid 唯一标识 */
    public void setUuid(UUID uuid) {
        this.m_uuid = uuid;
    }

    /** @return 登录名 */
    public String getUserName() {
        return m_user_name;
    }

    /** @param userName 登录名 */
    public void setUserName(String userName) {
        this.m_user_name = userName;
    }

    /** @return 密码 */
    public String getPassword() {
        return m_password;
    }

    /** @param password 密码 */
    public void setPassword(String password) {
        this.m_password = password;
    }
}
