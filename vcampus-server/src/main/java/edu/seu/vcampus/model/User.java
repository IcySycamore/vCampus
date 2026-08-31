package edu.seu.vcampus.model;

import java.io.Serializable;

/**
 * 用户实体类.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uId;
    private String uName;
    private Integer uAge;
    private String uSex;
    private String uPwd;
    private String salt;
    private String uRole;

    public User() {
    }

    public User(String uId, String uName, Integer uAge, String uSex,
                String uPwd, String salt, String uRole) {
        this.uId = uId;
        this.uName = uName;
        this.uAge = uAge;
        this.uSex = uSex;
        this.uPwd = uPwd;
        this.salt = salt;
        this.uRole = uRole;
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }

    public String getuName() {
        return uName;
    }

    public void setuName(String uName) {
        this.uName = uName;
    }

    public Integer getuAge() {
        return uAge;
    }

    public void setuAge(Integer uAge) {
        this.uAge = uAge;
    }

    public String getuSex() {
        return uSex;
    }

    public void setuSex(String uSex) {
        this.uSex = uSex;
    }

    public String getuPwd() {
        return uPwd;
    }

    public void setuPwd(String uPwd) {
        this.uPwd = uPwd;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getuRole() {
        return uRole;
    }

    public void setuRole(String uRole) {
        this.uRole = uRole;
    }
}