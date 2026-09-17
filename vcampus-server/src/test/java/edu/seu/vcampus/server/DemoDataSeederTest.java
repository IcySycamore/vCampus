package edu.seu.vcampus.server;

import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.server.library.BookDaoMemory;
import edu.seu.vcampus.server.library.BorrowDaoMemory;
import edu.seu.vcampus.server.library.LibraryAccountDaoMemory;
import edu.seu.vcampus.server.user.AuthService;
import edu.seu.vcampus.server.user.FileUserRepository;
import edu.seu.vcampus.server.user.NonceManager;
import edu.seu.vcampus.server.user.SessionManager;
import edu.seu.vcampus.server.user.UserRepository;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 演示数据种子测试：数量、幂等、开关以及「逾期 / 未逾期」两类借阅的日期语义。
 */
class DemoDataSeederTest {

    /** 临时账户文件。 */
    private File m_usersFile;

    /** 每个用例前准备一份干净的账户文件。 */
    @BeforeEach
    void setUp() throws IOException {
        m_usersFile = File.createTempFile("vcampus-demo-users", ".tsv");
        if (!m_usersFile.delete()) {
            throw new IOException("无法准备临时账户文件");
        }
    }

    /** 用例后清掉临时文件与系统属性。 */
    @AfterEach
    void tearDown() {
        System.clearProperty(DemoDataSeeder.PROPERTY);
        if (m_usersFile != null && m_usersFile.exists() && !m_usersFile.delete()) {
            m_usersFile.deleteOnExit();
        }
    }

    @Test
    void seedsAccountsBooksAndBorrowsIncludingOverdue() throws Exception {
        Fixture fixture = new Fixture();

        DemoDataSeeder.SeedReport report = fixture.seed();

        assertEquals(DemoDataSeeder.STUDENT_COUNT + DemoDataSeeder.TEACHER_COUNT,
                report.getNewUsers(), "应一次性建好学生与教师账号");
        assertEquals(DemoDataSeeder.EXTRA_BOOKS, report.getNewBooks(), "应写入全部额外馆藏");
        assertEquals(DemoDataSeeder.BORROWS, report.getBorrows(), "应写入全部借阅记录");
        assertEquals(DemoDataSeeder.OVERDUE, report.getOverdue(), "其中应有若干条已逾期");
        assertNotNull(fixture.users.findByUsername(DemoDataSeeder.studentName(1)),
                "学生账号应落到账户库");
        assertNotNull(fixture.users.findByUsername(DemoDataSeeder.teacherName(1)),
                "教师账号应落到账户库");
    }

    @Test
    void seedIsIdempotent() throws Exception {
        Fixture fixture = new Fixture();
        fixture.seed();

        DemoDataSeeder.SeedReport second = fixture.seed();

        assertEquals(0, second.getNewUsers(), "重复执行不应重复建号");
        assertEquals(0, second.getNewBooks(), "重复执行不应重复插书");
    }

    @Test
    void seedIfEnabledDoesNothingWhenSwitchOff() throws Exception {
        Fixture fixture = new Fixture();

        assertNull(DemoDataSeeder.seedIfEnabled(fixture.users, fixture.auth, fixture.books,
                fixture.borrows, fixture.accounts), "开关关闭时不应注入任何数据");
        assertNull(fixture.users.findByUsername(DemoDataSeeder.studentName(1)),
                "开关关闭时不应建号");
    }

    @Test
    void overdueBorrowsArePastDueWhileFreshOnesAreNot() throws Exception {
        Fixture fixture = new Fixture();
        fixture.seed();

        int overdue = 0;
        int fresh = 0;
        Date now = new Date();
        for (int i = 1; i <= DemoDataSeeder.BORROWS; i++) {
            UserRepository.Credential credential = fixture.users
                    .findByUsername(DemoDataSeeder.studentName(i));
            assertNotNull(credential, "参与借阅的学生应存在");
            for (BorrowRecord record : fixture.borrows.findByUser(credential.getUuid())) {
                if (record.getDueAt().before(now)) {
                    overdue++;
                } else {
                    fresh++;
                }
            }
        }
        assertEquals(DemoDataSeeder.OVERDUE, overdue, "逾期条数应与种子一致");
        assertEquals(DemoDataSeeder.BORROWS - DemoDataSeeder.OVERDUE, fresh,
                "其余应为未逾期，含应还在未来");
    }

    /** 一次种子注入所需的全部依赖。 */
    private final class Fixture {

        /** 账户库（落地临时文件，模拟生产）。 */
        private final UserRepository users;

        /** 认证服务。 */
        private final AuthService auth;

        /**
         * 装配依赖；账户库落地临时文件，与生产一致。
         *
         * @throws IOException 账户文件初始化失败
         */
        Fixture() throws IOException {
            this.users = new FileUserRepository(m_usersFile);
            this.auth = new AuthService(users, NonceManager.getInstance(),
                    SessionManager.getInstance());
        }

        /** 馆藏。 */
        private final BookDaoMemory books = BookDaoMemory.withSampleBooks();

        /** 借阅记录。 */
        private final BorrowDaoMemory borrows = new BorrowDaoMemory();

        /** 读者账户。 */
        private final LibraryAccountDaoMemory accounts = new LibraryAccountDaoMemory();

        /** 执行注入。 */
        private DemoDataSeeder.SeedReport seed() throws Exception {
            return DemoDataSeeder.seed(users, auth, books, borrows, accounts);
        }
    }
}
