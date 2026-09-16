package edu.seu.vcampus.common.constant;

/**
 * 协议载荷上限（见 ADR-0010 D1：单条消息必须有上限）。
 *
 * <p>
 * 列表类查询一律走 {@link edu.seu.vcampus.common.message.PageResponse} 分页（单页上限
 * {@code MAX_PAGE_SIZE}）；批量命令则受本类约束。<b>双端共用同一常量</b>：客户端按它切片， 服务端按它校验，因此不可能出现「客户端切了、服务端不认」的不一致。
 *
 * <p>
 * 上限的意义：单条消息必须在毫秒级处理完，读循环才不会被业务长期占用 → 心跳才能按时回 ACK。 超限不是「截断」，而是明确拒绝（服务端回 400），由客户端负责分片与进度提示。
 */
public final class ProtocolLimit {

    /** 单条批量命令允许携带的最大条目数（103 批量注册 / 105 批量注销）。 */
    public static final int MAX_BATCH_SIZE = 50;

    /**
     * 私有构造器，禁止实例化常量类。
     */
    private ProtocolLimit() {
    }
}
