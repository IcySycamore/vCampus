package edu.seu.vcampus.model;

public class User {
    private String uId;
    private String uName;
    private Integer uAge;
    private String uSex;
    private String uPwd;
    private String uRole;

    public User() {}

    public User(String uId, String uName, Integer uAge, String uSex, String uPwd, String uRole) {
        this.uId = uId;
        this.uName = uName;
        this.uAge = uAge;
        this.uSex = uSex;
        this.uPwd = uPwd;
        this.uRole = uRole;
    }

    // Getter 和 Setter
    public String getuId() { return uId; }
    public void setuId(String uId) { this.uId = uId; }

    public String getuName() { return uName; }
    public void setuName(String uName) { this.uName = uName; }

    public Integer getuAge() { return uAge; }
    public void setuAge(Integer uAge) { this.uAge = uAge; }

    public String getuSex() { return uSex; }
    public void setuSex(String uSex) { this.uSex = uSex; }

    public String getuPwd() { return uPwd; }
    public void setuPwd(String uPwd) { this.uPwd = uPwd; }

    public String getuRole() { return uRole; }
    public void setuRole(String uRole) { this.uRole = uRole; }
}
