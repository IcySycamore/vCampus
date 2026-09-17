package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.LibraryAccount;
import edu.seu.vcampus.common.library.entity.LibraryAccountStatus;
import edu.seu.vcampus.common.library.entity.PopularBorrow;
import edu.seu.vcampus.common.library.entity.ReservationStatus;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.server.db.DatabaseAvailability;
import edu.seu.vcampus.server.db.DbHelper;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 图书馆四个 JDBC 数据访问实现的真库集成测试。
 *
 * <p>
 * 覆盖 {@link BookDaoJdbc}、{@link BorrowDaoJdbc}、{@link LibraryAccountDaoJdbc}、
 * {@link ReservationDaoJdbc}：这四份实现就是生产路径（由 {@code LibraryModule} 装配），因此必须直连 MySQL
 * 校验落库语义（库存边界、原子归还、预约状态流转）。
 *
 * <p>
 * <b>环境门控</b>（见 ADR-0005）：连不上 MySQL 时整体跳过；前置是库中已有 {@code sql/vCampus.sql} 建出的表。
 *
 * <p>
 * 测试数据全部带时间戳后缀，跑完按本人 uuid / isbn 前缀物理删除，不与演示数据互相影响。
 */
class LibraryDaoJdbcTest {

    /** 测试 uuid 前缀；{@code uUuid} 列是 {@code CHAR(36)}，标识必须压进列宽。 */
    private static final String UUID_PREFIX = "jdblib-it-";

    /** 测试 ISBN 前缀；{@code bIsbn} 列是 {@code VARCHAR(20)}。 */
    private static final String ISBN_PREFIX = "JDLIT" + (System.nanoTime() % 1000000000L) + "-";

    /** 本轮测试用户 uuid。 */
    private String m_userUuid;

    /** 本轮 ISBN 前缀。 */
    private String m_isbnPrefix;

    /** 被测 DAO。 */
    private BookDaoJdbc m_books;

    /** 被测 DAO。 */
    private BorrowDaoJdbc m_borrows;

    /** 被测 DAO。 */
    private LibraryAccountDaoJdbc m_accounts;

    /** 被测 DAO。 */
    private ReservationDaoJdbc m_reservations;

    /** 每个用例前确认数据库可用并生成唯一标识。 */
    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(databaseAvailable(),
                "MySQL 不可用，跳过 JDBC 集成测试（docker compose up -d mysql 后自动执行）");
        m_userUuid = uuid("t", 0L);
        m_isbnPrefix = ISBN_PREFIX;
        m_books = new BookDaoJdbc();
        m_borrows = new BorrowDaoJdbc();
        m_accounts = new LibraryAccountDaoJdbc();
        m_reservations = new ReservationDaoJdbc();
    }

    /** 用例后物理删除本轮数据（软删除的账户要直接清掉）。 */
    @AfterEach
    void tearDown() {
        executeUpdate("DELETE FROM tblBorrow WHERE uId LIKE ?", UUID_PREFIX + "%");
        executeUpdate("DELETE FROM tblReservation WHERE uUuid LIKE ?", UUID_PREFIX + "%");
        executeUpdate("DELETE FROM tblLibraryAccount WHERE uUuid LIKE ?", UUID_PREFIX + "%");
        executeUpdate("DELETE FROM tblBook WHERE bIsbn LIKE ?", ISBN_PREFIX + "%");
    }

    @Test
    void insertBookIsFoundByIsbnAndBySearch() throws Exception {
        String isbn = m_isbn(1);
        Connection connection = DbHelper.getConnection();
        assertTrue(m_books.insertBook(connection, book(isbn, "数据库系统概念", 3)),
                "新书应插入成功");
        assertFalse(m_books.insertBook(connection, book(isbn, "重复 ISBN", 1)),
                "ISBN 重复应返回 false 而不是抛异常");

        Book found = m_books.findByIsbn(null, isbn);
        assertNotNull(found, "按 ISBN 应能查到");
        assertEquals("数据库系统概念", found.getTitle());
        assertEquals(3, found.getTotalCopies());
        assertEquals(3, found.getAvailableCopies());
        assertFalse(found.isWithdrawn());

        PageResponse<Book> page = m_books.search(new BookQuery(isbn, "isbn", 1, 20));
        assertEquals(1, page.getTotal(), "普通检索应命中这本书");
        assertEquals(isbn, page.getItems().get(0).getIsbn());
    }

    @Test
    void adjustAvailableStaysWithinZeroAndTotal() throws Exception {
        String isbn = m_isbn(2);
        Connection connection = DbHelper.getConnection();
        m_books.insertBook(connection, book(isbn, "算法导论", 1));

        assertTrue(m_books.adjustAvailable(null, isbn, -1), "有库存时借出应成功");
        assertEquals(0, m_books.findByIsbn(null, isbn).getAvailableCopies());
        assertFalse(m_books.adjustAvailable(null, isbn, -1), "库存为零时借出应失败");
        assertEquals(0, m_books.findByIsbn(null, isbn).getAvailableCopies(), "失败不得改库存");

        assertTrue(m_books.adjustAvailable(null, isbn, 1), "归还应成功");
        assertFalse(m_books.adjustAvailable(null, isbn, 1), "归还超过馆藏总数应失败");
        assertFalse(m_books.adjustAvailable(null, m_isbn(99), 1), "不存在的图书应失败");
    }

    @Test
    void withdrawnBookLeavesSearchButStillAcceptsReturns() throws Exception {
        String isbn = m_isbn(3);
        Connection connection = DbHelper.getConnection();
        m_books.insertBook(connection, book(isbn, "编译原理", 2));

        assertTrue(m_books.adjustAvailable(null, isbn, -1), "先借出一本，归还才有空间");
        assertTrue(m_books.withdrawBook(null, isbn), "下架应成功");
        assertTrue(m_books.findByIsbn(null, isbn).isWithdrawn(), "下架标记应落库");
        assertEquals(0, m_books.search(new BookQuery(isbn, "isbn", 1, 20)).getTotal(),
                "普通检索不应返回下架图书");
        assertEquals(1, m_books.searchCatalog(new BookQuery(isbn, "isbn", 1, 20)).getTotal(),
                "馆藏检索应包含下架图书");
        assertFalse(m_books.adjustAvailable(null, isbn, -1), "下架后不得借出");
        assertTrue(m_books.adjustAvailable(null, isbn, 1), "下架后仍允许归还");
    }

    @Test
    void updateBookRewritesFieldsButKeepsWithdrawnState() throws Exception {
        String isbn = m_isbn(4);
        Connection connection = DbHelper.getConnection();
        m_books.insertBook(connection, book(isbn, "计算机网络", 2));
        m_books.withdrawBook(null, isbn);

        Book edited = m_books.findByIsbn(null, isbn);
        edited.setTitle("计算机网络（第 7 版）");
        edited.setAuthor("谢希仁");
        edited.setCategory("计算机");
        assertTrue(m_books.updateBook(null, edited), "更新应成功");

        Book reloaded = m_books.findByIsbn(null, isbn);
        assertEquals("计算机网络（第 7 版）", reloaded.getTitle());
        assertEquals("谢希仁", reloaded.getAuthor());
        assertTrue(reloaded.isWithdrawn(), "更新资料不得清掉下架状态");
    }

    @Test
    void borrowInsertThenReturnIsAtomicAndRepeatSafe() throws Exception {
        String isbn = m_isbn(5);
        Connection connection = DbHelper.getConnection();
        m_books.insertBook(connection, book(isbn, "深入理解计算机系统", 1));

        BorrowRecord record = new BorrowRecord(m_userUuid, isbn, "深入理解计算机系统",
                new Date(), new Date(System.currentTimeMillis() + 30L * 24 * 3600 * 1000));
        long id = m_borrows.insert(null, record);
        assertTrue(id > 0L, "借阅记录号应由数据库生成");
        assertTrue(m_borrows.hasActive(null, m_userUuid, isbn), "应存在未归还记录");

        assertNotNull(m_borrows.findActiveById(null, id), "未归还记录应能按号查到");
        assertTrue(m_borrows.markReturned(null, id, new Timestamp(System.currentTimeMillis()),
                BigDecimal.ZERO.setScale(2), true), "首次归还应成功");
        assertFalse(m_borrows.markReturned(null, id, new Timestamp(System.currentTimeMillis()),
                BigDecimal.ZERO.setScale(2), true), "重复归还应失败");

        assertNull(m_borrows.findActiveById(null, id), "归还后不再是未归还记录");
        assertNotNull(m_borrows.findById(null, id), "已归还记录仍可按号查到");
        assertNotNull(m_borrows.findById(null, id).getReturnedAt(), "归还时间应落库");
        assertFalse(m_borrows.hasActive(null, m_userUuid, isbn), "归还后不应再有未归还记录");
    }

    @Test
    void renewAndFinePaymentHappenOnce() throws Exception {
        String isbn = m_isbn(6);
        Connection connection = DbHelper.getConnection();
        m_books.insertBook(connection, book(isbn, "操作系统概念", 1));

        BorrowRecord record = new BorrowRecord(m_userUuid, isbn, "操作系统概念",
                new Date(), new Date());
        long id = m_borrows.insert(null, record);

        Timestamp renewed = new Timestamp(System.currentTimeMillis() + 60L * 24 * 3600 * 1000);
        assertTrue(m_borrows.renew(null, id, renewed, 1), "首次续借应成功");
        BorrowRecord afterRenew = m_borrows.findById(null, id);
        assertEquals(1, afterRenew.getRenewalCount(), "续借次数应落库");
        assertNotNull(afterRenew.getDueAt());

        assertTrue(m_borrows.markReturned(null, id, new Timestamp(System.currentTimeMillis()),
                BigDecimal.valueOf(5).setScale(2), false), "带滞纳金归还应成功");
        assertFalse(m_borrows.findById(null, id).isFinePaid(), "未缴时标记应为未缴");

        String transactionId = "TX" + Long.toHexString(System.nanoTime());
        assertTrue(m_borrows.markFinePaid(null, id, transactionId), "首次缴费应成功");
        assertFalse(m_borrows.markFinePaid(null, id, "TX-AGAIN"), "重复缴费应失败");
        assertTrue(m_borrows.findById(null, id).isFinePaid(), "缴费状态应落库");
        assertEquals(transactionId, m_borrows.findById(null, id).getFineTransactionId());
    }

    @Test
    void popularBorrowRanksByRecordCount() throws Exception {
        String isbn = m_isbn(7);
        Connection connection = DbHelper.getConnection();
        m_books.insertBook(connection, book(isbn, "数据库系统概论", 3));
        for (int i = 0; i < 3; i++) {
            m_borrows.insert(null, new BorrowRecord(uuid("r", i), isbn, "数据库系统概论",
                    new Date(System.currentTimeMillis() - i * 1000L), new Date()));
        }

        List<PopularBorrow> ranked = m_borrows.findPopular(50);
        boolean found = false;
        for (PopularBorrow item : ranked) {
            if (isbn.equals(item.getIsbn())) {
                found = true;
                assertEquals(3, item.getBorrowCount(), "应累计三条借阅记录");
                assertEquals("数据库系统概论", item.getTitle());
            }
        }
        assertTrue(found, "热门排行里应包含刚造的借阅");
    }

    @Test
    void accountInsertUpdateAndSoftDelete() throws Exception {
        LibraryAccount account = new LibraryAccount(m_userUuid, 5, new Date());
        assertTrue(m_accounts.insert(account), "开户应成功");
        assertNotNull(account.getId(), "开户后应回填主键");

        LibraryAccount loaded = m_accounts.findByUserUuid(m_userUuid);
        assertNotNull(loaded, "按 uuid 应能查到账户");
        assertEquals(5, loaded.getBorrowLimit());
        assertEquals(LibraryAccountStatus.NORMAL, loaded.getStatus());
        assertFalse(loaded.isDeleted());

        loaded.setBorrowLimit(3);
        loaded.setStatus(LibraryAccountStatus.SUSPENDED);
        loaded.setUpdatedAt(new Date());
        assertTrue(m_accounts.update(loaded), "更新账户应成功");
        LibraryAccount updated = m_accounts.findByUserUuid(m_userUuid);
        assertEquals(3, updated.getBorrowLimit());
        assertEquals(LibraryAccountStatus.SUSPENDED, updated.getStatus());

        assertTrue(m_accounts.softDelete(m_userUuid, new Date()), "软删除应成功");
        LibraryAccount deleted = m_accounts.findByUserUuid(m_userUuid);
        assertNotNull(deleted, "接口约定软删除后仍可查到该行");
        assertTrue(deleted.isDeleted(), "应带上删除标记");
        assertTrue(rowExists("tblLibraryAccount", "uUuid", m_userUuid), "软删除不得物理删行");
    }

    @Test
    void reservationFlowsFromWaitingToReady() throws Exception {
        String isbn = m_isbn(8);
        Connection connection = DbHelper.getConnection();
        m_books.insertBook(connection, book(isbn, "数据库原理", 1));

        long id = m_reservations.insert(null, new BookReservation(m_userUuid, isbn, "数据库原理",
                new Date()));
        assertTrue(id > 0L, "预约号应由数据库生成");
        assertTrue(m_reservations.hasActive(null, m_userUuid, isbn), "新建预约即有效");
        assertTrue(m_reservations.hasActiveForBook(null, isbn), "该书应标记为有人预约");
        assertNull(m_reservations.findReady(null, m_userUuid, isbn), "刚提交时未到馆");

        BookReservation waiting = m_reservations.findFirstWaiting(null, isbn);
        assertNotNull(waiting, "应能取到最早等待记录");
        assertEquals(id, waiting.getId().longValue());
        assertEquals(ReservationStatus.WAITING, waiting.getStatus());

        Timestamp readyAt = new Timestamp(System.currentTimeMillis());
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() - 1000L);
        assertTrue(m_reservations.updateStatus(null, id, ReservationStatus.READY, readyAt,
                expiresAt), "标记到馆应成功");

        BookReservation ready = m_reservations.findReady(null, m_userUuid, isbn);
        assertNotNull(ready, "到馆后应能按用户 + ISBN 查到");
        assertEquals(ReservationStatus.READY, ready.getStatus());

        List<BookReservation> expired = m_reservations.findExpiredReady(null, isbn,
                new Timestamp(System.currentTimeMillis()));
        assertEquals(1, expired.size(), "保留期已过应被扫出");
        assertEquals(id, expired.get(0).getId().longValue());

        List<BookReservation> mine = m_reservations.findByUser(m_userUuid);
        assertEquals(1, mine.size(), "用户预约列表应有一条");
        assertTrue(m_reservations.updateStatus(null, id, ReservationStatus.FULFILLED, null, null),
                "流转到已完成应成功");
        assertFalse(m_reservations.hasActive(null, m_userUuid, isbn), "完成后不再算有效预约");
        assertFalse(m_reservations.hasActiveForBook(null, isbn), "完成后该书不再有有效预约");
    }

    @Test
    void duplicateActiveReservationIsRefusedByDatabase() throws Exception {
        final String isbn = m_isbn(9);
        Connection connection = DbHelper.getConnection();
        m_books.insertBook(connection, book(isbn, "数据库实现", 1));
        m_reservations.insert(null, new BookReservation(m_userUuid, isbn, "数据库实现",
                new Date()));

        // 接口要求「同一用户同一 ISBN 仅一条有效预约」由数据库保证，而不只是靠业务层先查后插
        assertThrows(SQLException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                m_reservations.insert(null, new BookReservation(m_userUuid, isbn,
                        "数据库实现", new Date()));
            }
        });
        assertEquals(1, m_reservations.findByUser(m_userUuid).size(), "重复插入不得留下第二行");
    }

    @Test
    void duplicateActiveBorrowIsRefusedByDatabase() throws Exception {
        final String isbn = m_isbn(10);
        Connection connection = DbHelper.getConnection();
        m_books.insertBook(connection, book(isbn, "数据库系统", 1));
        m_borrows.insert(null, new BorrowRecord(m_userUuid, isbn, "数据库系统",
                new Date(), new Date()));

        assertThrows(SQLException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                m_borrows.insert(null, new BorrowRecord(m_userUuid, isbn, "数据库系统",
                        new Date(), new Date()));
            }
        });
        assertTrue(m_borrows.hasActive(null, m_userUuid, isbn), "原记录应仍在");
    }

    /**
     * 造一本测试用图书。
     *
     * @param isbn   ISBN
     * @param title  书名
     * @param copies 馆藏数量
     * @return 图书实体
     */
    private static Book book(String isbn, String title, int copies) {
        return new Book(isbn, title, "测试作者", "测试分类", copies, copies);
    }

    /**
     * 生成测试 ISBN（{@code bIsbn} 列是 {@code VARCHAR(20)}）。
     *
     * @param index 序号
     * @return ISBN
     */
    private String m_isbn(int index) {
        return m_isbnPrefix + index;
    }

    /**
     * 生成恰好 36 位的测试 uuid（{@code uUuid} 列是 {@code CHAR(36)}，超长会被 MySQL 拒绝）。
     *
     * @param tag   区分用途的短标记
     * @param index 序号
     * @return 补足 36 位的 uuid
     */
    private static String uuid(String tag, long index) {
        StringBuilder builder = new StringBuilder(UUID_PREFIX);
        builder.append(tag).append('-').append(index).append('-');
        builder.append(Long.toHexString(System.nanoTime()));
        while (builder.length() < 36) {
            builder.append('0');
        }
        return builder.substring(0, 36);
    }

    /**
     * 判断某行是否还在表中。
     *
     * @param table  表名
     * @param column 列名
     * @param value  取值
     * @return 存在返回 true
     */
    private static boolean rowExists(String table, String column, String value) {
        Connection connection = null;
        PreparedStatement statement = null;
        java.sql.ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?");
            statement.setString(1, value);
            rows = statement.executeQuery();
            return rows.next() && rows.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        } finally {
            close(rows, statement, connection);
        }
    }

    /**
     * 执行一条写语句（用于清理测试数据）。
     *
     * @param sql   语句
     * @param param 唯一参数
     */
    private static void executeUpdate(String sql, String param) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, param);
            statement.executeUpdate();
        } catch (SQLException e) {
            // 清理失败不影响用例结论
        } catch (RuntimeException e) {
            // 数据库不可用时 setUp 已跳过，这里同样忽略
        } finally {
            close(null, statement, connection);
        }
    }

    /**
     * 探测数据库是否可用。
     *
     * @return 能取到连接返回 true
     */
    private static boolean databaseAvailable() {
        Connection connection = null;
        try {
            connection = DbHelper.getConnection();
            // 连得上不代表建表脚本跑过；缺表时应当整体跳过，而不是抛一堆 Table doesn't exist
            return connection != null && DatabaseAvailability.isReady();
        } catch (SQLException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        } finally {
            close(null, null, connection);
        }
    }

    /**
     * 安静关闭结果集、语句与连接。
     *
     * @param rows       结果集；可为 null
     * @param statement  语句；可为 null
     * @param connection 连接；可为 null
     */
    private static void close(java.sql.ResultSet rows, PreparedStatement statement,
            Connection connection) {
        if (rows != null) {
            try {
                rows.close();
            } catch (SQLException ignored) {
                // 关闭失败无影响
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ignored) {
                // 关闭失败无影响
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // 关闭失败无影响
            }
        }
    }
}
