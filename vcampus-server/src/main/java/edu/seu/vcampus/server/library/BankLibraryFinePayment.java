package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.bank.exception.BankAccountNotOpenedException;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.server.bank.BankService;
import java.math.BigDecimal;
import java.util.Arrays;

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
    public String pay(String userId, BigDecimal amount, String reference, char[] password)
            throws LibraryException {
        try {
            BankTransaction transaction = bank.consumeWithPassword(userId, password,
                    amount, reference, "图书逾期滞纳金");
            return transaction.getTransactionId();
        } catch (BankAccountNotOpenedException exception) {
            throw new LibraryException(StatusCode.BAD_REQUEST,
                    "银行账户未开户，请先开户并充值");
        } catch (IllegalArgumentException exception) {
            String message = exception.getMessage() == null ? "" : exception.getMessage();
            if (message.contains("余额不足") || message.contains("insufficient")) {
                throw new LibraryException(StatusCode.BAD_REQUEST,
                        "银行账户余额不足，请先充值");
            }
            if (message.contains("密码错误")) {
                throw new LibraryException(StatusCode.BAD_REQUEST,
                        "银行密码错误，请重新输入");
            }
            throw new LibraryException(StatusCode.BAD_REQUEST, message);
        } catch (IllegalStateException exception) {
            String message = exception.getMessage() == null ? "" : exception.getMessage();
            if (message.contains("尚未设置银行密码")) {
                throw new LibraryException(StatusCode.BAD_REQUEST,
                        "银行账户尚未设置密码，请先在银行模块开户");
            }
            if (message.contains("挂失")) {
                throw new LibraryException(StatusCode.FORBIDDEN, "银行账户已挂失，请先解挂");
            }
            if (message.contains("次数过多")) {
                throw new LibraryException(StatusCode.FORBIDDEN,
                        "银行密码错误次数过多，请一分钟后重试");
            }
            throw new LibraryException(StatusCode.FORBIDDEN, "银行账户当前不可用");
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }
}
