package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.entity.Book;

/** 管理员输入校验，忽略客户端传入的可借数量和下架状态。 */
final class LibraryCatalogValidator {
    private LibraryCatalogValidator() {
    }

    static Book book(Object data) throws LibraryException {
        if (!(data instanceof Book)) {
            throw new LibraryException(StatusCode.BAD_REQUEST, "图书资料必须是 Book 对象");
        }
        Book input = (Book) data;
        String isbn = LibraryRequestValidator.isbn(input.getIsbn());
        String title = text(input.getTitle(), "书名", 200);
        String author = text(input.getAuthor(), "作者", 100);
        String category = text(input.getCategory(), "分类", 100);
        if (input.getTotalCopies() < 0) {
            throw new LibraryException(StatusCode.BAD_REQUEST, "馆藏总数不能为负数");
        }
        return new Book(isbn, title, author, category,
                input.getTotalCopies(), input.getTotalCopies());
    }

    private static String text(String value, String name, int max) throws LibraryException {
        if (value == null || value.trim().length() == 0) {
            throw new LibraryException(StatusCode.BAD_REQUEST, name + "不能为空");
        }
        String result = value.trim();
        if (result.length() > max) {
            throw new LibraryException(StatusCode.BAD_REQUEST, name + "不能超过 " + max + " 个字符");
        }
        return result;
    }
}
