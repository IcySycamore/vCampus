package edu.seu.vcampus.common.message;

import java.io.Serializable;

/**
 * 客户端与服务器端之间传输的统一消息信封。
 *
 * <p>所有传输对象必须实现 {@link java.io.Serializable}，且两端共享本类以保证序列化
 * 一致性（见 ADR-0006）。命令码与状态码后续以常量统一维护。
 */
public class Message implements Serializable {

    /** 序列化版本号（协议兼容依据，协议变更时谨慎修改）。 */
    private static final long serialVersionUID = 1L;

    /** 消息唯一标识。 */
    private Long uid;

    /** 消息名称/命令名。 */
    private String name;

    /** 消息类型：命令或数据。 */
    private String type;

    /** 状态码。 */
    private String statusCode;

    /** 传输数据（可为任意可序列化对象）。 */
    private Object data;

    /** 发送者用户名。 */
    private String sender;

    /**
     * 构造一个空消息。
     */
    public Message() {
    }

    /**
     * 构造一个带命令名与数据载荷的消息。
     *
     * @param name 命令名
     * @param data 数据载荷
     */
    public Message(String name, Object data) {
        this.name = name;
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

    /** @return 消息名称/命令名 */
    public String getName() {
        return name;
    }

    /** @param name 消息名称/命令名 */
    public void setName(String name) {
        this.name = name;
    }

    /** @return 消息类型 */
    public String getType() {
        return type;
    }

    /** @param type 消息类型 */
    public void setType(String type) {
        this.type = type;
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
}
