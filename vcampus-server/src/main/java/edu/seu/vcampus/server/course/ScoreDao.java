package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Score;

import java.util.ArrayList;
import java.util.List;

/**
 * 成绩数据访问对象：管理学生成绩记录。
 *
 * <p>
 * 成绩复用公共实体 {@link Score}，以「学生 uuid + 课程编号」唯一定位一条成绩。课程编号来自 {@code CourseSection.getCode()}。
 *
 * <p>
 * <b>没有内存表</b>：每次查询直接落到 {@link ScoreStore}（ADR-0011 记录了这次收敛）。原先这里持有一份 全量成绩
 * Map、构造时读回来、写时同步落库，问题是成绩表随选课与学期线性增长却常驻内存，而且 「内存改了但写库失败」时两边会不一致。改成按需查之后，本类只剩下参数校验与「把库里的记录号
 * 回填进实体」这点职责，含 {@code scId} 的自增号也交给数据库分配。
 */
public class ScoreDao {

    /** 持久化后端；由调用方显式传入，没有默认值。 */
    private final ScoreStore m_store;

    /**
     * 指定持久化后端构造。
     *
     * @param store 持久化后端，不能为 null
     * @throws IllegalArgumentException store 为 null
     */
    public ScoreDao(ScoreStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
        m_store = store;
    }

    /**
     * 按「学生 uuid + 课程编号」查一条成绩。
     *
     * @param studentUuid 学生 uuid
     * @param courseCode  课程编号
     * @return 成绩，不存在返回 null
     */
    public Score find(String studentUuid, String courseCode) {
        if (studentUuid == null || courseCode == null) {
            return null;
        }
        return m_store.find(studentUuid, courseCode);
    }

    /**
     * 查某学生的全部成绩。
     *
     * @param studentUuid 学生 uuid
     * @return 成绩列表，不返回 null
     */
    public List<Score> findByStudent(String studentUuid) {
        if (studentUuid == null) {
            return new ArrayList<Score>();
        }
        return m_store.findByStudent(studentUuid);
    }

    /**
     * 查某课程的全部成绩。
     *
     * @param courseCode 课程编号
     * @return 成绩列表，不返回 null
     */
    public List<Score> findByCourse(String courseCode) {
        if (courseCode == null) {
            return new ArrayList<Score>();
        }
        return m_store.findByCourse(courseCode);
    }

    /**
     * 保存成绩：按「学生 + 课程编号 + 学期」覆盖旧值，落库后把记录号回填进实体。
     *
     * @param score 成绩
     * @return 是否成功
     */
    public boolean save(Score score) {
        if (score == null) {
            return false;
        }
        Long stored = m_store.save(score);
        if (stored == null) {
            return false;
        }
        score.setId(stored);
        return true;
    }

    /**
     * 删除某学生某课程的成绩（不限学期）。
     *
     * @param studentUuid 学生 uuid
     * @param courseCode  课程编号
     * @return 是否成功
     */
    public boolean delete(String studentUuid, String courseCode) {
        if (studentUuid == null || courseCode == null) {
            return false;
        }
        return m_store.delete(studentUuid, courseCode);
    }
}
