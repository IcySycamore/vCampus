package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.LibraryAccount;
import edu.seu.vcampus.common.library.entity.PopularBorrow;
import edu.seu.vcampus.common.message.PageResponse;
import java.sql.SQLException;
import java.util.List;

/** 图书检索、借还、续借、预约及罚款服务入口。 */
public class LibraryService {
    /** 完整服务的进程级单例。 */
    private static LibraryService s_instance;
    private final LibraryCatalogService m_catalog;
    private final LibraryCirculationService m_circulation;
    private final LibraryReservationService m_reservations;
    private final LibraryFineService m_fines;
    private final LibraryAccountService m_accounts;

    /**
     * 获取完整图书馆服务的进程级单例。
     * 
     * @param dataSource     连接来源
     * @param accountDao     图书馆账户数据访问接口
     * @param bookDao        图书数据访问接口
     * @param borrowDao      借阅记录数据访问接口
     * @param reservationDao 预约数据访问接口
     * @return 首次调用创建的图书馆服务
     */
    public static synchronized LibraryService getInstance(LibraryConnectionSource dataSource,
            LibraryAccountDao accountDao, BookDao bookDao, BorrowDao borrowDao,
            ReservationDao reservationDao) {
        LibraryValues.requireDependencies("library", dataSource, accountDao,
                bookDao, borrowDao, reservationDao);
        if (s_instance == null) {
            s_instance = new LibraryService(dataSource, accountDao, bookDao,
                    borrowDao, reservationDao);
        }
        return s_instance;
    }

    LibraryService(LibraryConnectionSource dataSource, LibraryAccountDao accountDao,
            BookDao bookDao, BorrowDao borrowDao, ReservationDao reservationDao) {
        LibraryValues.requireDependencies("library", dataSource, accountDao,
                bookDao, borrowDao, reservationDao);
        m_catalog = new LibraryCatalogService(dataSource, bookDao);
        m_accounts = new LibraryAccountService(accountDao);
        m_reservations = new LibraryReservationService(dataSource, bookDao,
                borrowDao, reservationDao, m_accounts);
        m_circulation = new LibraryCirculationService(dataSource, bookDao,
                borrowDao, m_reservations, m_accounts);
        m_fines = new LibraryFineService(dataSource, borrowDao);
    }

    /** @return 馆藏管理服务 */
    public LibraryCatalogService getCatalog() {
        return m_catalog;
    }

    /**
     * 检索未下架馆藏。
     * 
     * @param query 分页查询
     * @return 馆藏分页
     * @throws SQLException 数据访问失败
     */
    public PageResponse<Book> search(BookQuery query) throws SQLException {
        return m_catalog.search(query);
    }

    /**
     * 查询用户全部借阅记录。
     * 
     * @param userId 用户 UUID
     * @return 借阅记录
     * @throws SQLException 数据访问失败
     */
    public List<BorrowRecord> listBorrows(String userId) throws SQLException {
        return m_circulation.listBorrows(userId);
    }

    /**
     * 查询累计借阅次数最高的图书。
     * 
     * @param limit 最大返回数量
     * @return 热门借阅排行
     * @throws SQLException 数据访问失败
     */
    public List<PopularBorrow> listPopular(int limit) throws SQLException {
        return m_circulation.listPopular(limit);
    }

    /**
     * 查询当前用户的图书馆读者账户。
     * 
     * @param userId 用户 UUID
     * @return 读者账户
     * @throws SQLException     数据访问失败
     * @throws LibraryException 账户不存在
     */
    public LibraryAccount queryAccount(String userId)
            throws SQLException, LibraryException {
        return m_accounts.query(userId);
    }

    /**
     * 借出图书。
     * 
     * @param userId 用户 UUID
     * @param isbn   ISBN
     * @return 新借阅记录
     * @throws SQLException     数据访问失败
     * @throws LibraryException 业务规则拒绝
     */
    public BorrowRecord borrow(String userId, String isbn)
            throws SQLException, LibraryException {
        return m_circulation.borrow(userId, isbn);
    }

    /**
     * 归还图书并固化滞纳金。
     * 
     * @param userId   用户 UUID
     * @param recordId 借阅记录号
     * @return 更新后的记录
     * @throws SQLException     数据访问失败
     * @throws LibraryException 业务规则拒绝
     */
    public BorrowRecord returnBook(String userId, long recordId)
            throws SQLException, LibraryException {
        return m_circulation.returnBook(userId, recordId);
    }

    /**
     * 从原到期日起续借 30 天。
     * 
     * @param userId   用户 UUID
     * @param recordId 借阅记录号
     * @return 更新后的记录
     * @throws SQLException     数据访问失败
     * @throws LibraryException 业务规则拒绝
     */
    public BorrowRecord renew(String userId, long recordId)
            throws SQLException, LibraryException {
        return m_circulation.renew(userId, recordId);
    }

    /**
     * 为无库存图书提交预约。
     * 
     * @param userId 用户 UUID
     * @param isbn   ISBN
     * @return 新预约
     * @throws SQLException     数据访问失败
     * @throws LibraryException 业务规则拒绝
     */
    public BookReservation reserve(String userId, String isbn)
            throws SQLException, LibraryException {
        return m_reservations.reserve(userId, isbn);
    }

    /**
     * 查询我的预约并刷新过期状态。
     * 
     * @param userId 用户 UUID
     * @return 预约记录
     * @throws SQLException     数据访问失败
     * @throws LibraryException 业务规则拒绝
     */
    public List<BookReservation> listReservations(String userId)
            throws SQLException, LibraryException {
        return m_reservations.list(userId);
    }

    /**
     * 取消预约。
     * 
     * @param userId        用户 UUID
     * @param reservationId 预约号
     * @return 更新后的预约
     * @throws SQLException     数据访问失败
     * @throws LibraryException 业务规则拒绝
     */
    public BookReservation cancelReservation(String userId, long reservationId)
            throws SQLException, LibraryException {
        return m_reservations.cancel(userId, reservationId);
    }

    /**
     * 缴纳借阅记录的滞纳金。
     * 
     * @param userId   用户 UUID
     * @param recordId 借阅记录号
     * @param payment  校园银行扣款接口
     * @return 已结清记录
     * @throws SQLException     数据访问失败
     * @throws LibraryException 业务规则或扣款失败
     */
    public BorrowRecord payFine(String userId, long recordId,
            LibraryFinePayment payment) throws SQLException, LibraryException {
        return m_fines.pay(userId, recordId, payment);
    }

    /** @return 用户账户生命周期使用的图书馆开户钩子 */
    public LibraryAccountProvisioner getAccountProvisioner() {
        return m_accounts.provisioner();
    }
}
