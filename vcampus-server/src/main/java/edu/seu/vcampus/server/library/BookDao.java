package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.message.PageResponse;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 图书馆藏数据访问接口，由数据库负责人提供实现。
 * 带 Connection 的方法必须复用传入连接，不得自行提交、回滚或关闭连接。
 * 实现须支持多线程调用，不得在实例字段中保存当前事务连接。
 */
public interface BookDao {

    /**
     * 按书名、作者、分类或全部字段进行模糊检索，按书名升序返回。
     * 仅返回未下架图书。
     * 实现负责获取和释放本次查询使用的连接及资源。
     *
     * @param query 已校验并规范化的分页查询条件
     * @return 匹配图书分页；total 仅统计未下架图书
     * @throws SQLException 数据访问失败
     */
    PageResponse<Book> search(BookQuery query) throws SQLException;

    /**
     * 在当前事务中按 ISBN 查询图书。
     * 包含已下架图书；存在时锁定该行至事务结束，使借出、修改与下架互斥。
     *
     * @param connection 业务层管理的事务连接
     * @param isbn ISBN
     * @return 图书，不存在时返回 null
     * @throws SQLException 数据访问失败
     */
    Book findByIsbn(Connection connection, String isbn) throws SQLException;

    /**
     * 原子增减可借数量，保证更新后数量处于零到馆藏总数之间。
     * 并发操作不得丢失更新或使库存越界；返回 false 时不得修改库存。
     * 下架图书禁止负向扣减，正向归还仍允许；必须与下架及馆藏修改使用同一行锁。
     *
     * @param connection 业务层管理的事务连接
     * @param isbn ISBN
     * @param change 数量变化，借书为 -1，还书为 1
     * @return 更新成功为 true；图书不存在或更新将越界为 false
     * @throws SQLException 数据访问失败
     */
    boolean adjustAvailable(Connection connection, String isbn, int change) throws SQLException;

    /**
     * 管理员查询全部馆藏，含已下架记录；搜索规则同 search，自行管理查询连接。
     * @param query 已校验并规范化的分页查询条件
     * @return 图书分页，无匹配返回空页；total 包含已下架馆藏
     * @throws SQLException 查询失败
     */
    PageResponse<Book> searchCatalog(BookQuery query) throws SQLException;

    /**
     * 新增馆藏；数据库必须原子保证 ISBN 唯一，包含已下架记录。
     * @param connection 业务事务连接，不得自行提交、回滚或关闭
     * @param book 已校验的新书，可借数量等于总数、未下架
     * @return 新增成功为 true，ISBN 已存在为 false
     * @throws SQLException 数据访问失败
     */
    boolean insertBook(Connection connection, Book book) throws SQLException;

    /**
     * 更新已锁定的馆藏资料与数量，ISBN 不变，不得修改下架状态或借阅历史。
     * @param connection 已通过 findByIsbn 锁定图书的事务连接
     * @param book 服务计算后的资料、总数与可借数量
     * @return 更新成功为 true，记录不存在为 false
     * @throws SQLException 数据访问失败
     */
    boolean updateBook(Connection connection, Book book) throws SQLException;

    /**
     * 将已锁定的图书标记为下架，不删除图书、库存及借阅记录。
     * @param connection 已通过 findByIsbn 锁定图书的事务连接
     * @param isbn ISBN
     * @return 标记成功为 true，记录不存在为 false
     * @throws SQLException 数据访问失败
     */
    boolean withdrawBook(Connection connection, String isbn) throws SQLException;
}
