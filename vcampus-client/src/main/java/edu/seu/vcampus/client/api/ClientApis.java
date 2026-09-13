package edu.seu.vcampus.client.api;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserModule;
import edu.seu.vcampus.client.user.UserService;

/**
 * 客户端各业务模块 API 的只读容器（见 ADR-0009 D8）。
 *
 * <p>
 * 由装配层 {@code VCampusClientApp.connect()} 一次性创建，沿装配链 （入口 → {@code LoginFlow} →
 * {@code MainFrame} → {@code MainContentPanel}）传递，
 * <b>只用于构造页面</b>：每个页面构造器只接收自己那一个 API（如 {@code LibraryPanel(LibraryService)}），
 * 容器本身不往页面里传，避免页面顺藤摸瓜访问别的模块。
 *
 * <p>
 * 当前只有用户管理模块具备客户端逻辑 API；学籍/选课/图书馆/商店/银行的 getter
 * 在其模块装配（{@code XxxModule.register}）落地时逐个补齐，不预先造空实现。
 */
public final class ClientApis {

    /** 用户管理 API。 */
    private final UserService m_user;

    private ClientApis(UserService user) {
        this.m_user = user;
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
        return new ClientApis(UserModule.register(dispatcher));
    }

    /** @return 用户管理 API */
    public UserService user() {
        return m_user;
    }
}
