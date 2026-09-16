package edu.seu.vcampus.client.api;

import edu.seu.vcampus.client.bank.BankModule;
import edu.seu.vcampus.client.bank.BankService;
import edu.seu.vcampus.client.course.CourseModule;
import edu.seu.vcampus.client.course.CourseService;
import edu.seu.vcampus.client.handler.ConnectionListener;
import edu.seu.vcampus.client.library.LibraryModule;
import edu.seu.vcampus.client.library.LibraryService;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.student.StudentModule;
import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.user.UserAdminService;
import edu.seu.vcampus.client.user.UserModule;
import edu.seu.vcampus.client.user.UserService;

/**
 * 客户端各业务模块 API 的只读容器（见 ADR-0009 D8）。
 *
 * <p>
 * 由装配层 {@code VCampusClientApp.connect()} 一次性创建，沿装配链 （入口 → {@code LoginFlow} →
 * {@code MainFrame} → {@code MainContentPanel}）传递，<b>只用于构造页面</b>：
 * 每个页面构造器只接收自己那一个 API，容器本身不往页面里传。
 *
 * <p>
 * 当前用户管理、学籍、选课、图书馆和银行模块具备客户端逻辑 API；商店的 getter
 * 在其模块装配落地时补齐。
 */
public final class ClientApis {

    /** 消息分发器：仅供本类登记连接事件使用，不向外暴露。 */
    private final ClientMessageDispatcher m_dispatcher;

    /** 用户管理 API。 */
    private final UserService m_user;

    /** 学籍 API。 */
    private final StudentService m_student;

    /** 选课 API。 */
    private final CourseService m_course;

    /** 图书馆 API。 */
    private final LibraryService m_library;

    /** 银行 API。 */
    private final BankService m_bank;

    private ClientApis(ClientMessageDispatcher dispatcher, UserService user,
            StudentService student, CourseService course, LibraryService library,
            BankService bank) {
        this.m_dispatcher = dispatcher;
        this.m_user = user;
        this.m_student = student;
        this.m_course = course;
        this.m_library = library;
        this.m_bank = bank;
    }

    /**
     * 装配各模块客户端 API（各模块自带接线规则，入口只做汇总）。
     *
     * @param dispatcher 客户端消息分发器
     * @return API 容器
     * @throws IllegalArgumentException 分发器为 null
     */
    public static ClientApis create(ClientMessageDispatcher dispatcher) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        UserService user = UserModule.register(dispatcher);
        StudentService student = StudentModule.register(dispatcher, user);
        CourseService course = CourseModule.register(dispatcher, user);
        LibraryService library = LibraryModule.register(dispatcher, user);
        BankService bank = BankModule.register(dispatcher, user);
        return new ClientApis(dispatcher, user, student, course, library, bank);
    }

    /**
     * 登记连接事件监听（断线时回调）。
     *
     * @param listener 连接事件监听器
     * @throws IllegalArgumentException 监听器为 null
     */
    public void addConnectionListener(ConnectionListener listener) {
        m_dispatcher.addConnectionListener(listener);
    }

    /** @return 用户管理 API（我的轨） */
    public UserService user() {
        return m_user;
    }

    /**
     * 用户管理 API（管理轨，需 {@code USER_MANAGE}）。
     *
     * @return 管理轨 API
     */
    public UserAdminService userAdmin() {
        return m_user.admin();
    }

    /** @return 学籍 API */
    public StudentService student() {
        return m_student;
    }

    /** @return 选课 API */
    public CourseService course() {
        return m_course;
    }

    /** @return 图书馆 API；共享用户模块现有会话 */
    public LibraryService library() {
        return m_library;
    }

    /** @return 银行 API */
    public BankService bank() {
        return m_bank;
    }
}
