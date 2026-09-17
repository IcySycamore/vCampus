package edu.seu.vcampus.common.library.entity;

/** 图书预约状态。 */
public enum ReservationStatus {
    /** 等待图书归还。 */
    WAITING,
    /** 图书已到馆并进入保留期。 */
    READY,
    /** 已借阅预约图书。 */
    FULFILLED,
    /** 用户主动取消。 */
    CANCELLED,
    /** 到馆后超过保留期。 */
    EXPIRED
}
