package edu.seu.vcampus.common.message;

import java.io.Serializable;

/**
 * 客户端与服务器端之间传输的统一消息信封（见 docs/应用层协议规定.md）。
 *
 * <p>所有传输对象必须实现 {@link java.io.Serializable}，两端共享本类以保证序列化一致性。
 */
public class Message implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 2L;

    /** 消息唯一标识。 */
    private Long uid;

    /** 命令码（标识要执行的操作）。 */
    private int command;

    /** 状态码。 */
    private String statusCode;

    /** 传输数据（可为任意可序列化对象）。 */
    private Object data;

    /** 发送者用户名。 */
    private String sender;

    /** 会话令牌（登录成功后由服务器签发，之后每条请求都携带；登录/注册阶段为空）。 */
    private String token;

    /**
     * 构造一个空消息。
     */
    public Message() {
    }

    /**
     * 构造一个带命令码与数据载荷的消息。
     *
     * @param command 命令码
     * @param data    数据载荷
     */
    public Message(int command, Object data) {
        this.command = command;
        this.data = data;
    }

    /** @return 消息唯一标识 */
    public Long getUid() {
        return uid;
    }

    /** @param uid 消息唯一标识 */
    public void setUid(Long uid) {
        this.uid = uid;
    }

    /** @return 命令码 */
    public int getCommand() {
        return command;
    }

    /** @param command 命令码 */
    public void setCommand(int command) {
        this.command = command;
    }

    /** @return 状态码 */
    public String getStatusCode() {
        return statusCode;
    }

    /** @param statusCode 状态码 */
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    /** @return 传输数据 */
    public Object getData() {
        return data;
    }

    /** @param data 传输数据 */
    public void setData(Object data) {
        this.data = data;
    }

    /** @return 发送者用户名 */
    public String getSender() {
        return sender;
    }

    /** @param sender 发送者用户名 */
    public void setSender(String sender) {
        this.sender = sender;
    }

    /** @return 会话令牌 */
    public String getToken() {
        return token;
    }

    /** @param token 会话令牌 */
    public void setToken(String token) {
        this.token = token;
    }
}
