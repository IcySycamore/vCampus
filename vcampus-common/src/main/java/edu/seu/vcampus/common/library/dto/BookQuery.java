package edu.seu.vcampus.common.library.dto;

import edu.seu.vcampus.common.message.PageResponse;
import java.io.Serializable;

/** 图书分页查询条件；普通检索与管理员馆藏检索共用。 */
public final class BookQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String keyword;
    private final String field;
    private final int pageNumber;
    private final int pageSize;

    /** 创建默认的全字段第一页查询。 */
    public BookQuery() {
        this(null, "all", PageResponse.DEFAULT_PAGE_NUMBER, PageResponse.DEFAULT_PAGE_SIZE);
    }

    /**
     * 创建查询条件。
     * @param keyword 关键词；null 表示空关键词
     * @param field all、title、author 或 category
     * @param pageNumber 页码，从 1 开始
     * @param pageSize 每页记录数，最大 100
     */
    public BookQuery(String keyword, String field, int pageNumber, int pageSize) {
        this.keyword = keyword;
        this.field = field;
        this.pageNumber = PageResponse.normalizePageNumber(pageNumber);
        this.pageSize = PageResponse.normalizePageSize(pageSize);
    }

    /** @return 关键词；可能为 null */
    public String getKeyword() {
        return keyword;
    }

    /** @return 检索字段 */
    public String getField() {
        return field;
    }

    /** @return 页码 */
    public int getPageNumber() {
        return pageNumber;
    }

    /** @return 每页记录数 */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 复制查询条件并切换页码。
     * @param number 新页码
     * @return 新查询条件
     */
    public BookQuery withPageNumber(int number) {
        return new BookQuery(keyword, field, number, pageSize);
    }
}
