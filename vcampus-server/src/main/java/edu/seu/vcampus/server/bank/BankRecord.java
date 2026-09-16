package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.bank.entity.BankAccount;
import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.bank.entity.BankTransactionType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 账户状态与锁对象；方法由 BankService 在该记录锁内调用。 */
final class BankRecord {
    final BankAccount account;
    BankCredential credential;
    final List<BankTransaction> transactions = new ArrayList<BankTransaction>();
    BankRecord(BankAccount account, BankCredential credential) {
        this.account = account;
        this.credential = credential;
    }
    BankTransaction verifyPayment(char[] password, BigDecimal amount, String order) {
        if (credential == null) {
            throw new IllegalStateException("账户尚未设置银行密码");
        }
        credential.verify(password);
        for (BankTransaction item : transactions) {
            if (item.getType() == BankTransactionType.CONSUMPTION
                    && order.equals(item.getRelatedOrderId())) {
                if (amount.compareTo(item.getAmount()) != 0) {
                    throw new IllegalArgumentException("订单金额与已支付记录不一致");
                }
                return item;
            }
        }
        return null;
    }
    BankTransactionListResponse page(BankTransactionQueryRequest request) {
        BankTransactionQueryRequest query = request == null
                ? new BankTransactionQueryRequest() : request;
        List<BankTransaction> filtered = new ArrayList<BankTransaction>();
        for (BankTransaction transaction : transactions) {
            if (query.getType() == null || query.getType() == transaction.getType()) {
                filtered.add(transaction);
            }
        }
        long total = filtered.size();
        long offset = ((long) query.getPageNumber() - 1L) * query.getPageSize();
        List<BankTransaction> page = new ArrayList<BankTransaction>();
        if (offset < total) {
            long end = Math.min(total, offset + query.getPageSize());
            for (long index = offset; index < end; index++) {
                page.add(filtered.get((int) index));
            }
        }
        return new BankTransactionListResponse(page, query.getPageNumber(),
                query.getPageSize(), total);
    }
}
