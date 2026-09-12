package edu.seu.vcampus.common.user.dto;

import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.entity.Role;

import java.io.Serializable;

/**
 * 用户分页查询条件（命令 106）。
 *
 * <p>
 * 各字段可为空表示不过滤；分页参数默认 1/20、上限 100（见 ADR-0009 D4）。
 */
public final class UserQuery implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 关键词：匹配登录名或姓名。 */
    private final String m_keyword;

    /** 角色过滤；null 表示全部角色。 */
    private final Role m_role;

    /** 启用状态过滤；null 表示不限。 */
    private final Boolean m_enabled;

    /** 页码。 */
    private final int m_page_number;

    /** 每页记录数。 */
    private final int m_page_size;

    /** 构造查询全部用户（默认分页）的条件。 */
    public UserQuery() {
        this(null, null, null, PageResponse.DEFAULT_PAGE_NUMBER, PageResponse.DEFAULT_PAGE_SIZE);
    }

    /**
     * 构造查询条件。
     *
     * @param keyword 关键词；null 不过滤
     * @param role 角色；null 不过滤
     * @param enabled 启用状态；null 不过滤
     * @param pageNumber 页码
     * @param pageSize 每页记录数
     */
    public UserQuery(String keyword, Role role, Boolean enabled, int pageNumber, int pageSize) {
        this.m_keyword = keyword;
        this.m_role = role;
        this.m_enabled = enabled;
        this.m_page_number = PageResponse.normalizePageNumber(pageNumber);
        this.m_page_size = PageResponse.normalizePageSize(pageSize);
    }

    /**
     * 按页码复制一份查询条件（供分页条使用）。
     *
     * @param pageNumber 新页码
     * @return 同条件、新页码的查询对象
     */
    public UserQuery withPageNumber(int pageNumber) {
        return new UserQuery(m_keyword, m_role, m_enabled, pageNumber, m_page_size);
    }

    /** @return 关键词；可能为 null */
    public String getKeyword() {
        return m_keyword;
    }

    /** @return 角色；可能为 null */
    public Role getRole() {
        return m_role;
    }

    /** @return 启用状态；可能为 null */
    public Boolean getEnabled() {
        return m_enabled;
    }

    /** @return 页码 */
    public int getPageNumber() {
        return m_page_number;
    }

    /** @return 每页记录数 */
    public int getPageSize() {
        return m_page_size;
    }
}
