package edu.seu.vcampus.client.api;

import edu.seu.vcampus.client.bank.BankModule;
import edu.seu.vcampus.client.bank.BankService;
import edu.seu.vcampus.client.handler.ConnectionListener;
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
 * 由装配层 {@code VCampusClientApp.connect()} 一次性创建，沿装配链 （入口 → {@code LoginFlow} → {@code MainFrame} →
 * {@code MainContentPanel}）传递， <b>只用于构造页面</b>：每个页面构造器只接收自己那一个 API（如
 * {@code LibraryPanel(LibraryService)}）， 容器本身不往页面里传，避免页面顺藤摸瓜访问别的模块。
 *
 * <p>
 * 当前用户管理、学籍和银行模块具备客户端逻辑 API；选课/图书馆/商店的 getter
 * 在其模块装配（{@code XxxModule.register}）落地时逐个补齐，不预先造空实现。
 */
public final class ClientApis {

    /** 消息分发器：仅供本类登记连接事件使用，不向外暴露。 */
    private final ClientMessageDispatcher m_dispatcher;

    /** 用户管理 API。 */
    private final UserService m_user;

    /** 学籍 API。 */
    private final StudentService m_student;

    /** 银行 API。 */
    private final BankService m_bank;

    private ClientApis(ClientMessageDispatcher dispatcher, UserService user,
            StudentService student, BankService bank) {
        this.m_dispatcher = dispatcher;
        this.m_user = user;
        this.m_student = student;
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
        return new ClientApis(dispatcher, user, student, BankModule.register(dispatcher, user));
    }

    /**
     * 登记连接事件监听（断线时回调）。
     *
     * <p>
     * 这是容器唯一对外暴露的「非业务」能力：断线处理必须只有一处（主窗口），否则每个页面 各弹一个「连接已断开」窗口。
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
     * <p>
     * 与 {@link #user()} 共享同一份内存会话，因此界面不必关心 token 从哪来。
     *
     * @return 管理轨 API
     */
    public UserAdminService userAdmin() {
        return m_user.admin();
    }

    /** @return 银行 API */
    public BankService bank() {
        return m_bank;
    }

    /** @return 学籍 API */
    public StudentService student() {
        return m_student;
    }
}
