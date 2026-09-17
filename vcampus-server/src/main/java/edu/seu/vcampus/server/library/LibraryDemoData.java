package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.server.user.AuthModule;
import edu.seu.vcampus.server.user.AuthService;
import edu.seu.vcampus.server.user.UserRepository;
import edu.seu.vcampus.server.user.UserRepository.Credential;
import java.util.Date;

/** 预置一个有一本逾期未还图书的演示读者，便于客户端体验归还与缴费流程。 */
public final class LibraryDemoData {
    /** 演示学生登录名。 */
    public static final String USERNAME = "fine_demo";

    /** 演示学生初始口令。 */
    public static final String PASSWORD = "1";

    /** 演示逾期图书 ISBN。 */
    public static final String ISBN = "9787302423287";

    /** 演示逾期图书书名。 */
    public static final String TITLE = "Java语言程序设计";

    private LibraryDemoData() {
    }

    /**
     * 在内存借阅库中写入一条逾期未还的演示记录。
     * @param borrows 借阅记录内存库；null 时忽略
     */
    public static void seed(BorrowDaoMemory borrows) {
        if (borrows == null) {
            return;
        }
        AuthService auth = AuthModule.authService();
        UserRepository users = AuthModule.repository();
        if (auth == null || users == null) {
            return;
        }
        if (!auth.exists(USERNAME)) {
            auth.register(USERNAME, "滞纳金演示学生", PASSWORD, "学生");
        }
        Credential demo = users.findByUsername(USERNAME);
        if (demo == null || demo.getUuid() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        BorrowRecord record = new BorrowRecord(demo.getUuid(), ISBN, TITLE,
                new Date(now - 45L * 24L * 60L * 60L * 1000L),
                new Date(now - 15L * 24L * 60L * 60L * 1000L));
        borrows.seedRecord(record);
    }
}
