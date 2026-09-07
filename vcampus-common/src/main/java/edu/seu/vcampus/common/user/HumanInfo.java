package edu.seu.vcampus.common.user;

import edu.seu.vcampus.common.random.RandomGen;
import java.io.Serializable;
import java.util.UUID;

/**
 * 人的基本信息档案。
 *
 * <p>
 * 作为登录成功后回传的个人资料载体，须实现 {@link Serializable} 以便在
 * {@code Message} 中随对象流传输。
 */
public class HumanInfo implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 唯一标识（UUID，主键）。 */
    private UUID m_uuid;

    /** 性别枚举。 */
    public enum Gender {
        /** 男 */
        MALE,
        /** 女 */
        FEMALE,
    }

    /** ID 1(身份证号) */
    private String m_id_0;
    /** ID 2(学号) */
    private String m_id_1;

    /** 姓名 */
    private String m_name;

    /** 电话 */
    private String m_tel;

    /** 家庭住址。 */
    private String m_home_address;

    /** 学校/工作地址。 */
    private String m_work_address;

    /** 年龄。 */
    private int m_age;

    /** 性别。 */
    private Gender m_gender;
    /** 学院。 */
    private Department m_department;

    /** 专业。 */
    private Major m_major;

    /** 职称。 */
    private Title m_title;

    /**
     * 构造一个空档案，并生成唯一标识。
     */
    public HumanInfo() {
        m_uuid = new RandomGen().getUuid();
    }

    /**
     * 构造并初始化部分档案字段。
     *
     * @param id          登录 ID
     * @param name        姓名
     * @param tel         电话
     * @param homeAddress 家庭住址
     * @param workAddress 工作地址
     * @param age         年龄
     * @param gender      性别
     */
    public HumanInfo(String id_0, String id_1, String name, String tel, String homeAddress,
            String workAddress, int age, Gender gender) {
        this();
        this.m_id_0 = id_0;
        this.m_id_1 = id_1;
        this.m_name = name;
        this.m_tel = tel;
        this.m_home_address = homeAddress;
        this.m_work_address = workAddress;
        this.m_age = age;
        this.m_gender = gender;
    }

    /** @return 身份证 ID */
    public String getId() {
        return m_id_0;
    }

    /** @param id 身份证 ID */
    public void setId(String id) {
        this.m_id_0 = id;
    }

    public UUID getUuid() {
        return m_uuid;
    }

    /** @param uuid 唯一标识 UUID */
    public void setUuid(UUID uuid) {
        this.m_uuid = uuid;
    }

    /**
     * @return
     *         /** @return 身份证 ID
     */
    public String getStudentNumber() {
        return m_id_1;
    }

    /** @param id 身份证 ID */
    public void setStudentNumber(String id) {
        this.m_id_1 = id;
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

    /** @return 学院 */
    public Department getDepartment() {
        return m_department;
    }

    /** @param department 学院 */
    public void setDepartment(Department department) {
        this.m_department = department;
    }

    /** @return 专业 */
    public Major getMajor() {
        return m_major;
    }

    /** @param major 专业 */
    public void setMajor(Major major) {
        this.m_major = major;
    }

    /** @return 职称 */
    public Title getTitle() {
        return m_title;
    }

    /** @param title 职称 */
    public void setTitle(Title title) {
        this.m_title = title;
    }
}
