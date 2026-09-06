package edu.seu.vcampus.common.entity;

import java.io.Serializable;

/**
 * 用户实体（登录账户：学生/教师/管理员），对应表 tblUser。
 *
 * <p>本类由客户端与服务器端共享：登录响应会将 User 放入 {@code Message.data} 传输，
 * 因此必须实现 {@link java.io.Serializable}（见 ADR-0006）。
 */
public class User implements Serializable {

    /** 序列化版本号（协议兼容依据，字段变更时谨慎修改）。 */
    private static final long serialVersionUID = 1L;

    /** 登录ID。 */
    private String uId;

    /** 姓名。 */
    private String uName;

    /** 年龄。 */
    private Integer uAge;

    /** 性别：男/女。 */
    private String uSex;

    /** 密码。 */
    private String uPwd;

    /** 角色：学生/教师/管理员。 */
    private String uRole;

    /**
     * 构造一个空用户。
     */
    public User() {
    }

    /**
     * 构造一个完整用户。
     *
     * @param uId 登录ID
     * @param uName 姓名
     * @param uAge 年龄
     * @param uSex 性别
     * @param uPwd 密码
     * @param uRole 角色
     */
    public User(String uId, String uName, Integer uAge, String uSex, String uPwd, String uRole) {
        this.uId = uId;
        this.uName = uName;
        this.uAge = uAge;
        this.uSex = uSex;
        this.uPwd = uPwd;
        this.uRole = uRole;
    }

    /** @return 登录ID */
    public String getuId() {
        return uId;
    }

    /** @param uId 登录ID */
    public void setuId(String uId) {
        this.uId = uId;
    }

    /** @return 姓名 */
    public String getuName() {
        return uName;
    }

    /** @param uName 姓名 */
    public void setuName(String uName) {
        this.uName = uName;
    }

    /** @return 年龄 */
    public Integer getuAge() {
        return uAge;
    }

    /** @param uAge 年龄 */
    public void setuAge(Integer uAge) {
        this.uAge = uAge;
    }

    /** @return 性别 */
    public String getuSex() {
        return uSex;
    }

    /** @param uSex 性别 */
    public void setuSex(String uSex) {
        this.uSex = uSex;
    }

    /** @return 密码 */
    public String getuPwd() {
        return uPwd;
    }

    /** @param uPwd 密码 */
    public void setuPwd(String uPwd) {
        this.uPwd = uPwd;
    }

    /** @return 角色 */
    public String getuRole() {
        return uRole;
    }

    /** @param uRole 角色 */
    public void setuRole(String uRole) {
        this.uRole = uRole;
    }
}
