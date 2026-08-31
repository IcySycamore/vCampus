package edu.seu.vcampus.common.user;



import java.io.Serializable;


/**
 * 人的基本信息档案。
 *
 * <p>
 * 作为登录成功后回传的个人资料载体，须实现 {@link Serializable} 以便在
 * {@code Message} 中随对象流传输（见 ADR-0006）。
 */
public class HumanInfo implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 性别枚举。 */
    public enum Gender {
        /** 男 */
        MALE,
        /** 女 */
        FEMALE,
        /** 其他 */
        OTHER
    }



    /** 登录 ID。 */
    private String m_id;

    /** 姓名。 */
    private String m_name;

    /** 电话。 */
    private String m_tel;

    /** 家庭住址。 */
    private String m_home_address;

    /** 工作地址。 */
    private String m_work_address;

    /** 年龄。 */
    private int m_age;

    /** 性别。 */
    private Gender m_gender;

    /**
     * 构造一个空档案，并自动生成唯一标识。
     */
    public HumanInfo() {

    }

    /**
     * 构造并初始化全部档案字段。
     *
     * @param id          登录 ID
     * @param name        姓名
     * @param tel         电话
     * @param homeAddress 家庭住址
     * @param workAddress 工作地址
     * @param age         年龄
     * @param gender      性别
     */
    public HumanInfo(String id, String name, String tel, String homeAddress,
            String workAddress, int age, Gender gender) {
        this();
        this.m_id = id;
        this.m_name = name;
        this.m_tel = tel;
        this.m_home_address = homeAddress;
        this.m_work_address = workAddress;
        this.m_age = age;
        this.m_gender = gender;
    }



    /** @return 登录 ID */
    public String getId() {
        return m_id;
    }

    /** @param id 登录 ID */
    public void setId(String id) {
        this.m_id = id;
    }

    /** @return 姓名 */
    public String getName() {
        return m_name;
    }

    /** @param name 姓名 */
    public void setName(String name) {
        this.m_name = name;
    }

    /** @return 电话 */
    public String getTel() {
        return m_tel;
    }

    /** @param tel 电话 */
    public void setTel(String tel) {
        this.m_tel = tel;
    }

    /** @return 家庭住址 */
    public String getHomeAddress() {
        return m_home_address;
    }

    /** @param homeAddress 家庭住址 */
    public void setHomeAddress(String homeAddress) {
        this.m_home_address = homeAddress;
    }

    /** @return 工作地址 */
    public String getWorkAddress() {
        return m_work_address;
    }

    /** @param workAddress 工作地址 */
    public void setWorkAddress(String workAddress) {
        this.m_work_address = workAddress;
    }

    /** @return 年龄 */
    public int getAge() {
        return m_age;
    }

    /** @param age 年龄 */
    public void setAge(int age) {
        this.m_age = age;
    }

    /** @return 性别 */
    public Gender getGender() {
        return m_gender;
    }

    /** @param gender 性别 */
    public void setGender(Gender gender) {
        this.m_gender = gender;
    }
}
