package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.bank.exception.BankAccountNotOpenedException;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.server.bank.BankService;
import java.math.BigDecimal;

/** 使用现有校园银行消费能力支付图书逾期滞纳金。 */
public final class BankLibraryFinePayment implements LibraryFinePayment {
    private final BankService bank;

    /** @param bank 全服共享的银行服务 */
    public BankLibraryFinePayment(BankService bank) {
        if (bank == null) {
            throw new IllegalArgumentException("bank must not be null");
        }
        this.bank = bank;
    }

    @Override
    public String pay(String userId, BigDecimal amount, String reference)
            throws LibraryException {
        try {
            BankTransaction transaction = bank.consume(userId, amount,
                    reference, "图书逾期滞纳金");
            return transaction.getTransactionId();
        } catch (BankAccountNotOpenedException exception) {
            throw new LibraryException(StatusCode.BAD_REQUEST,
                    "银行账户未开户，请先开户并充值");
        } catch (IllegalArgumentException exception) {
            throw new LibraryException(StatusCode.BAD_REQUEST,
                    "银行账户余额不足，请先充值");
        } catch (IllegalStateException exception) {
            throw new LibraryException(StatusCode.FORBIDDEN, "银行账户当前不可用");
        }
    }
}
