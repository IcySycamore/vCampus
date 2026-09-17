package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import edu.seu.vcampus.server.user.AccountProvisioning;
import edu.seu.vcampus.server.user.AuthModule;
import edu.seu.vcampus.server.user.SessionManager;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 选课模块装配入口：登记选课命令码与处理器。
 *
 * <p>
 * 与 {@code StudentModule} / {@code BankModule} 同构，应用组装层只需调用
 * {@link #register(ServerMessageDispatcher, SessionManager, AccountProvisioning)}。
 *
 * <p>
 */
public final class CourseModule {

    /**
     * 缺省学院 uuid。
     *
     * <p>
     * 只在学院池为空时当傅底用（池正常由 {@link #bootstrapColleges()} 从引导文件建立）。它指向的行
     * <b>必须已经存在于库里</b>：{@code tblCourseStudent.cstCollegeUuid} 与 {@code tblTeacher.tcCollegeUuid}
     * 都是非空外键，指向不存在的学院会被数据库直接拒掉（1452），而注册失败会导致整个开户回滚。
     */
    public static final String DEFAULT_COLLEGE_UUID = "00000000-0000-0000-0000-000000000c01";

    /** 学院池（学院 uuid 列表）；由 {@link #bootstrapColleges()} 写入。 */
    private static volatile List<String> s_collegePool = Collections.emptyList();

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
     * 全服只应有一份目录：本模块各处（选课、成绩、排课）必须读写同一份，再 new 一个就写到别的实例上， 界面上看不到。
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
     * 取成绩 DAO 单例，与 {@link #courseDao()} 配对。
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
     * 用调用方给定的课程目录与成绩 DAO 装配。
     *
     * <p>
     * 之所以要能注入：一切都得落在<b>同一份</b>课程目录上，若这里再 new 一个，请求处理的是另一个实例， 界面上就看不到 —— 与商店那次「第二个
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
            provisioning.add(new CourseProvisioner(dao, collegePool()));
        }
    }

    /**
     * 从引导文件建立学院池。
     *
     * <p>
     * 必须在登记开户钩子（{@link #register}）之前调用：钩子给学生/教师建档时要挂一个已经存在于库里的
     * 学院。入口在启动时按此顺序组装，测试仍可以不注入文件，这时池里只有缺省学院。
     *
     * <p>
     * 文件里一个可用学院都没有时只告警，不把服务端卡死；但那只意味着师生注册会撞外键，告警里已写明。
     *
     * @return 学院池（学院 uuid 列表），不会为空
     * @throws IOException 读取引导文件失败
     */
    public static List<String> bootstrapColleges() throws IOException {
        final String path = System.getProperty(CollegePoolBootstrap.FILE_PROPERTY,
                CollegePoolBootstrap.DEFAULT_FILE);
        List<String> pool = CollegePoolBootstrap.seed(courseDao(), new File(path));
        if (pool.isEmpty()) {
            System.err.println("学院池为空（" + path + " 里没有可用学院）：新建学生/教师档案会挂到 "
                    + DEFAULT_COLLEGE_UUID + "，该行不在库里时注册会因外键失败（1452）");
            pool = Collections.singletonList(DEFAULT_COLLEGE_UUID);
        }
        s_collegePool = pool;
        return pool;
    }

    /**
     * 供开户钩子使用的学院池；未调用过 {@link #bootstrapColleges()} 时只有缺省学院。
     *
     * @return 学院 uuid 列表，不会为空
     */
    private static List<String> collegePool() {
        List<String> pool = s_collegePool;
        return pool.isEmpty() ? Collections.singletonList(DEFAULT_COLLEGE_UUID) : pool;
    }
}
