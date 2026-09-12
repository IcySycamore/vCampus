package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;

import java.util.List;

/**
 * 学籍修改申请单的数据访问接口。
 *
 * <p>
 * 与 {@link StudentDao}（学籍记录本体）分开建接口，有两个原因：
 * <ul>
 * <li><b>职责不同</b>：学籍记录是「当前状态」，申请单是「审核流程的流水」，
 * 两者生命周期与查询方式都不同；合并会让实现类迅速膨胀（也会撞上单文件 200 行上限）；</li>
 * <li><b>可替换</b>：审核流将来可能单独落表存历史，与学籍表分开演进。</li>
 * </ul>
 * 实现同样分内存版与将来的 JDBC 版，业务层只依赖本接口。
 */
public interface StudentModifyRequestDao {

    /**
     * 新增一条申请单。
     *
     * @param request 申请单（主键为 null 时由存储分配并回填）
     * @return 是否成功
     */
    boolean insert(StudentModifyRequest request);

    /**
     * 按主键查询申请单。
     *
     * @param requestId 申请单主键
     * @return 申请单，不存在返回 null
     */
    StudentModifyRequest findById(Long requestId);

    /**
     * 更新一条申请单（审核结果回写）。
     *
     * @param request 申请单（主键必填）
     * @return 是否成功
     */
    boolean update(StudentModifyRequest request);

    /**
     * 按条件分页查询申请单。
     *
     * <p>
     * 结果按提交时间倒序（最新的在前），便于教务先看到新提交的申请。
     *
     * @param query 过滤条件（null 表示全部状态）
     * @param offset 起始下标（从 0 开始）
     * @param limit 最多返回条数
     * @return 申请单列表（无匹配返回空列表，不返回 null）
     */
    List<StudentModifyRequest> find(ModifyRequestQuery query, int offset, int limit);

    /**
     * 统计满足条件的申请单数。
     *
     * @param query 过滤条件（null 表示全部状态）
     * @return 申请单总数
     */
    long count(ModifyRequestQuery query);
}
