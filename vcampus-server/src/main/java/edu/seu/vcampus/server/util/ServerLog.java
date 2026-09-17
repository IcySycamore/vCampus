package edu.seu.vcampus.server.util;

/**
 * 服务端控制台日志：所有回显统一成 {@code [info]} / {@code [warning]} / {@code [error]} + 消息。
 *
 * <p>
 * 之前各模块直接 {@code System.out.println("[ShopMessageHandler] ...")}，前缀五花八门，抓日志时
 * 得先认模块名再判断严重程度。统一成三个级别后，可以直接按前缀过滤（例如只看 {@code [error]}）。
 *
 * <p>
 * 级别约定：{@code info} 是正常流程（启动、连接、认证、导入）；{@code warning} 是能继续跑但要留意的事 （跳过了一条脏数据、客户端静默超时）；{@code error}
 * 是本次操作失败或连接异常退出。
 */
public final class ServerLog {

    /** 私有构造器，禁止实例化日志工具。 */
    private ServerLog() {
    }

    /**
     * 正常流程信息。
     *
     * @param message 消息
     */
    public static void info(String message) {
        System.out.println("[info]" + message);
    }

    /**
     * 警示信息：不影响继续运行，但需要留意。
     *
     * @param message 消息
     */
    public static void warning(String message) {
        System.out.println("[warning]" + message);
    }

    /**
     * 错误信息。
     *
     * @param message 消息
     */
    public static void error(String message) {
        System.err.println("[error]" + message);
    }

    /**
     * 错误信息（带异常）。
     *
     * @param message 消息
     * @param cause   异常；可为 null
     */
    public static void error(String message, Throwable cause) {
        if (cause == null) {
            error(message);
            return;
        }
        System.err.println("[error]" + message + "（" + cause.getClass().getSimpleName() + ": "
                + cause.getMessage() + "）");
    }
}
