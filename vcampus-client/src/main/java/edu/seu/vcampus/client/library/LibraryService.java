package edu.seu.vcampus.client.library;

import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.NetworkConstant;
import edu.seu.vcampus.common.library.LibraryPolicy;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import java.util.ArrayList;
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
     * @param keyword 关键词
     * @param field 检索字段
     * @return 图书列表
     */
    public List<Book> searchBooks(String keyword, String field) {
        return list(transport.call(Command.LIBRARY_SEARCH,
                new String[] {keyword, field}), Book.class);
    }

    /** @return 当前用户的借阅记录，身份由服务器从 token 解析 */
    public List<BorrowRecord> listMyBorrows() {
        return list(transport.call(Command.LIBRARY_LIST_BORROWS, null), BorrowRecord.class);
    }

    /**
     * 借书。
     * @param isbn ISBN
     * @return 借阅记录
     */
    public BorrowRecord borrowBook(String isbn) {
        return value(transport.call(Command.LIBRARY_BORROW, isbn), BorrowRecord.class);
    }

    /**
     * 归还本人的借阅。
     * @param recordId 记录号
     * @return 更新后的记录
     */
    public BorrowRecord returnBook(long recordId) {
        return value(transport.call(Command.LIBRARY_RETURN, recordId), BorrowRecord.class);
    }

    /**
     * 管理员查询全部馆藏，包含已下架图书。
     * @param keyword 关键词
     * @return 图书列表
     */
    public List<Book> searchCatalog(String keyword) {
        return list(transport.call(Command.LIBRARY_CATALOG_SEARCH,
                new String[] {keyword, "all"}), Book.class);
    }

    /**
     * 录入馆藏。
     * @param book 图书资料
     * @return 新图书
     */
    public Book createBook(Book book) {
        return value(transport.call(Command.LIBRARY_CREATE_BOOK, book), Book.class);
    }

    /**
     * 修改馆藏。
     * @param book 图书资料
     * @return 更新后的图书
     */
    public Book updateBook(Book book) {
        return value(transport.call(Command.LIBRARY_UPDATE_BOOK, book), Book.class);
    }

    /**
     * 下架馆藏。
     * @param isbn ISBN
     * @return 下架后的图书
     */
    public Book withdrawBook(String isbn) {
        return value(transport.call(Command.LIBRARY_WITHDRAW_BOOK, isbn), Book.class);
    }

    private <T> T value(Object data, Class<T> type) {
        if (!type.isInstance(data)) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        return type.cast(data);
    }

    private <T> List<T> list(Object data, Class<T> type) {
        if (!(data instanceof List<?>)) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        List<T> result = new ArrayList<T>();
        for (Object item : (List<?>) data) {
            result.add(value(item, type));
        }
        return result;
    }
}
