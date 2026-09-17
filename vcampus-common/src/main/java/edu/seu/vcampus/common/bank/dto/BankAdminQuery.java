package edu.seu.vcampus.common.bank.dto;

import edu.seu.vcampus.common.message.PageResponse;

import java.io.Serializable;

/**
 * 银行账户管理端的分页查询条件。
 *
 * <p>关键词匹配登录名或姓名，为空表示不过滤。分页参数默认1/20、单页上限100，
 * 归一化委托 {@link PageResponse}（见 ADR-0009 D4）。</p>
 */
public final class BankAdminQuery implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 关键词：匹配登录名或姓名；null 表示不过滤。 */
    private final String keyword;

    /** 页码。 */
    private final int pageNumber;

    /** 每页记录数。 */
    private final int pageSize;

    /** 构造查询全部账户（默认分页）的条件。 */
    public BankAdminQuery() {
        this(null, PageResponse.DEFAULT_PAGE_NUMBER, PageResponse.DEFAULT_PAGE_SIZE);
    }

    /**
     * 构造查询条件。
     *
     * @param keyword 关键词；null 表示不过滤
     * @param pageNumber 页码
     * @param pageSize 每页记录数
     */
    public BankAdminQuery(String keyword, int pageNumber, int pageSize) {
        this.keyword = keyword;
        this.pageNumber = PageResponse.normalizePageNumber(pageNumber);
        this.pageSize = PageResponse.normalizePageSize(pageSize);
    }

    /** @return 关键词；可能为 null */
    public String getKeyword() {
        return keyword;
    }

    /** @return 页码 */
    public int getPageNumber() {
        return pageNumber;
    }

    /** @return 每页记录数 */
    public int getPageSize() {
        return pageSize;
    }
}
