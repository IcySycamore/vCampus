package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.user.entity.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AccountProvisioning 测试：开户广播、失败回滚、撤销容错。
 */
class AccountProvisioningTest {

    /** 按登记顺序广播给全部钩子。 */
    @Test
    void provisionsInRegistrationOrder() {
        final List<String> calls = new ArrayList<String>();
        AccountProvisioning provisioning = new AccountProvisioning();
        provisioning.add(recorder("first", calls, null));
        provisioning.add(recorder("second", calls, null));

        provisioning.provision("uuid-1", "张三", Role.STUDENT);

        assertEquals(2, calls.size());
        assertTrue(calls.get(0).startsWith("first"));
        assertTrue(calls.get(1).startsWith("second"));
        assertEquals(2, provisioning.size());
    }

    /** 中途失败：回滚已成功的钩子，并把异常抛给调用方。 */
    @Test
    void rollsBackOnFailure() {
        final List<String> calls = new ArrayList<String>();
        final AccountProvisioning provisioning = new AccountProvisioning();
        provisioning.add(recorder("ok", calls, null));
        provisioning.add(recorder("boom", calls, new IllegalStateException("建档案失败")));
        provisioning.add(recorder("never", calls, null));

        assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() {
                provisioning.provision("uuid-1", "张三", Role.STUDENT);
            }
        });

        assertTrue(calls.contains("boom:provision:uuid-1:STUDENT"));
        assertTrue(calls.contains("ok:revoke:uuid-1"), "已成功的模块必须被回滚");
        assertFalseContains(calls, "never");
    }

    /** 撤销时单个钩子失败不影响其它钩子（只记录，不中断）。 */
    @Test
    void revokeIsFaultTolerant() {
        final List<String> calls = new ArrayList<String>();
        AccountProvisioning provisioning = new AccountProvisioning();
        provisioning.add(new AccountProvisioner() {
            @Override
            public void provision(String userUuid, String userName, Role role) {
                // 本用例只验证撤销路径
            }

            @Override
            public void revoke(String userUuid) {
                throw new IllegalStateException("撤销失败");
            }
        });
        provisioning.add(recorder("ok", calls, null));

        provisioning.revoke("uuid-1");

        assertTrue(calls.contains("ok:revoke:uuid-1"));
    }

    /** 钩子为 null 拒绝登记。 */
    @Test
    void rejectsNullProvisioner() {
        final AccountProvisioning provisioning = new AccountProvisioning();
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                provisioning.add(null);
            }
        });
    }

    private void assertFalseContains(List<String> calls, String prefix) {
        for (String call : calls) {
            if (call.startsWith(prefix)) {
                throw new AssertionError("不应调用: " + call);
            }
        }
    }

    private AccountProvisioner recorder(final String name, final List<String> calls,
            final RuntimeException provisionFailure) {
        return new AccountProvisioner() {
            @Override
            public void provision(String userUuid, String userName, Role role) {
                calls.add(name + ":provision:" + userUuid + ":" + role);
                if (provisionFailure != null) {
                    throw provisionFailure;
                }
            }

            @Override
            public void revoke(String userUuid) {
                calls.add(name + ":revoke:" + userUuid);
            }
        };
    }
}
