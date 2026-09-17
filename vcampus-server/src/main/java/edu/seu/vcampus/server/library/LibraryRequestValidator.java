package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.dto.BookRef;
import edu.seu.vcampus.common.library.dto.BorrowRequest;
import edu.seu.vcampus.common.library.dto.FinePaymentRequest;
import edu.seu.vcampus.common.library.dto.RecordRef;
import edu.seu.vcampus.common.library.dto.ReservationRef;

/** 图书馆协议参数校验；在业务调用前拒绝错误类型、缺失值和非法格式。 */
final class LibraryRequestValidator {
    private LibraryRequestValidator() {
    }

    static BookQuery search(Object data) throws LibraryException {
        if (!(data instanceof BookQuery)) {
            throw badRequest("搜索参数必须是 BookQuery");
        }
        BookQuery query = (BookQuery) data;
        String keyword = query.getKeyword() == null ? "" : query.getKeyword().trim();
        if (keyword.length() > 200) {
            throw badRequest("搜索关键词不能超过 200 个字符");
        }
        String field = query.getField() == null ? "all" : query.getField().trim();
        if (field.length() == 0) {
            field = "all";
        }
        if (!"all".equals(field) && !"title".equals(field)
                && !"author".equals(field) && !"isbn".equals(field)) {
            throw badRequest("检索范围仅支持 all（全部）、title（书名）、author（作者）、isbn（ISBN）");
        }
        if (query.getPageNumber() < 1 || query.getPageSize() < 1 || query.getPageSize() > 100) {
            throw badRequest("分页参数必须为正数且每页不能超过 100 条");
        }
        return new BookQuery(keyword, field, query.getPageNumber(), query.getPageSize());
    }

    static BorrowRequest borrow(Object data) throws LibraryException {
        if (!(data instanceof BorrowRequest)) {
            throw badRequest("借阅参数必须是 BorrowRequest");
        }
        BorrowRequest request = (BorrowRequest) data;
        return new BorrowRequest(isbn(request.getIsbn()));
    }

    static BookRef book(Object data) throws LibraryException {
        if (!(data instanceof BookRef)) {
            throw badRequest("图书引用必须是 BookRef");
        }
        BookRef reference = (BookRef) data;
        return new BookRef(isbn(reference.getIsbn()));
    }

    static String isbn(String value) throws LibraryException {
        if (value == null) {
            throw badRequest("ISBN 不能为空，请选择有效图书");
        }
        String isbn = value.trim();
        if (isbn.length() == 0) {
            throw badRequest("ISBN 不能为空，请选择要借阅的图书");
        }
        String compact = isbn.replace("-", "");
        if (isbn.length() > 25 || !isbn.matches("[0-9Xx]+(-[0-9Xx]+)*")
                || !compact.matches("[0-9]{9}[0-9Xx]|(978|979)[0-9]{10}")) {
            throw badRequest("ISBN 格式不正确：应为 10 位或以 978/979 开头的 13 位，可含连字符");
        }
        // 保留连字符与字母大小写，避免改变现有馆藏的字符串主键；不校验 ISBN 校验位。
        return isbn;
    }

    static RecordRef record(Object data) throws LibraryException {
        if (!(data instanceof RecordRef)) {
            throw badRequest("归还参数必须是 RecordRef");
        }
        long id = ((RecordRef) data).getRecordId();
        if (id <= 0) {
            throw badRequest("借阅记录号必须大于 0");
        }
        return new RecordRef(id);
    }

    static ReservationRef reservation(Object data) throws LibraryException {
        if (!(data instanceof ReservationRef)) {
            throw badRequest("预约参数必须是 ReservationRef");
        }
        long id = ((ReservationRef) data).getReservationId();
        if (id <= 0) {
            throw badRequest("预约记录号必须大于 0");
        }
        return new ReservationRef(id);
    }

    static FinePaymentRequest finePayment(Object data) throws LibraryException {
        if (!(data instanceof FinePaymentRequest)) {
            throw badRequest("缴纳滞纳金参数必须是 FinePaymentRequest");
        }
        FinePaymentRequest request = (FinePaymentRequest) data;
        if (request.getRecordId() <= 0) {
            throw badRequest("借阅记录号必须大于 0");
        }
        char[] password = request.getPassword();
        if (password == null || password.length == 0) {
            throw badRequest("银行密码不能为空");
        }
        return new FinePaymentRequest(request.getRecordId(), password);
    }

    private static LibraryException badRequest(String message) {
        return new LibraryException(StatusCode.BAD_REQUEST, message);
    }
}
