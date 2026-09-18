package edu.seu.vcampus.common.course;

import java.io.Serializable;

/**
 * 教学楼（值对象）：容纳若干教室的物理楼宇，如「教一」「教二」。
 *
 * <p>教学楼与教室是两个层次：教学楼（{@code Building}）下挂多间教室（{@link Classroom}），
 * 教室通过 {@code buildingUuid} 归属到某栋教学楼。
 */
public class Building implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 教学楼唯一标识。 */
    private String m_uuid;

    /** 教学楼名称，如「教一」。 */
    private String m_name;

    /** 所属学院 uuid。 */
    private String m_college_uuid;

    /** 构造一个空教学楼对象，供对象流填充字段。 */
    public Building() {
    }

    /** @return 教学楼唯一标识 */
    public String getUuid() {
        return m_uuid;
    }

    /** @param uuid 教学楼唯一标识 */
    public void setUuid(String uuid) {
        this.m_uuid = uuid;
    }

    /** @return 教学楼名称 */
    public String getName() {
        return m_name;
    }

    /** @param name 教学楼名称 */
    public void setName(String name) {
        this.m_name = name;
    }

    /** @return 所属学院 uuid */
    public String getCollegeUuid() {
        return m_college_uuid;
    }

    /** @param collegeUuid 所属学院 uuid */
    public void setCollegeUuid(String collegeUuid) {
        this.m_college_uuid = collegeUuid;
    }
}
