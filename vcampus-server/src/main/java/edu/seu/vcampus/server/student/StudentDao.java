package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.util.List;

/**
 * 学籍数据访问接口（学籍表 tblStudentRecord 的增删改查契约）。
 *
 * <p>
 * 接口与实现分离（见 ADR-0003 纵向划分、CONTEXT.md 四层结构）： 业务层 {@link StudentService}
 * 只依赖本接口，不关心底层是内存还是 JDBC。 数据库实现待 DbHelper（魏雨霏）合入后补充 {@code StudentDaoJdbc}。
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

    /**
     * 按条件分页查询学籍记录（命令 208）。
     *
     * <p>
     * 只返回未删除记录；{@code query} 为 null 表示不加过滤条件。分页由调用方算出
     * {@code offset}（{@code (pageNumber - 1) * pageSize}），本层不做页码换算，
     * 以便两种实现（内存 / JDBC）行为一致。
     *
     * @param query 过滤条件（null 表示全部）
     * @param offset 起始下标（从 0 开始）
     * @param limit 最多返回条数
     * @return 记录列表（无匹配返回空列表，不返回 null）
     */
    List<StudentProfile> find(StudentQuery query, int offset, int limit);

    /**
     * 统计满足条件的学籍记录数（与 {@link #find} 配对，用于算总页数）。
     *
     * @param query 过滤条件（null 表示全部）
     * @return 记录总数
     */
    long count(StudentQuery query);
}
