package edu.seu.vcampus.common.bank.dto;

import edu.seu.vcampus.common.bank.entity.BankTransaction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 银行资金流水分页查询响应。
 */
public final class BankTransactionListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<BankTransaction> transactions;
    private final int pageNumber;
    private final int pageSize;
    private final long totalCount;

    /**
     * 创建流水分页响应。
     *
     * @param transactions 当前页流水，不能为空且不能包含空元素
     * @param pageNumber 当前页码，从 1 开始
     * @param pageSize 每页记录数，范围为 1 至 100
     * @param totalCount 满足条件的流水总数，不能为负
     */
    public BankTransactionListResponse(List<BankTransaction> transactions,
            int pageNumber, int pageSize, long totalCount) {
        if (transactions == null) {
            throw new IllegalArgumentException("transactions must not be null");
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be at least one");
        }
        if (pageSize < 1 || pageSize > BankTransactionQueryRequest.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between one and 100");
        }
        if (transactions.size() > pageSize) {
            throw new IllegalArgumentException("transactions must not exceed pageSize");
        }
        if (totalCount < transactions.size()) {
            throw new IllegalArgumentException("totalCount must cover current page");
        }
        this.transactions = immutableCopy(transactions);
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
    }

    /** @return 不可修改的当前页流水列表 */
    public List<BankTransaction> getTransactions() {
        return transactions;
    }

    /** @return 当前页码 */
    public int getPageNumber() {
        return pageNumber;
    }

    /** @return 每页记录数 */
    public int getPageSize() {
        return pageSize;
    }

    /** @return 满足条件的流水总数 */
    public long getTotalCount() {
        return totalCount;
    }

    /** @return 总页数；无记录时返回零 */
    public long getTotalPages() {
        if (totalCount == 0) {
            return 0;
        }
        return (totalCount - 1) / pageSize + 1;
    }

    private static List<BankTransaction> immutableCopy(
            List<BankTransaction> transactions) {
        List<BankTransaction> copy = new ArrayList<BankTransaction>(transactions.size());
        for (BankTransaction transaction : transactions) {
            if (transaction == null) {
                throw new IllegalArgumentException("transactions must not contain null");
            }
            copy.add(transaction);
        }
        return Collections.unmodifiableList(copy);
    }
}
