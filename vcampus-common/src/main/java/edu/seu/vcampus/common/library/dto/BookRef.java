package edu.seu.vcampus.common.library.dto;

import java.io.Serializable;

/** 通过 ISBN 引用一本馆藏图书。 */
public final class BookRef implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String isbn;

    /**
     * 创建馆藏图书引用。
     * @param isbn 馆藏 ISBN
     */
    public BookRef(String isbn) {
        this.isbn = isbn;
    }

    /** @return 馆藏 ISBN */
    public String getIsbn() {
        return isbn;
    }
}
