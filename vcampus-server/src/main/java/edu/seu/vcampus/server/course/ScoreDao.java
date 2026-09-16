package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Score;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 成绩数据访问对象（内存实现）：管理学生成绩记录。
 *
 * <p>成绩复用公共实体 {@link Score}，以「学生 uuid + 课程编号」唯一定位一条成绩。
 * 课程编号来自 {@code CourseSection.getCode()}。骨架期为内存存储，后续可替换为 JDBC 实现。
 */
public class ScoreDao {

    /** 主键 → 成绩。 */
    private final ConcurrentMap<Long, Score> m_scores = new ConcurrentHashMap<Long, Score>();

    /** 自增主键计数器（模拟数据库自增分配）。 */
    private final AtomicLong m_next_id = new AtomicLong(1L);

    /** 构造一个空的内存成绩数据访问对象。 */
    public ScoreDao() {
    }

    /**
     * 按「学生 uuid + 课程编号」查一条成绩。
     *
     * @param studentUuid 学生 uuid
     * @param courseCode 课程编号
     * @return 成绩，不存在返回 null
     */
    public Score find(String studentUuid, String courseCode) {
        if (studentUuid == null || courseCode == null) {
            return null;
        }
        for (Score score : m_scores.values()) {
            if (studentUuid.equals(score.getStudentUuid())
                    && courseCode.equals(score.getCourseCode())) {
                return score;
            }
        }
        return null;
    }

    /**
     * 查某学生的全部成绩。
     *
     * @param studentUuid 学生 uuid
     * @return 成绩列表
     */
    public List<Score> findByStudent(String studentUuid) {
        List<Score> result = new ArrayList<Score>();
        if (studentUuid == null) {
            return result;
        }
        for (Score score : m_scores.values()) {
            if (studentUuid.equals(score.getStudentUuid())) {
                result.add(score);
            }
        }
        return result;
    }

    /**
     * 查某课程的全部成绩。
     *
     * @param courseCode 课程编号
     * @return 成绩列表
     */
    public List<Score> findByCourse(String courseCode) {
        List<Score> result = new ArrayList<Score>();
        if (courseCode == null) {
            return result;
        }
        for (Score score : m_scores.values()) {
            if (courseCode.equals(score.getCourseCode())) {
                result.add(score);
            }
        }
        return result;
    }

    /** @return 全部成绩快照 */
    public List<Score> findAll() {
        return new ArrayList<Score>(m_scores.values());
    }

    /**
     * 保存成绩（覆盖「同一学生同一课程」的旧成绩；主键缺失时自动分配）。
     *
     * @param score 成绩
     * @return 是否成功
     */
    public boolean save(Score score) {
        if (score == null) {
            return false;
        }
        Score existing = find(score.getStudentUuid(), score.getCourseCode());
        if (existing != null) {
            existing.setScore(score.getScore());
            existing.setSemester(score.getSemester());
            return true;
        }
        if (score.getId() == null) {
            score.setId(m_next_id.getAndIncrement());
        }
        m_scores.put(score.getId(), score);
        return true;
    }

    /**
     * 删除某学生某课程的成绩。
     *
     * @param studentUuid 学生 uuid
     * @param courseCode 课程编号
     * @return 是否成功
     */
    public boolean delete(String studentUuid, String courseCode) {
        Score existing = find(studentUuid, courseCode);
        if (existing == null) {
            return false;
        }
        m_scores.remove(existing.getId());
        return true;
    }
}
