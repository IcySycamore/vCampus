package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 批量操作结果（命令 103 批量注册、105 批量注销的响应载荷）。
 *
 * <p>
 * 批量操作刻意<b>不做「全成功或全回滚」</b>：从文件导入几十个账号时，某一个重名就让整批失败
 * 是无法使用的。因此逐条执行、逐条记账，把失败原因原样带回给界面展示。
 */
public final class BatchResult implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 成功条数。 */
    private final int m_success_count;

    /** 失败明细。 */
    private final List<Failure> m_failures;

    /**
     * 构造批量结果。
     *
     * @param successCount 成功条数
     * @param failures 失败明细；null 视为无失败
     */
    public BatchResult(int successCount, List<Failure> failures) {
        this.m_success_count = successCount;
        this.m_failures = failures == null ? new ArrayList<Failure>() : new ArrayList<Failure>(
                failures);
    }

    /** @return 成功条数 */
    public int getSuccessCount() {
        return m_success_count;
    }

    /** @return 失败明细（只读） */
    public List<Failure> getFailures() {
        return Collections.unmodifiableList(m_failures);
    }

    /** @return 失败条数 */
    public int getFailureCount() {
        return m_failures.size();
    }

    /** @return 是否全部成功 */
    public boolean isAllSucceeded() {
        return m_failures.isEmpty();
    }

    /** @return 面向界面的摘要文本 */
    public String summary() {
        if (isAllSucceeded()) {
            return "全部成功，共 " + m_success_count + " 条";
        }
        return "成功 " + m_success_count + " 条，失败 " + m_failures.size() + " 条";
    }

    /**
     * 单条失败明细。
     */
    public static final class Failure implements Serializable {

        /** 序列化版本号。 */
        private static final long serialVersionUID = 1L;

        /** 登录名。 */
        private final String m_user_name;

        /** 失败原因（服务器给出的说明）。 */
        private final String m_reason;

        /**
         * 构造失败明细。
         *
         * @param userName 登录名
         * @param reason 失败原因
         */
        public Failure(String userName, String reason) {
            this.m_user_name = userName;
            this.m_reason = reason;
        }

        /** @return 登录名 */
        public String getUserName() {
            return m_user_name;
        }

        /** @return 失败原因 */
        public String getReason() {
            return m_reason;
        }

        @Override
        public String toString() {
            return m_user_name + "：" + m_reason;
        }
    }
}
