package edu.seu.vcampus.common.library.entity;

import java.io.Serializable;

/** 图书累计借阅次数排行项。 */
public class PopularBorrow implements Serializable {
    private static final long serialVersionUID = 1L;
    private String isbn;
    private String title;
    private int borrowCount;

    /** 创建空排行项，供对象流使用。 */
    public PopularBorrow() {
    }

    /**
     * 创建排行项。
     * @param isbn ISBN
     * @param title 书名快照
     * @param borrowCount 累计借阅次数
     */
    public PopularBorrow(String isbn, String title, int borrowCount) {
        this.isbn = isbn;
        this.title = title;
        this.borrowCount = borrowCount;
    }

    /** @return ISBN */
    public String getIsbn() {
        return isbn;
    }

    /** @param isbn ISBN */
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    /** @return 书名 */
    public String getTitle() {
        return title;
    }

    /** @param title 书名 */
    public void setTitle(String title) {
        this.title = title;
    }

    /** @return 累计借阅次数 */
    public int getBorrowCount() {
        return borrowCount;
    }

    /** @param borrowCount 累计借阅次数 */
    public void setBorrowCount(int borrowCount) {
        this.borrowCount = borrowCount;
    }
}
