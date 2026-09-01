package edu.seu.vcampus.common.user;

import java.util.HashMap;
import java.util.Map;

/**
 * 教师身份（账户子类），在 {@link User} 基础上增加工号与职称。
 *
 * <p>
 * 院系取自个人档案 {@link HumanInfo}。
 */
public class Teacher extends User {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 工号。 */
    private String m_work_id;

    /** 职称。 */
    private Title m_title;

    /**
     * 构造一个空的教师身份。
     */
    public Teacher() {
    }

    /**
     * 构造并初始化教师身份。
     *
     * @param humanInfo 个人档案
     * @param userName  登录名
     * @param password  密码
     * @param workId    工号
     * @param title     职称
     */
    public Teacher(HumanInfo humanInfo, String userName, String password,
            String workId, Title title) {
        super(humanInfo, userName, password);
        this.m_work_id = workId;
        this.m_title = title;
    }

    /** @return 工号 */
    public String getWorkId() {
        return m_work_id;
    }

    /** @param workId 工号 */
    public void setWorkId(String workId) {
        this.m_work_id = workId;
    }

    /** @return 职称 */
    public Title getTitle() {
        return m_title;
    }

    /** @param title 职称 */
    public void setTitle(Title title) {
        this.m_title = title;
    }

    /**
     * 返回教师身份的传输附加信息（工号/职称/院系）。
     *
     * @return 附加信息键值
     */
    public Map<String, Object> toExtraInfo() {
        Map<String, Object> extra = new HashMap<String, Object>();
        extra.put("工号", m_work_id);
        extra.put("职称", m_title);
        HumanInfo info = getHumanInfo();
        extra.put("院系", info == null ? null : info.getDepartment());
        return extra;
    }
}