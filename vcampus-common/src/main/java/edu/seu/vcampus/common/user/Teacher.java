package edu.seu.vcampus.common.user;

import java.util.HashMap;
import java.util.Map;

/**
 * 教师身份（账户子类），职称等资料取自个人档案 {@link HumanInfo}。
 */
public class Teacher extends User {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

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
     */
    public Teacher(HumanInfo humanInfo, String userName, String password) {
        super(humanInfo, userName, password);
    }

    /**
     * 返回教师身份的传输附加信息（职称/院系，取自档案）。
     *
     * @return 附加信息键值
     */
    public Map<String, Object> toExtraInfo() {
        Map<String, Object> extra = new HashMap<String, Object>();
        HumanInfo info = getHumanInfo();
        extra.put("职称", info == null ? null : info.getTitle());
        extra.put("院系", info == null ? null : info.getDepartment());
        return extra;
    }
}