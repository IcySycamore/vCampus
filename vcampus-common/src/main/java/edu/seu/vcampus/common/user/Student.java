package edu.seu.vcampus.common.user;

import java.util.HashMap;
import java.util.Map;

/**
 * 学生身份（账户子类），在 {@link User} 基础上增加学号。
 *
 * <p>
 * 院系与专业取自个人档案 {@link HumanInfo}。
 */
public class Student extends User {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造一个空的学生身份。
     */
    public Student() {
    }

    /**
     * 构造并初始化学生身份。
     *
     * @param humanInfo 个人档案
     * @param userName  登录名
     * @param password  密码
     */
    public Student(HumanInfo humanInfo, String userName, String password) {
        super(humanInfo, userName, password);
    }
    /**
     * 返回学生身份的传输附加信息（学号/院系/专业）。
     *
     * @return 附加信息键值
     */
    public Map<String, Object> toExtraInfo() {
        Map<String, Object> extra = new HashMap<String, Object>();
        HumanInfo info = getHumanInfo();
        extra.put("院系", info == null ? null : info.getDepartment());
        extra.put("专业", info == null ? null : info.getMajor());
        return extra;
    }
}
