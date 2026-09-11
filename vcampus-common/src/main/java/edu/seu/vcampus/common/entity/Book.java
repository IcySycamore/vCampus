package edu.seu.vcampus.common.entity;

import java.io.Serializable;

/**
 * 图书馆藏信息。
 */
public class Book implements Serializable {

    private static final long serialVersionUID = 1L;
    private String isbn;
    private String title;
    private String author;
    private String category;
    private int totalCopies;
    private int availableCopies;

    /** 创建空图书对象，供对象流和数据访问层使用。 */
    public Book() {
    }

    /**
     * 创建图书。
     *
     * @param isbn ISBN
     * @param title 书名
     * @param author 作者
     * @param category 分类
     * @param totalCopies 馆藏数量
     * @param availableCopies 可借数量
     */
    public Book(String isbn, String title, String author, String category,
            int totalCopies, int availableCopies) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
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

    /** @return 作者 */
    public String getAuthor() {
        return author;
    }

    /** @param author 作者 */
    public void setAuthor(String author) {
        this.author = author;
    }

    /** @return 分类 */
    public String getCategory() {
        return category;
    }

    /** @param category 分类 */
    public void setCategory(String category) {
        this.category = category;
    }

    /** @return 馆藏数量 */
    public int getTotalCopies() {
        return totalCopies;
    }

    /** @param totalCopies 馆藏数量 */
    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    /** @return 可借数量 */
    public int getAvailableCopies() {
        return availableCopies;
    }

    /** @param availableCopies 可借数量 */
    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }
}
