package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.message.Message;

/**
 * 为银行业务解析经过认证的用户身份。
 *
 * <p>实现应由服务器会话层提供：先校验 {@link Message#getToken()}，再返回
 * token 对应的稳定用户主键（User.userId）。禁止用登录名、sender、学号的数值转换
 * 或哈希值替代主键；无法取得主键时返回 null。当前主线会话只包含登录名，
 * 尚需认证集成方提供主键查询能力，银行模块不自行分配用户 ID。</p>
 */
public interface BankIdentityResolver {

    /**
     * 从请求中解析真实用户编号。
     *
     * @param request 已由会话层接收的银行请求
     * @return 已认证的正数用户主键；无法认证或取得主键时返回 null
     */
    Long resolveUserId(Message request);
}
