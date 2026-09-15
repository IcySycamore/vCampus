package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.user.entity.Role;

/**
 * 账户开户钩子：账户建立/注销时，各模块用它同步维护自己的<b>1:1 档案表</b>。
 *
 * <p>
 * 背景（业务要求）：管理员创建一个账户后，该账号在各业务表里的档案应当<b>同时</b>建好，
 * 而不是等到用户第一次访问功能时才懒创建——否则「账户存在但查不到自己的学籍/账户」会成为一个 需要到处判空的常态。
 *
 * <p>
 * 但「建行」只对<b>一账号一行</b>的档案表成立（用户表、学籍表、银行账户表）；
 * 借阅记录、选课记录、订单这类<b>行为记录表</b>是「一账号多行」，注册时预建空行属于脏数据，
 * 应当在首次行为发生时写入，因此对应的模块<b>不实现</b>本接口。
 *
 * <p>
 * 落库映射（`sql/vCampus.sql` 扩充后）：
 *
 * <table border="1">
 * <caption>表与开户语义</caption>
 * <tr>
 * <th>表</th>
 * <th>性质</th>
 * <th>注册时是否建行</th>
 * </tr>
 * <tr>
 * <td>{@code tblUser}</td>
 * <td>账户本身</td>
 * <td>是（用户模块自己负责）</td>
 * </tr>
 * <tr>
 * <td>{@code tblStudentRecord}</td>
 * <td>1:1 学籍档案</td>
 * <td>是（在校人员：学生、教师；管理员不建）</td>
 * </tr>
 * <tr>
 * <td>{@code tblBankAccount}</td>
 * <td>1:1 银行账户</td>
 * <td>否：由用户显式开户（命令 604）</td>
 * </tr>
 * <tr>
 * <td>{@code tblBorrowRecord} / {@code tblCourseSelection} /
 * {@code tblShopOrder}</td>
 * <td>行为记录</td>
 * <td>否：首次借书/选课/下单时写入</td>
 * </tr>
 * </table>
 *
 * <p>
 * 实现约定：{@link #provision} 必须<b>幂等</b>（重复调用不产生第二条档案），
 * 失败时抛运行时异常——装配层会回滚已建档案并撤销账户，不允许留下「半个账户」。
 */
public interface AccountProvisioner {

    /**
     * 为新账户建立本模块的 1:1 档案。
     *
     * @param userUuid 账户全局唯一标识
     * @param displayName 姓名（{@code AuthService} 传入的即姓名；未采集时它已用登录名顶上）
     * @param role 角色；null 表示无法解析（实现方按「不建」处理）
     */
    void provision(String userUuid, String displayName, Role role);

    /**
     * 撤销账户时清理本模块的档案（软删除，保留历史引用）。
     *
     * @param userUuid 账户全局唯一标识
     */
    void revoke(String userUuid);
}
