package edu.seu.vcampus.server;

import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.Score;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.LibraryAccount;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.server.course.CourseDao;
import edu.seu.vcampus.server.course.ScoreDao;
import edu.seu.vcampus.server.library.BookDao;
import edu.seu.vcampus.server.library.BorrowDao;
import edu.seu.vcampus.server.library.LibraryAccountDao;
import edu.seu.vcampus.server.user.AuthService;
import edu.seu.vcampus.server.user.UserRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 演示数据种子：一次性准备「有量、有逾期」的演示数据。
 *
 * <p>
 * 默认<b>不生效</b>，只有启动时显式打开开关才会注入：
 *
 * <pre>
 * java -Dvcampus.demo.seed=true -jar vcampusServer.jar
 * </pre>
 *
 * <p>
 * 注入内容：{@value #STUDENT_COUNT} 个学生与 {@value #TEACHER_COUNT} 个教师账号（统一口令
 * {@value #PASSWORD}，注册即同步建立学籍档案）、{@value #EXTRA_BOOKS} 本额外馆藏、 {@value #BORROWS} 条借阅记录（其中
 * {@value #OVERDUE} 条已逾期），并为借书人建立读者账户。
 *
 * <p>
 * 幂等：账号已存在会被跳过，重复启动不会报错也不会产生重复账号。
 */
public final class DemoDataSeeder {

    /** 开启演示种子的系统属性名。 */
    public static final String PROPERTY = "vcampus.demo.seed";

    /** 学生账号数。 */
    public static final int STUDENT_COUNT = 40;

    /** 教师账号数。 */
    public static final int TEACHER_COUNT = 10;

    /** 演示账号统一口令。 */
    public static final String PASSWORD = "init1234";

    /** 额外馆藏册数（服务端另有 7 本自带样例）。 */
    public static final int EXTRA_BOOKS = 23;

    /** 借阅记录总数。 */
    public static final int BORROWS = 24;

    /** 其中已逾期的条数。 */
    public static final int OVERDUE = 8;

    /** 借期天数（与图书馆策略一致：借出后 {@value #LOAN_DAYS} 天应还）。 */
    public static final int LOAN_DAYS = 30;

    /** 读者借阅上限。 */
    public static final int BORROW_LIMIT = 5;

    /** 一天对应的毫秒数。 */
    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    /** 额外馆藏：ISBN、书名、作者、分类、册数。 */
    private static final String[][] BOOK_SEEDS = {
            { "9787115428028", "计算机网络（第7版）", "谢希仁", "计算机", "6" },
            { "9787111544937", "代码大全（第2版）", "Steve McConnell", "软件工程", "3" },
            { "9787121353260", "深入理解计算机系统", "Randal E. Bryant", "计算机", "5" },
            { "9787111591559", "操作系统概念", "Abraham Silberschatz", "计算机", "4" },
            { "9787302568407", "软件工程导论", "张海藩", "软件工程", "8" },
            { "9787115477101", "Linux 内核设计与实现", "Robert Love", "计算机", "3" },
            { "9787115546081", "图解 HTTP", "上野宣", "计算机", "5" },
            { "9787121316272", "重构：改善既有代码的设计", "Martin Fowler", "软件工程", "4" },
            { "9787111213827", "编译原理", "Alfred V. Aho", "计算机", "6" },
            { "9787111558423", "数据结构与算法分析", "Mark Allen Weiss", "计算机", "7" },
            { "9787115477521", "机器学习", "周志华", "人工智能", "5" },
            { "9787121363139", "统计学习方法", "李航", "人工智能", "4" },
            { "9787111636663", "数据库原理与应用", "王珊", "数据库", "9" },
            { "9787302521044", "MySQL 技术内幕", "姜承尧", "数据库", "5" },
            { "9787111599364", "高性能 MySQL", "Baron Schwartz", "数据库", "3" },
            { "9787115293800", "数字逻辑基础", "康华光", "电子信息", "6" },
            { "9787040396898", "高等数学（上册）", "同济大学", "数学", "12" },
            { "9787040396904", "高等数学（下册）", "同济大学", "数学", "12" },
            { "9787040470215", "线性代数", "同济大学", "数学", "10" },
            { "9787111698832", "离散数学及其应用", "Kenneth H. Rosen", "数学", "6" },
            { "9787301211342", "中国近现代史纲要", "本书编写组", "人文", "8" },
            { "9787111407011", "人月神话", "Frederick P. Brooks", "软件工程", "4" },
            { "9787115428029", "设计模式：可复用面向对象软件的基础", "Erich Gamma", "软件工程", "5" }
    };

    /** 私有构造器，禁止实例化。 */
    private DemoDataSeeder() {
    }

    /**
     * 开关开启时注入演示数据。
     *
     * @param users    账户库
     * @param auth     认证服务（建号入口，注册后自动建立各模块档案）
     * @param books    馆藏数据访问
     * @param borrows  借阅记录数据访问
     * @param accounts 读者账户数据访问
     * @return 本次注入的统计；开关关闭时返回 null
     * @throws SQLException 数据访问失败
     */
    public static SeedReport seedIfEnabled(UserRepository users, AuthService auth, BookDao books,
            BorrowDao borrows, LibraryAccountDao accounts, CourseDao courses, ScoreDao scores)
            throws SQLException {
        if (!Boolean.getBoolean(PROPERTY)) {
            return null;
        }
        SeedReport report = seed(users, auth, books, borrows, accounts, courses, scores);
        System.out.println("演示种子：新建账号 " + report.getNewUsers() + " 个、馆藏 "
                + report.getNewBooks() + " 本、借阅 " + report.getOverdue() + " 条逾期 / "
                + report.getBorrows() + " 条记录、选课 " + report.getEnrollments() + " 条 / 成绩 "
                + report.getScores() + " 条（统一口令 " + PASSWORD + "）");
        return report;
    }

    /**
     * 执行注入（与开关无关，便于测试直接调用）。
     *
     * @param users    账户库
     * @param auth     认证服务
     * @param books    馆藏数据访问
     * @param borrows  借阅记录数据访问
     * @param accounts 读者账户数据访问
     * @return 注入统计
     * @throws SQLException 数据访问失败
     */
    public static SeedReport seed(UserRepository users, AuthService auth, BookDao books,
            BorrowDao borrows, LibraryAccountDao accounts, CourseDao courses, ScoreDao scores)
            throws SQLException {
        SeedReport report = new SeedReport();
        report.setNewUsers(createAccounts(auth));
        report.setNewBooks(createBooks(books));
        createLoan(users, books, borrows, accounts, report);
        createCoursework(users, courses, scores, report);
        return report;
    }

    /**
     * 给演示学生选课并录成绩。
     *
     * <p>
     * 课程目录由 {@code CourseModule} 播种（CS101 数据结构 / CS102 计算机网络 / CS103 操作系统），
     * 这里按课程编号找回来再挂学生，因此两个模块必须共用同一份 {@link CourseDao}。
     *
     * <p>
     * 成绩不是人人都有：每三名学生留一个空成绩，用来演示「已选课但未录入」这个中间态。
     *
     * @param users   账户库
     * @param courses 课程目录
     * @param scores  成绩
     * @param report  统计出参
     */
    private static void createCoursework(UserRepository users, CourseDao courses, ScoreDao scores,
            SeedReport report) {
        String[] codes = { "CS101", "CS102", "CS103" };
        double[] marks = { 88.0, 76.5, 92.0 };
        int enrolled = 0;
        int graded = 0;
        for (int i = 1; i <= STUDENT_COUNT; i++) {
            UserRepository.Credential account = users.findByUsername(studentName(i));
            if (account == null || account.getUuid() == null) {
                continue;
            }
            for (int c = 0; c < codes.length; c++) {
                CourseSection course = findCourse(courses, codes[c]);
                if (course == null) {
                    continue;
                }
                if (!course.getStudentUuids().add(account.getUuid())) {
                    continue;// 已经选过，重复启动不重复计数
                }
                courses.saveCourse(course);
                enrolled++;
                // 每三名学生留一个空成绩：记录照样落，只是不填分数，
                // 用来演示「已选课但未录入」这个中间态（不是没选课）
                Score score = new Score(account.getUuid(), codes[c], course.getSemester());
                if (i % 3 != 0) {
                    score.setScore(Double.valueOf(marks[c] - (i % 7)));
                    graded++;
                }
                scores.save(score);
            }
        }
        report.setEnrollments(enrolled);
        report.setScores(graded);
    }

    /**
     * 按课程编号找课程。
     *
     * @param courses 课程目录
     * @param code    课程编号
     * @return 课程；不存在返回 null
     */
    private static CourseSection findCourse(CourseDao courses, String code) {
        for (CourseSection course : courses.findAllCourses()) {
            if (code.equals(course.getCode())) {
                return course;
            }
        }
        return null;
    }

    /**
     * 批量建号：{@value #STUDENT_COUNT} 个学生 + {@value #TEACHER_COUNT} 个教师。
     *
     * @param auth 认证服务
     * @return 实际新建的账号数（已存在的跳过）
     */
    private static int createAccounts(AuthService auth) {
        int created = 0;
        for (int i = 1; i <= STUDENT_COUNT; i++) {
            created += registerQuietly(auth, studentName(i), "演示学生" + pad(i),
                    Role.STUDENT.getDisplayName());
        }
        for (int i = 1; i <= TEACHER_COUNT; i++) {
            created += registerQuietly(auth, teacherName(i), "演示教师" + pad(i),
                    Role.TEACHER.getDisplayName());
        }
        return created;
    }

    /**
     * 建一个账号，已存在时静默跳过（保证种子可重复执行）。
     *
     * @param auth        认证服务
     * @param username    登录名
     * @param displayName 姓名
     * @param role        角色显示名
     * @return 新建返回 1，跳过返回 0
     */
    private static int registerQuietly(AuthService auth, String username, String displayName,
            String role) {
        try {
            auth.register(username, displayName, PASSWORD, role);
            return 1;
        } catch (IllegalStateException e) {
            return 0;// 账号已存在：种子幂等
        }
    }

    /**
     * 写入额外馆藏。
     *
     * @param books 馆藏数据访问
     * @return 实际新增的册数
     * @throws SQLException 数据访问失败
     */
    private static int createBooks(BookDao books) throws SQLException {
        int created = 0;
        for (String[] seed : BOOK_SEEDS) {
            String isbn = seed[0];
            String title = seed[1];
            String author = seed[2];
            String category = seed[3];
            int copies = Integer.parseInt(seed[4]);
            if (books.insertBook(null, new Book(isbn, title, author, category, copies, copies))) {
                created++;
            }
        }
        return created;
    }

    /**
     * 造借阅记录：先给借书的同学建读者账户，再插入记录并扣减可借数量。
     *
     * @param users    账户库
     * @param books    馆藏数据访问
     * @param borrows  借阅记录数据访问
     * @param accounts 读者账户数据访问
     * @param report   统计累加器
     * @throws SQLException 数据访问失败
     */
    private static void createLoan(UserRepository users, BookDao books, BorrowDao borrows,
            LibraryAccountDao accounts, SeedReport report) throws SQLException {
        List<String[]> borrowers = collectBorrowers(users);
        if (borrowers.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (int i = 0; i < BORROWS; i++) {
            String[] borrower = borrowers.get(i % borrowers.size());
            String[] book = BOOK_SEEDS[i % BOOK_SEEDS.length];
            boolean overdue = i < OVERDUE;
            // 逾期：借出 35~42 天前，应还日在 5~12 天前；未逾期：借出 2~17 天前，应还日仍在未来
            long borrowedAt = overdue
                    ? now - (LOAN_DAYS + 5L + i) * DAY_MILLIS
                    : now - (2L + i) * DAY_MILLIS;
            long dueAt = borrowedAt + LOAN_DAYS * DAY_MILLIS;
            ensureAccount(accounts, borrower[0], new Date(borrowedAt));
            if (borrows.hasActive(null, borrower[0], book[0])) {
                continue;// 该同学这本书还没还：种子幂等，不重复借也不重复扣库存
            }
            BorrowRecord record = new BorrowRecord(borrower[0], book[0], book[1],
                    new Date(borrowedAt), new Date(dueAt));
            if (borrows.insert(null, record) > 0) {
                books.adjustAvailable(null, book[0], -1);
                report.setBorrows(report.getBorrows() + 1);
                if (overdue) {
                    report.setOverdue(report.getOverdue() + 1);
                }
            }
        }
    }

    /**
     * 取出参与借阅的学生 uuid。
     *
     * @param users 账户库
     * @return 每项为该学生的 uuid；取不到返回空列表
     */
    private static List<String[]> collectBorrowers(UserRepository users) {
        List<String[]> borrowers = new ArrayList<String[]>();
        for (int i = 1; i <= STUDENT_COUNT; i++) {
            UserRepository.Credential credential = users.findByUsername(studentName(i));
            if (credential != null && credential.getUuid() != null) {
                borrowers.add(new String[] { credential.getUuid() });
            }
        }
        return borrowers;
    }

    /**
     * 保证读者账户存在。
     *
     * @param accounts 读者账户数据访问
     * @param uuid     用户 uuid
     * @param since    建档时间
     * @throws SQLException 数据访问失败
     */
    private static void ensureAccount(LibraryAccountDao accounts, String uuid, Date since)
            throws SQLException {
        if (accounts.findByUserUuid(uuid) == null) {
            accounts.insert(new LibraryAccount(uuid, BORROW_LIMIT, since));
        }
    }

    /**
     * 学生登录名。
     *
     * @param index 序号，从 1 开始
     * @return 形如 {@code student001}
     */
    static String studentName(int index) {
        return "student" + pad(index);
    }

    /**
     * 教师登录名。
     *
     * @param index 序号，从 1 开始
     * @return 形如 {@code teacher001}
     */
    static String teacherName(int index) {
        return "teacher" + pad(index);
    }

    /**
     * 三位零填充。
     *
     * @param value 数值
     * @return 定长三位字符串
     */
    private static String pad(int value) {
        String text = String.valueOf(value);
        while (text.length() < 3) {
            text = "0" + text;
        }
        return text;
    }

    /** 注入结果统计。 */
    public static final class SeedReport {

        /** 新建账号数。 */
        private int newUsers;

        /** 新建馆藏数。 */
        private int newBooks;

        /** 写入的借阅记录数。 */
        private int borrows;

        /** 其中逾期条数。 */
        private int overdue;

        /** 写入的选课记录数。 */
        private int enrollments;

        /** 录入的成绩条数。 */
        private int scores;

        /** @return 新建账号数 */
        public int getNewUsers() {
            return newUsers;
        }

        /** @param value 新建账号数 */
        void setNewUsers(int value) {
            this.newUsers = value;
        }

        /** @return 新建馆藏数 */
        public int getNewBooks() {
            return newBooks;
        }

        /** @param value 新建馆藏数 */
        void setNewBooks(int value) {
            this.newBooks = value;
        }

        /** @return 借阅记录数 */
        public int getBorrows() {
            return borrows;
        }

        /** @param value 借阅记录数 */
        void setBorrows(int value) {
            this.borrows = value;
        }

        /** @return 逾期条数 */
        public int getOverdue() {
            return overdue;
        }

        /** @param value 逾期条数 */
        void setOverdue(int value) {
            this.overdue = value;
        }

        /** @return 新建的选课记录数 */
        public int getEnrollments() {
            return enrollments;
        }

        /** @param value 新建的选课记录数 */
        void setEnrollments(int value) {
            this.enrollments = value;
        }

        /** @return 录入的成绩条数 */
        public int getScores() {
            return scores;
        }

        /** @param value 录入的成绩条数 */
        void setScores(int value) {
            this.scores = value;
        }
    }
}
