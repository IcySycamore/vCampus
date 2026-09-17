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
 * 选课模块装配入口：登记选课命令码与处理器，并预置演示课表。
 *
 * <p>
 * 与 {@code StudentModule} / {@code BankModule} 同构，应用组装层只需调用
 * {@link #register(ServerMessageDispatcher, SessionManager)}。课程与教室目前为内存实现， 重启后由本模块重新预置演示课表。
 */
public final class CourseModule {

    /** 演示学院名称。 */
    private static final String DEMO_COLLEGE = "计算机学院";

    /** 演示学期。 */
    private static final String DEMO_SEMESTER = "2026-2027-1";

    /** 私有构造器，禁止实例化装配入口。 */
    private CourseModule() {
    }

    /**
     * 登记选课模块全部命令（不接入开户钩子）。
     *
     * @param dispatcher 应用共享的消息分发器
     * @param sessions   全服唯一的会话表
     */
    public static void register(ServerMessageDispatcher dispatcher, SessionManager sessions) {
        register(dispatcher, sessions, null);
    }

    /**
     * 登记选课模块全部命令，并把选课开户钩子接入账户生命周期。
     *
     * @param dispatcher   应用共享的消息分发器
     * @param sessions     全服唯一的会话表
     * @param provisioning 开户钩子登记表；null 表示不为新账号建档
     */
    public static void register(ServerMessageDispatcher dispatcher, SessionManager sessions,
            AccountProvisioning provisioning) {
        if (dispatcher == null || sessions == null) {
            throw new IllegalArgumentException("dispatcher and sessions must not be null");
        }
        // 课程目录与成绩同一套开关：缺省内存，-Dvcampus.store=jdbc 时从 MySQL 恢复。
        // 库里已有学院就不再播种演示目录，否则每次启动都会多出一套同名的学院与课程。
        boolean jdbc = "jdbc".equalsIgnoreCase(System.getProperty("vcampus.store"));
        CourseStore courseStore = jdbc ? new CourseStoreJdbc() : new CourseStoreMemory();
        CourseDao dao = new CourseDao(courseStore);
        String collegeUuid = jdbc && !courseStore.loadColleges().isEmpty()
                ? courseStore.loadColleges().get(0).getUuid()
                : seedCatalog(dao);
        CourseManagementService management = new CourseManagementService(dao);
        // 成绩按与其它模块同一套开关落库；课程目录（CourseDao）仍是内存实现，待补齐 JDBC 后端
        ScoreStore scoreStore = "jdbc".equalsIgnoreCase(System.getProperty("vcampus.store"))
                ? new ScoreStoreJdbc()
                : new ScoreStoreMemory();
        CourseService service = new CourseService(dao, new ScoreDao(scoreStore));
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
        college.setName(DEMO_COLLEGE);
        college.getResearchDirections().add(new Field("人工智能"));
        college.getResearchDirections().add(new Field("网络"));
        college.getMajors().add(new Field("软件工程"));
        dao.saveCollege(college);

        seedClassroom(dao, college.getUuid(), 60, "教一");
        seedClassroom(dao, college.getUuid(), 60, "教二");
        seedCourse(dao, college.getUuid(), "CS101", "数据结构", 3, 40);
        seedCourse(dao, college.getUuid(), "CS102", "计算机网络", 2, 40);
        seedCourse(dao, college.getUuid(), "CS103", "操作系统", 3, 30);
        return college.getUuid();
    }

    private static void seedClassroom(CourseDao dao, String collegeUuid, int capacity,
            String location) {
        Classroom room = new Classroom();
        room.setCollegeUuid(collegeUuid);
        room.setCapacity(capacity);
        room.setLocation(location);
        for (int day = 1; day <= 5; day++) {
            room.getAvailableTimeslots().add(new Timeslot(day, 8 * 60, 20 * 60));
        }
        dao.saveClassroom(room);
    }

    private static void seedCourse(CourseDao dao, String collegeUuid, String code, String name,
            int credit, int capacity) {
        CourseSection course = new CourseSection();
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
