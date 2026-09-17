package edu.seu.vcampus.client.library;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.NetworkConstant;
import edu.seu.vcampus.common.library.LibraryPolicy;
import edu.seu.vcampus.common.library.dto.BookRef;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.dto.BorrowRequest;
import edu.seu.vcampus.common.library.dto.FinePaymentRequest;
import edu.seu.vcampus.common.library.dto.RecordRef;
import edu.seu.vcampus.common.library.dto.ReservationRef;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.LibraryAccount;
import edu.seu.vcampus.common.library.entity.PopularBorrow;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import java.util.List;

/** 图书馆同步客户端 API；会话始终读取组长提供的 UserService 缓存。 */
public class LibraryService {
    private final UserService users;
    private final LibraryTransport transport;

    /**
     * 创建 API，复用同一登录连接的分发器及用户服务。
     * @param dispatcher 客户端分发器
     * @param users 已装配的用户 API
     */
    public LibraryService(ClientMessageDispatcher dispatcher, UserService users) {
        this(dispatcher, users, NetworkConstant.DEFAULT_REQUEST_TIMEOUT_MILLIS);
    }

    /**
     * 创建指定超时的 API。
     * @param dispatcher 客户端分发器
     * @param users 已装配的用户 API
     * @param timeoutMillis 请求超时毫秒数
     */
    public LibraryService(ClientMessageDispatcher dispatcher, UserService users,
            long timeoutMillis) {
        transport = new LibraryTransport(dispatcher, users, timeoutMillis);
        this.users = users;
    }

    /** @return 当前服务器会话；未登录为 null，不另行缓存 */
    public SessionEntry currentSession() {
        return users.currentSession();
    }

    /** @return 是否持有有效的登录缓存 */
    public boolean isLoggedIn() {
        return users.isLoggedIn();
    }

    /** @return 当前身份的借阅上限 */
    public int borrowLimit() {
        SessionEntry entry = currentSession();
        return LibraryPolicy.borrowLimit(entry == null ? null : entry.getRole());
    }

    /** @return 当前身份是否可维护馆藏 */
    public boolean canManageCatalog() {
        SessionEntry entry = currentSession();
        return isLoggedIn() && entry != null && LibraryPolicy.canManage(entry.getRole());
    }

    /**
     * 检索可见馆藏。
     * @param query 分页查询条件
     * @return 图书分页
     */
    public PageResponse<Book> searchBooks(BookQuery query) {
        return LibraryResponses.page(transport.call(Command.LIBRARY_SEARCH, query), Book.class);
    }

    /** @return 当前用户的借阅记录，身份由服务器从 token 解析 */
    public List<BorrowRecord> listMyBorrows() {
        return LibraryResponses.list(transport.call(
                Command.LIBRARY_LIST_BORROWS, null), BorrowRecord.class);
    }

    /** @return 服务器按累计借阅次数统计的热门图书 */
    public List<PopularBorrow> listPopularBorrows() {
        return LibraryResponses.list(transport.call(
                Command.LIBRARY_POPULAR_BORROWS, null), PopularBorrow.class);
    }

    /** @return 当前用户的图书馆读者账户 */
    public LibraryAccount queryMyAccount() {
        return LibraryResponses.value(transport.call(
                Command.LIBRARY_ACCOUNT_QUERY, null), LibraryAccount.class);
    }

    /**
     * 借书。
     * @param isbn ISBN
     * @return 借阅记录
     */
    public BorrowRecord borrowBook(String isbn) {
        return LibraryResponses.value(transport.call(Command.LIBRARY_BORROW,
                new BorrowRequest(isbn)), BorrowRecord.class);
    }

    /**
     * 归还本人的借阅。
     * @param recordId 记录号
     * @return 更新后的记录
     */
    public BorrowRecord returnBook(long recordId) {
        return LibraryResponses.value(transport.call(Command.LIBRARY_RETURN,
                new RecordRef(recordId)), BorrowRecord.class);
    }

    /**
     * 续借本人的未归还记录。
     * @param recordId 借阅记录号
     * @return 新到期日已更新的记录
     */
    public BorrowRecord renewBook(long recordId) {
        return LibraryResponses.value(transport.call(Command.LIBRARY_RENEW,
                new RecordRef(recordId)), BorrowRecord.class);
    }

    /**
     * 为暂无库存的图书提交预约。
     * @param isbn ISBN
     * @return 新预约
     */
    public BookReservation reserveBook(String isbn) {
        return LibraryResponses.value(transport.call(Command.LIBRARY_RESERVE,
                new BookRef(isbn)), BookReservation.class);
    }

    /** @return 当前用户全部预约记录 */
    public List<BookReservation> listMyReservations() {
        return LibraryResponses.list(transport.call(
                Command.LIBRARY_LIST_RESERVATIONS, null), BookReservation.class);
    }

    /**
     * 取消本人有效预约。
     * @param reservationId 预约号
     * @return 已取消预约
     */
    public BookReservation cancelReservation(long reservationId) {
        return LibraryResponses.value(transport.call(Command.LIBRARY_CANCEL_RESERVATION,
                new ReservationRef(reservationId)), BookReservation.class);
    }

    /**
     * 从校园银行账户缴纳逾期滞纳金。
     * @param recordId 借阅记录号
     * @param password 银行账户密码
     * @return 已结清记录
     */
    public BorrowRecord payFine(long recordId, char[] password) {
        return LibraryResponses.value(transport.call(Command.LIBRARY_PAY_FINE,
                new FinePaymentRequest(recordId, password)), BorrowRecord.class);
    }

    /**
     * 管理员查询全部馆藏，包含已下架图书。
     * @param query 分页查询条件
     * @return 图书分页
     */
    public PageResponse<Book> searchCatalog(BookQuery query) {
        return LibraryResponses.page(transport.call(
                Command.LIBRARY_CATALOG_SEARCH, query), Book.class);
    }

    /**
     * 录入馆藏。
     * @param book 图书资料
     * @return 新图书
     */
    public Book createBook(Book book) {
        return LibraryResponses.value(transport.call(
                Command.LIBRARY_CREATE_BOOK, book), Book.class);
    }

    /**
     * 修改馆藏。
     * @param book 图书资料
     * @return 更新后的图书
     */
    public Book updateBook(Book book) {
        return LibraryResponses.value(transport.call(
                Command.LIBRARY_UPDATE_BOOK, book), Book.class);
    }

    /**
     * 下架馆藏。
     * @param isbn ISBN
     * @return 下架后的图书
     */
    public Book withdrawBook(String isbn) {
        return LibraryResponses.value(transport.call(Command.LIBRARY_WITHDRAW_BOOK,
                new BookRef(isbn)), Book.class);
    }
}
