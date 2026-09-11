package edu.seu.vcampus.common.constant;

/**
 * 网络通信统一常量（客户端与服务器端共用，见 ADR-0006）。
 *
 * <p>
 * 主机、端口等连接参数集中在此维护，避免两端各写一份导致不一致。
 */
public final class NetworkConstant {

    /** 默认服务器主机地址（本地演示）。 */
    public static final String DEFAULT_HOST = "127.0.0.1";

    /** 默认服务器监听端口（协议 v1 约定）。 */
    public static final int DEFAULT_PORT = 8888;

    /** 默认请求-响应等待超时（毫秒）。 */
    public static final long DEFAULT_REQUEST_TIMEOUT_MILLIS = 10000L;

    /** 私有构造器，禁止实例化常量类。 */
    private NetworkConstant() {
    }
}
