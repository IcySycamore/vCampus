package edu.seu.vcampus.common.course;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 教室（值对象）：课程安排使用的物理教学场地。
 *
 * <p>教室属于某个学院，具备容量、位置（教学楼）与可用/偏好时间槽；额外支持标签
 * （如多媒体、机房等）。
 */
public class Classroom implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 教室唯一标识。 */
    private String m_uuid;

    /** 所属学院 uuid。 */
    private String m_college_uuid;

    /** 容量（可容纳人数）。 */
    private int m_capacity;

    /** 位置（教学楼，如「教一」）。 */
    private String m_location;

    /** 教室号，如「101」。 */
    private String m_name;

    /** 所属教学楼 uuid。 */
    private String m_building_uuid;

    /** 可用时间槽。 */
    private Set<Timeslot> m_available_timeslots = new HashSet<Timeslot>();

    /** 偏好时间槽。 */
    private Set<Timeslot> m_preference_timeslots = new HashSet<Timeslot>();

    /** 标签（extra）。 */
    private Set<String> m_tags = new HashSet<String>();

    /** 构造一个空教室对象，供对象流与数据访问层填充字段。 */
    public Classroom() {
    }

    /**
     * 构造一个教室。
     *
     * @param uuid 教室唯一标识
     * @param collegeUuid 所属学院 uuid
     * @param capacity 容量
     * @param location 位置
     */
    public Classroom(String uuid, String collegeUuid, int capacity, String location) {
        this.m_uuid = uuid;
        this.m_college_uuid = collegeUuid;
        this.m_capacity = capacity;
        this.m_location = location;
    }

    /** @return 教室唯一标识 */
    public String getUuid() {
        return m_uuid;
    }

    /** @param uuid 教室唯一标识 */
    public void setUuid(String uuid) {
        this.m_uuid = uuid;
    }

    /** @return 所属学院 uuid */
    public String getCollegeUuid() {
        return m_college_uuid;
    }

    /** @param collegeUuid 所属学院 uuid */
    public void setCollegeUuid(String collegeUuid) {
        this.m_college_uuid = collegeUuid;
    }

    /** @return 容量 */
    public int getCapacity() {
        return m_capacity;
    }

    /** @param capacity 容量 */
    public void setCapacity(int capacity) {
        this.m_capacity = capacity;
    }

    /** @return 位置 */
    public String getLocation() {
        return m_location;
    }

    /** @param location 位置 */
    public void setLocation(String location) {
        this.m_location = location;
    }

    /** @return 教室号 */
    public String getName() {
        return m_name;
    }

    /** @param name 教室号 */
    public void setName(String name) {
        this.m_name = name;
    }

    /** @return 所属教学楼 uuid */
    public String getBuildingUuid() {
        return m_building_uuid;
    }

    /** @param buildingUuid 所属教学楼 uuid */
    public void setBuildingUuid(String buildingUuid) {
        this.m_building_uuid = buildingUuid;
    }

    /** @return 可用时间槽集合 */
    public Set<Timeslot> getAvailableTimeslots() {
        return m_available_timeslots;
    }

    /** @param availableTimeslots 可用时间槽集合 */
    public void setAvailableTimeslots(Set<Timeslot> availableTimeslots) {
        this.m_available_timeslots = availableTimeslots == null
                ? new HashSet<Timeslot>() : availableTimeslots;
    }

    /** @return 偏好时间槽集合 */
    public Set<Timeslot> getPreferenceTimeslots() {
        return m_preference_timeslots;
    }

    /** @param preferenceTimeslots 偏好时间槽集合 */
    public void setPreferenceTimeslots(Set<Timeslot> preferenceTimeslots) {
        this.m_preference_timeslots = preferenceTimeslots == null
                ? new HashSet<Timeslot>() : preferenceTimeslots;
    }

    /** @return 标签集合 */
    public Set<String> getTags() {
        return m_tags;
    }

    /** @param tags 标签集合 */
    public void setTags(Set<String> tags) {
        this.m_tags = tags == null ? new HashSet<String>() : tags;
    }
}
