package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Score;

import java.util.List;

/**
 * 成绩的持久化后端。
 *
 * <p>
 * {@link ScoreDao} 仍在内存里持有成绩表，本接口只负责「把变更写下去、启动时读回来」，与银行模块 的 {@code BankStore} 同一套做法：缺省是不落库的
 * {@link ScoreStoreMemory}， {@code -Dvcampus.store=jdbc} 时换成 {@link ScoreStoreJdbc}。
 *
 * <p>
 * <b>标识</b>：成绩对外按「学生 uuid + 课程编号 + 学期」定位，主键不是它；表里另有自增的 {@code scId}，只用于回填
 * {@link Score#setId(Long)}。注意 {@link Score} 引用课程用的是<b>课程 编号</b>而不是 uuid，落库时要把编号换算成
 * {@code tblCourse.coUuid}（成绩表上有指向课程的外键）， 换算不到就说明课程不存在，直接报错，不写空行。
 *
 * <p>
 * <b>学期</b>：{@code ScoreDao.find} 只按学生 + 课程编号查，忽略学期，所以 {@link #delete} 也按
 * 这两个条件删（可能连带删掉该课的多个学期）；单学期场景下两者行为一致。
 */
public interface ScoreStore {

    /**
     * 加载全部成绩，用于服务启动时恢复内存状态。
     *
     * @return 成绩列表，不返回 null
     */
    List<Score> loadAll();

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
