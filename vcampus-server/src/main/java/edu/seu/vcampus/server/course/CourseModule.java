package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.College;
import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.Field;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.common.course.Timeslot;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import edu.seu.vcampus.server.user.AccountProvisioning;
import edu.seu.vcampus.server.user.AuthModule;
import edu.seu.vcampus.server.user.SessionManager;
import edu.seu.vcampus.server.user.UserRepository;

/**
 * 选课模块装配入口：登记选课命令码与处理器，并保证课程目录里有可选的课。
 *
 * <p>
 * 与 {@code StudentModule} / {@code BankModule} 同构，应用组装层只需调用
 * {@link #register(ServerMessageDispatcher, SessionManager, AccountProvisioning)}。
 *
 * <p>
 * 学院、教室、三门课由本模块在启动时写进数据库（{@link #seedCatalog}），用的是写死的 uuid， 重复启动走 upsert
 * 覆盖而不是堆出第二套。这是<b>演示数据</b>：验收时界面一打开得有课可选。 它不属于生产逻辑，将来要接真实课表时应当换掉。
 */
public final class CourseModule {

    /** 演示学院名称。 */
    private static final String DEMO_COLLEGE = "计算机学院";

    /** 演示学期。 */
    private static final String DEMO_SEMESTER = "2026-2027-1";

    // 演示目录一律用写死的 uuid，而不是每次 new 一个：College / Classroom / CourseSection 的
    // save 在 uuid 为空时才自动生成，写死之后重复启动走的是 upsert 覆盖，而不会往库里堆出第二套
    // 同名学院，也不会撞上 crLocation / coId 的唯一键报错。

    /** 演示学院 uuid。 */
    private static final String DEMO_COLLEGE_UUID = "00000000-0000-0000-0000-000000000c01";

    /** 演示教室「教一」uuid。 */
    private static final String DEMO_ROOM1_UUID = "00000000-0000-0000-0000-000000000c11";

    /** 演示教室「教二」uuid。 */
    private static final String DEMO_ROOM2_UUID = "00000000-0000-0000-0000-000000000c12";

    /** 演示课程 CS101 uuid。 */
    private static final String DEMO_COURSE1_UUID = "00000000-0000-0000-0000-000000000c21";

    /** 演示课程 CS102 uuid。 */
    private static final String DEMO_COURSE2_UUID = "00000000-0000-0000-0000-000000000c22";

    /** 演示课程 CS103 uuid。 */
    private static final String DEMO_COURSE3_UUID = "00000000-0000-0000-0000-000000000c23";

    /** 课程目录 DAO 单例。 */
    private static volatile CourseDao s_courseDao;

    /** 成绩 DAO 单例。 */
    private static volatile ScoreDao s_scoreDao;

    /** 私有构造器，禁止实例化装配入口。 */
    private CourseModule() {
    }

    /**
     * 取课程目录 DAO 单例
     *
     * <p>
     * 全服只应有一份目录：演示种子要往<b>同一个</b>目录里写选课与成绩，再 new 一个就写到别的实例上， 界面上看不到。
     *
     * @return 课程目录 DAO
     */
    public static CourseDao courseDao() {
        CourseDao dao = s_courseDao;
        if (dao == null) {
            synchronized (CourseModule.class) {
                dao = s_courseDao;
                if (dao == null) {
                    dao = new CourseDao(new CourseStoreJdbc());
                    s_courseDao = dao;
                }
            }
        }
        return dao;
    }

    /**
     * 取成绩 DAO 单例（落地 MySQL），与 {@link #courseDao()} 配对。
     *
     * @return 成绩 DAO
     */
    public static ScoreDao scoreDao() {
        ScoreDao dao = s_scoreDao;
        if (dao == null) {
            synchronized (CourseModule.class) {
                dao = s_scoreDao;
                if (dao == null) {
                    dao = new ScoreDao(new ScoreStoreJdbc());
                    s_scoreDao = dao;
                }
            }
        }
        return dao;
    }

    /**
     * 生产装配：课程目录与成绩 DAO 取自本模块单例，账户库取账号模块的那一份。
     *
     * @param dispatcher   应用共享的消息分发器
     * @param sessions     全服唯一的会话表
     * @param provisioning 开户钩子登记表；null 表示不为新账号建档
     */
    public static void register(ServerMessageDispatcher dispatcher, SessionManager sessions,
            AccountProvisioning provisioning) {
        register(dispatcher, sessions, provisioning, courseDao(), scoreDao());
    }

    /**
     * 用调用方给定的课程目录与成绩 DAO 装配（测试注入替身走这条）。
     *
     * <p>
     * 之所以要能注入：演示种子得往<b>同一份</b>课程目录里写选课与成绩，若这里再 new 一个，种子写进 的是另一个实例，界面上就看不到 —— 与商店那次「第二个
     * BankService」是同一类坑。
     *
     * @param dispatcher   应用共享的消息分发器
     * @param sessions     全服唯一的会话表
     * @param provisioning 开户钩子登记表；null 表示不为新账号建档
     * @param dao          课程目录 DAO
     * @param scores       成绩 DAO
     */
    public static void register(ServerMessageDispatcher dispatcher, SessionManager sessions,
            AccountProvisioning provisioning, CourseDao dao, ScoreDao scores) {
        if (dispatcher == null || sessions == null) {
            throw new IllegalArgumentException("dispatcher and sessions must not be null");
        }
        if (dao == null || scores == null) {
            throw new IllegalArgumentException("dao and scores must not be null");
        }
        String collegeUuid = seedCatalog(dao);
        CourseManagementService management = new CourseManagementService(dao);
        CourseService service = new CourseService(dao, scores);
        CourseMessageHandler handler = new CourseMessageHandler(dao, management, service,
                AuthModule.repository(), sessions);
        dispatcher.register(Command.COURSE_LIST, handler);
        dispatcher.register(Command.COURSE_SELECT, handler);
        dispatcher.register(Command.COURSE_DROP, handler);
        dispatcher.register(Command.SCORE_QUERY, handler);
        dispatcher.register(Command.SCORE_SAVE, handler);
        dispatcher.register(Command.COURSE_TEACHING_LIST, handler);
        dispatcher.register(Command.COURSE_SCHEDULE, handler);
        dispatcher.register(Command.COURSE_PREFERENCE_GET, handler);
        dispatcher.register(Command.COURSE_PREFERENCE_SET, handler);
        dispatcher.register(Command.COURSE_CLASSROOM_LIST, handler);
        if (provisioning != null) {
            CourseProvisioner provisioner = new CourseProvisioner(dao, collegeUuid);
            provisioning.add(provisioner);
            provisionExisting(provisioner, AuthModule.repository());
            seedDemoClaim(dao, management, AuthModule.repository());
        }
    }

    private static String seedCatalog(CourseDao dao) {
        College college = new College();
        college.setUuid(DEMO_COLLEGE_UUID);
        college.setName(DEMO_COLLEGE);
        college.getResearchDirections().add(new Field("人工智能"));
        college.getResearchDirections().add(new Field("网络"));
        college.getMajors().add(new Field("软件工程"));
        dao.saveCollege(college);

        seedClassroom(dao, DEMO_ROOM1_UUID, college.getUuid(), 60, "教一");
        seedClassroom(dao, DEMO_ROOM2_UUID, college.getUuid(), 60, "教二");
        seedCourse(dao, DEMO_COURSE1_UUID, college.getUuid(), "CS101", "数据结构", 3, 40);
        seedCourse(dao, DEMO_COURSE2_UUID, college.getUuid(), "CS102", "计算机网络", 2, 40);
        seedCourse(dao, DEMO_COURSE3_UUID, college.getUuid(), "CS103", "操作系统", 3, 30);
        return college.getUuid();
    }

    private static void seedClassroom(CourseDao dao, String uuid, String collegeUuid, int capacity,
            String location) {
        Classroom room = new Classroom();
        room.setUuid(uuid);
        room.setCollegeUuid(collegeUuid);
        room.setCapacity(capacity);
        room.setLocation(location);
        for (int day = 1; day <= 5; day++) {
            room.getAvailableTimeslots().add(new Timeslot(day, 8 * 60, 20 * 60));
        }
        dao.saveClassroom(room);
    }

    private static void seedCourse(CourseDao dao, String uuid, String collegeUuid, String code,
            String name, int credit, int capacity) {
        CourseSection course = new CourseSection();
        course.setUuid(uuid);
        course.setCode(code);
        course.setName(name);
        course.setCollegeUuid(collegeUuid);
        course.setCredit(credit);
        course.setCapacity(capacity);
        course.setSemester(DEMO_SEMESTER);
        course.setPreferredLocation("教一");
        dao.saveCourse(course);
    }

    private static void provisionExisting(CourseProvisioner provisioner, UserRepository users) {
        if (provisioner == null || users == null) {
            return;
        }
        for (UserRepository.Credential account : users.findAll()) {
            if (account == null || account.getUuid() == null) {
                continue;
            }
            Role role = account.getRole() == null ? null : Role.fromDisplayName(account.getRole());
            provisioner.provision(account.getUuid(), account.getDisplayName(), role);
        }
    }

    private static void seedDemoClaim(CourseDao dao, CourseManagementService management,
            UserRepository users) {
        if (users == null) {
            return;
        }
        for (UserRepository.Credential account : users.findAll()) {
            if (account == null || !"教师".equals(account.getRole())) {
                continue;
            }
            Teacher teacher = dao.findTeacher(account.getUuid());
            if (teacher != null) {
                CourseSection course = firstUnclaimed(dao);
                if (course != null) {
                    management.claimCourse(teacher.getUuid(), course.getUuid());
                }
            }
            return;
        }
    }

    private static CourseSection firstUnclaimed(CourseDao dao) {
        for (CourseSection course : dao.findAllCourses()) {
            if (course.getTeacherUuid() == null) {
                return course;
            }
        }
        return null;
    }
}
