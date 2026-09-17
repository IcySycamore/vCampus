package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Score;

import java.util.List;

/**
 * 成绩的持久化后端。
 *
 * <p>
 * 本接口是成绩的<b>唯一</b>数据来源：没有内存缓存，每次查询直接落 SQL。成绩表会随选课与学期线性 增长，而「查某人某课的成绩」「查某人的全部成绩」「查某门课的全部成绩」都能用索引定位，
 * 没有理由把全表装进内存（ADR-0011 记录了这次收敛）。
 *
 * <p>
 * <b>标识</b>：成绩对外按「学生 uuid + 课程编号 + 学期」定位，主键不是它；表里另有自增的 {@code scId}，只用于回填
 * {@link Score#setId(Long)}。注意 {@link Score} 引用课程用的是<b>课程 编号</b>而不是 uuid，落库时要把编号换算成
 * {@code tblCourse.coUuid}（成绩表上有指向课程的外键）， 换算不到就说明课程不存在，直接报错，不写空行。
 */
public interface ScoreStore {

    /**
     * 查一条成绩（学生 + 课程编号）。
     *
     * <p>
     * 同一条记录可能有多个学期，这里按下标顺序返回第一个，不指定学期的调用方拿到的结果与内存实现 时期一致。
     *
     * @param studentUuid 学生 uuid
     * @param courseCode  课程编号
     * @return 成绩；不存在返回 null
     */
    Score find(String studentUuid, String courseCode);

    /**
     * 查某学生的全部成绩。
     *
     * @param studentUuid 学生 uuid
     * @return 成绩列表，不返回 null
     */
    List<Score> findByStudent(String studentUuid);

    /**
     * 查某课程的全部成绩。
     *
     * @param courseCode 课程编号
     * @return 成绩列表，不返回 null
     */
    List<Score> findByCourse(String courseCode);

    /**
     * 写入或覆盖一条成绩（按「学生 + 课程编号 + 学期」定位）。
     *
     * @param score 成绩
     * @return 落库后的记录号；写入失败返回 null
     */
    Long save(Score score);

    /**
     * 删除某学生某课程的成绩（不限学期）。
     *
     * @param studentUuid 学生 uuid
     * @param courseCode  课程编号
     * @return 命中记录为 true
     */
    boolean delete(String studentUuid, String courseCode);
}
