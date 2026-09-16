package edu.seu.vcampus.common.library.dto;

import java.io.Serializable;

/** 借阅请求载荷。 */
public final class BorrowRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String isbn;

    /**
     * 创建借阅请求。
     * @param isbn 要借阅的 ISBN
     */
    public BorrowRequest(String isbn) {
        this.isbn = isbn;
    }

    /** @return 要借阅的 ISBN */
    public String getIsbn() {
        return isbn;
    }
}
