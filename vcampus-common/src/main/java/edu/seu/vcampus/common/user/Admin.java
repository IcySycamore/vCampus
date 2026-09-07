package edu.seu.vcampus.common.user;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员身份（账户子类），在 {@link User} 基础上增加超级管理员权限标记。
 */
public class Admin extends User {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 是否超级管理员（人员管理权限标记）。 */
    private boolean m_super_admin;

    /**
     * 构造一个空的管理员身份。
     */
    public Admin() {
    }

    /**
     * 构造并初始化管理员身份。
     *
     * @param humanInfo  个人档案
     * @param userName   登录名
     * @param password   密码
     * @param superAdmin 是否超级管理员
     */
    public Admin(HumanInfo humanInfo, String userName, String password,
            boolean superAdmin) {
        super(humanInfo, userName, password);
        this.m_super_admin = superAdmin;
    }

    /** @return 是否超级管理员 */
    public boolean isSuperAdmin() {
        return m_super_admin;
    }

    /** @param superAdmin 是否超级管理员 */
    public void setSuperAdmin(boolean superAdmin) {
        this.m_super_admin = superAdmin;
    }

    /**
     * 返回管理员身份的传输附加信息（超级管理员标记）。
     *
     * @return 附加信息键值
     */
    public Map<String, Object> toExtraInfo() {
        Map<String, Object> extra = new HashMap<String, Object>();
        extra.put("超级管理员", m_super_admin);
        return extra;
    }
}