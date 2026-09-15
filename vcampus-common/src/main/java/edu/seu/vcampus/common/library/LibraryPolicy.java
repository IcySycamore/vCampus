package edu.seu.vcampus.common.library;

import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;
import java.math.BigDecimal;
import java.util.Date;

/** 图书馆双端共用的角色解析、借阅额度与馆藏权限。 */
public final class LibraryPolicy {
    /** 在校师生最多同时借阅册数。 */
    public static final int BORROW_LIMIT = 30;
    /** 首次借阅天数。 */
    public static final int LOAN_DAYS = 30;
    /** 最多续借次数。 */
    public static final int MAX_RENEWALS = 2;
    /** 每次续借天数。 */
    public static final int RENEWAL_DAYS = 30;
    /** 预约到馆后的保留天数。 */
    public static final int RESERVATION_HOLD_DAYS = 15;
    /** 每册每天的逾期滞纳金。 */
    public static final BigDecimal DAILY_OVERDUE_FINE = new BigDecimal("0.10");
    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private LibraryPolicy() {
    }

    /**
     * 解析中文显示名或英文枚举名，未知角色返回 null。
     * @param value 会话中的角色
     * @return 标准角色
     */
    public static Role role(String value) {
        if (value != null) {
            for (Role role : Role.values()) {
                if (role.getDisplayName().equals(value.trim())
                        || role.name().equalsIgnoreCase(value.trim())) {
                    return role;
                }
            }
        }
        return null;
    }

    /**
     * 返回未归还上限；未知或无借阅权限的角色为零。
     * @param value 会话角色
     * @return 借阅上限
     */
    public static int borrowLimit(String value) {
        Role role = role(value);
        if (!Permissions.can(role, Capability.LIBRARY_BORROW)) {
            return 0;
        }
        return role == Role.STUDENT || role == Role.TEACHER ? BORROW_LIMIT : 0;
    }

    /**
     * 计算截止指定时刻已经产生的逾期滞纳金，不足一天按一天计算。
     * @param dueAt 应还时间
     * @param at 计费截止时间
     * @return 保留两位小数的非负金额
     */
    public static BigDecimal overdueFine(Date dueAt, Date at) {
        if (dueAt == null || at == null || !at.after(dueAt)) {
            return BigDecimal.ZERO.setScale(2);
        }
        long overdueMillis = at.getTime() - dueAt.getTime();
        long days = (overdueMillis + DAY_MILLIS - 1L) / DAY_MILLIS;
        return DAILY_OVERDUE_FINE.multiply(BigDecimal.valueOf(days)).setScale(2);
    }

    /**
     * 判断是否有馆藏维护权限。
     * @param value 会话角色
     * @return 是否允许
     */
    public static boolean canManage(String value) {
        return Permissions.can(role(value), Capability.LIBRARY_MANAGE);
    }
}
