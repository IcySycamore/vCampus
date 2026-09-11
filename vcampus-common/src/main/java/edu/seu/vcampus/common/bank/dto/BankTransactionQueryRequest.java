package edu.seu.vcampus.common.bank.dto;

import edu.seu.vcampus.common.bank.entity.BankTransactionType;

import java.io.Serializable;

/**
 * 银行资金流水分页查询请求。
 *
 * <p>流水归属账户由服务端根据已认证会话确定；类型为空表示查询全部类型。</p>
 */
public final class BankTransactionQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 默认页码。 */
    public static final int DEFAULT_PAGE_NUMBER = 1;

    /** 默认每页记录数。 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 单页允许的最大记录数。 */
    public static final int MAX_PAGE_SIZE = 100;

    private final int pageNumber;
    private final int pageSize;
    private final BankTransactionType type;

    /** 创建使用默认分页参数、查询全部类型的请求。 */
    public BankTransactionQueryRequest() {
        this(DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE, null);
    }

    /**
     * 创建查询全部流水类型的分页请求。
     *
     * @param pageNumber 页码，从 1 开始
     * @param pageSize 每页记录数，范围为 1 至 100
     */
    public BankTransactionQueryRequest(int pageNumber, int pageSize) {
        this(pageNumber, pageSize, null);
    }

    /**
     * 创建流水分页查询请求。
     *
     * @param pageNumber 页码，从 1 开始
     * @param pageSize 每页记录数，范围为 1 至 100
     * @param type 流水类型；为空表示全部类型
     */
    public BankTransactionQueryRequest(int pageNumber, int pageSize,
            BankTransactionType type) {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be at least one");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between one and 100");
        }
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.type = type;
    }

    /** @return 页码，从 1 开始 */
    public int getPageNumber() {
        return pageNumber;
    }

    /** @return 每页记录数 */
    public int getPageSize() {
        return pageSize;
    }

    /** @return 流水类型；为空表示全部类型 */
    public BankTransactionType getType() {
        return type;
    }
}
