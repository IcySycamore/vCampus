package edu.seu.vcampus.common.library.entity;

import java.io.Serializable;

/** 图书馆读者账户状态。 */
public enum LibraryAccountStatus implements Serializable {
    /** 可以借阅、续借和预约。 */
    NORMAL,
    /** 暂停新增借阅、续借和预约，仍可归还与缴费。 */
    SUSPENDED
}
