package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.util.List;

/**
 * 学籍数据访问接口（学籍档案表 {@code tblStudentProfile} 的增删改查契约）。
 *
 * <p>
 * 接口与实现分离（见 ADR-0003 纵向划分、CONTEXT.md 四层结构）：业务层 {@link StudentService} 只依赖本接口，不关心底层怎么存。实现只有 JDBC
 * 一份（{@link StudentDaoJdbc}），内存与文件版 都已删除 —— 学籍是全局共享状态，两份实现只会让测试与生产各跑一套。
 *
 * <p>
 * <b>筛选、排序、分页都不在本层</b>：它们由 {@link StudentService} 在补完姓名之后做。这不是
 * 分层洁癖——「按姓名搜」需要姓名，而姓名是业务层联查用户模块算出来的，DAO 拿它没地方拿；把过滤 放在这里，那两个条件（名字、学号）里就会有一个永远搜不到。
 */
public interface StudentDao {

    /**
     * 按学籍记录主键查单条记录。
     *
     * @param id 学籍记录主键
     * @return 记录，不存在返回 null
     */
    StudentProfile findById(Long id);

    /**
     * 按所属用户账户 uuid 查学籍记录（用户与学籍一一对应）。
     *
     * @param userUuid 用户账户 uuid
     * @return 记录，不存在返回 null
     */
    StudentProfile findByUserUuid(String userUuid);

    /**
     * 列出全部未删除的学籍记录。
     *
     * @return 记录列表
     */
    List<StudentProfile> findAll();

    /**
     * 新增一条学籍记录。
     *
     * @param profile 学籍记录
     * @return 是否成功
     */
    boolean insert(StudentProfile profile);

    /**
     * 更新一条学籍记录（按主键定位）。
     *
     * @param profile 学籍记录（主键必须已存在）
     * @return 是否成功
     */
    boolean update(StudentProfile profile);

    /**
     * 软删除一条学籍记录：置删除标记，不物理删除。
     *
     * @param id 学籍记录主键
     * @return 是否成功
     */
    boolean softDelete(Long id);
}
