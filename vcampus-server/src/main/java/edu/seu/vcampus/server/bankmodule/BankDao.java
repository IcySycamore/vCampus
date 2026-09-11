package edu.seu.vcampus.server.bankmodule;

import edu.seu.vcampus.common.bank.entity.BankAccount;
import java.math.BigDecimal;

/**
 * 银行账户数据访问接口。
 *
 * <p>定义对银行账户表的基础 CRUD 操作。</p>
 */
public interface BankDao {

    /**
     * 根据用户UUID查询银行账户。
     *
     * @param userUuid 用户全局唯一标识
     * @return 对应的银行账户；不存在时返回 null
     */
    BankAccount findByUserUuid(String userUuid);

    /**
     * 更新账户余额。
     *
     * @param userUuid 用户全局唯一标识
     * @param newBalance 新余额
     * @return 更新成功返回 true；账户不存在时返回 false
     */
    boolean updateBalance(String userUuid, BigDecimal newBalance);

    /**
     * 创建新的银行账户。
     *
     * @param account 待创建的账户对象
     * @return 创建成功返回 true；失败返回 false
     */
    boolean insert(BankAccount account);

    /**
     * 更新账户信息。
     *
     * @param account 待更新的账户对象
     * @return 更新成功返回 true；失败返回 false
     */
    boolean update(BankAccount account);
}
