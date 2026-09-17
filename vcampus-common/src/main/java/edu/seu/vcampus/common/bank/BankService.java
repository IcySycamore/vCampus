package edu.seu.vcampus.common.bank;

import edu.seu.vcampus.common.bank.entity.BankAccount;
import java.math.BigDecimal;

/**
 * 银行账户服务接口，供其他模块（如shop）通过用户UUID操作银行账户。
 *
 * <p>本接口提供账户查询、余额操作等核心功能，所有方法均通过用户UUID定位账户。</p>
 */
public interface BankService {

    /**
     * 根据用户UUID查询银行账户信息。
     *
     * @param userUuid 用户全局唯一标识
     * @return 对应的银行账户；若不存在则返回 null
     */
    BankAccount getAccountByUserUuid(String userUuid);

    /**
     * 根据用户UUID查询账户余额。
     *
     * @param userUuid 用户全局唯一标识
     * @return 账户余额；若账户不存在则返回 null
     */
    BigDecimal getBalance(String userUuid);

    /**
     * 从指定用户账户扣款（用于支付）。
     *
     * <p>此方法用于shop等模块在用户购买商品时扣除账户余额。</p>
     *
     * @param userUuid 用户全局唯一标识
     * @param amount 扣款金额，必须大于零
     * @return 扣款成功返回 true；账户不存在、余额不足、账户状态异常或金额无效时返回 false
     */
    boolean deduct(String userUuid, BigDecimal amount);

    /**
     * 向指定用户账户充值。
     *
     * @param userUuid 用户全局唯一标识
     * @param amount 充值金额，必须大于零
     * @return 充值成功返回 true；账户不存在、账户状态异常或金额无效时返回 false
     */
    boolean deposit(String userUuid, BigDecimal amount);

    /**
     * 检查指定用户账户余额是否充足。
     *
     * @param userUuid 用户全局唯一标识
     * @param amount 需要检查的金额
     * @return 余额充足返回 true；账户不存在或余额不足返回 false
     */
    boolean hasSufficientBalance(String userUuid, BigDecimal amount);

    /**
     * 检查指定用户的账户是否处于可操作状态（正常状态）。
     *
     * @param userUuid 用户全局唯一标识
     * @return 账户正常返回 true；账户不存在或状态异常返回 false
     */
    boolean isAccountOperational(String userUuid);
}
