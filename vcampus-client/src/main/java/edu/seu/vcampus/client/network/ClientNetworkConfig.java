package edu.seu.vcampus.client.network;

/**
 * 客户端网络连接参数，包括超时、重试退避与优雅关闭宽限期。
 */
public final class ClientNetworkConfig {

    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final int DEFAULT_READ_TIMEOUT = 30000;
    private static final int DEFAULT_RETRIES = 3;
    private static final long DEFAULT_INITIAL_BACKOFF = 250L;
    private static final long DEFAULT_MAX_BACKOFF = 4000L;
    private static final long DEFAULT_SHUTDOWN_GRACE = 1000L;

    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final int maxRetries;
    private final long initialBackoffMillis;
    private final long maxBackoffMillis;
    private final long shutdownGraceMillis;

    /**
     * 创建网络参数。
     *
     * @param connectTimeoutMillis TCP 连接超时，毫秒
     * @param readTimeoutMillis 消息读取超时，毫秒
     * @param maxRetries 首次失败后最多重试次数，0 表示不重试
     * @param initialBackoffMillis 首次重试等待时间，毫秒
     * @param maxBackoffMillis 指数退避等待上限，毫秒
     * @param shutdownGraceMillis 优雅关闭等待在途消息的时间，毫秒
     */
    public ClientNetworkConfig(int connectTimeoutMillis, int readTimeoutMillis,
            int maxRetries, long initialBackoffMillis, long maxBackoffMillis,
            long shutdownGraceMillis) {
        if (connectTimeoutMillis <= 0 || readTimeoutMillis <= 0) {
            throw new IllegalArgumentException("timeouts must be positive");
        }
        if (maxRetries < 0 || initialBackoffMillis < 0
                || maxBackoffMillis < initialBackoffMillis || shutdownGraceMillis < 0) {
            throw new IllegalArgumentException("invalid retry or shutdown configuration");
        }
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.maxRetries = maxRetries;
        this.initialBackoffMillis = initialBackoffMillis;
        this.maxBackoffMillis = maxBackoffMillis;
        this.shutdownGraceMillis = shutdownGraceMillis;
    }

    /** @return 适合桌面客户端的默认参数 */
    public static ClientNetworkConfig defaults() {
        return new ClientNetworkConfig(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT,
                DEFAULT_RETRIES, DEFAULT_INITIAL_BACKOFF, DEFAULT_MAX_BACKOFF,
                DEFAULT_SHUTDOWN_GRACE);
    }

    /** @return TCP 连接超时，毫秒 */
    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    /** @return 消息读取超时，毫秒 */
    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    /** @return 首次失败后的最多重试次数 */
    public int getMaxRetries() {
        return maxRetries;
    }

    /** @return 首次重试等待时间，毫秒 */
    public long getInitialBackoffMillis() {
        return initialBackoffMillis;
    }

    /** @return 指数退避等待上限，毫秒 */
    public long getMaxBackoffMillis() {
        return maxBackoffMillis;
    }

    /** @return 优雅关闭宽限期，毫秒 */
    public long getShutdownGraceMillis() {
        return shutdownGraceMillis;
    }
}
