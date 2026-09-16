package edu.seu.vcampus.client.student;

import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;

/**
 * 学籍命令的发送与响应校验：把「发一条命令、超时或被拒怎么报错」收在一处。
 *
 * <p>
 * 与 {@link StudentService}（业务方法）分开，一是业务类才能待在 200 行以内，二是错误处理策略
 * 只需要一份：状态码 → {@link ApiException} 的映射改这里就全局生效，不必在九个方法里各写一遍。
 *
 * <p>
 * token 由调用方按请求传入而不是在构造时捕获，这样登录/登出到会话变化时不需要重建本对象。
 */
final class StudentApiClient {

    /** 消息分发器（发送 + 按命令码等待响应）。 */
    private final ClientMessageDispatcher m_dispatcher;

    /** 请求超时，毫秒。 */
    private final long m_timeout_millis;

    /**
     * 构造发送器。
     *
     * @param dispatcher 消息分发器
     * @param timeoutMillis 请求超时，毫秒
     */
    StudentApiClient(ClientMessageDispatcher dispatcher, long timeoutMillis) {
        this.m_dispatcher = dispatcher;
        this.m_timeout_millis = timeoutMillis;
    }

    /**
     * 发送请求并校验响应状态，失败统一抛 {@link ApiException}。
     *
     * @param command 命令码
     * @param data 请求数据（可为 null）
     * @param token 会话令牌（可为 null，未登录时服务端回 401）
     * @return 成功响应
     * @throws ApiException 网络失败、线程被中断、超时或服务端拒绝
     */
    Message call(int command, Object data, String token) {
        Message message = new Message(command, data);
        message.setToken(token);
        Message response;
        try {
            response = m_dispatcher.request(message, m_timeout_millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(ApiErrors.LOCAL_INTERRUPTED);
        }
        if (response == null) {
            throw new ApiException(ApiErrors.LOCAL_TIMEOUT);
        }
        String code = response.getStatusCode();
        if (!StatusCode.SUCCESS.equals(code)) {
            throw new ApiException(code, describe(response));
        }
        return response;
    }

    /**
     * 拼接服务端返回的错误说明。
     *
     * @param response 失败响应
     * @return 可读的错误说明
     */
    private static String describe(Message response) {
        Object data = response.getData();
        if (data instanceof String && ((String) data).length() > 0) {
            return (String) data;
        }
        return "服务器拒绝：" + response.getStatusCode();
    }

    /**
     * 把界面传来的申请单编号解析成主键。
     *
     * @param requestId 编号文本
     * @return 主键
     * @throws ApiException 编号为空或不是数字
     */
    static Long parseId(String requestId) {
        if (requestId == null || requestId.trim().length() == 0) {
            throw new ApiException(null, "申请单编号不能为空");
        }
        try {
            return Long.valueOf(requestId.trim());
        } catch (NumberFormatException exception) {
            throw new ApiException(null, "申请单编号必须是数字：" + requestId);
        }
    }
}
