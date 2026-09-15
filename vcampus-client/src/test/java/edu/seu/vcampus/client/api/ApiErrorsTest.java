package edu.seu.vcampus.client.api;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ApiErrors 文案映射与 ApiException 语义测试。
 */
class ApiErrorsTest {

    /** 常用服务端状态码都有中文文案。 */
    @Test
    void mapsServerCodes() {
        assertEquals("请求不合法，或数据已存在", ApiErrors.messageFor(StatusCode.BAD_REQUEST));
        assertEquals("登录状态已失效，请重新登录", ApiErrors.messageFor(StatusCode.UNAUTHORIZED));
        assertEquals("当前身份没有该操作权限", ApiErrors.messageFor(StatusCode.FORBIDDEN));
        assertEquals("原密码不正确", ApiErrors.messageFor(StatusCode.WRONG_PASSWORD));
        assertEquals("该账号已被禁用，请联系管理员", ApiErrors.messageFor(StatusCode.USER_DISABLED));
        assertEquals("请先开通校园银行账户", ApiErrors.messageFor(Command.BANK_ACCOUNT_NOT_OPENED));
    }

    /** 本地码有独立文案，不与服务端码冲突。 */
    @Test
    void mapsLocalCodes() {
        assertTrue(ApiErrors.messageFor(ApiErrors.LOCAL_NETWORK).contains("断开"));
        assertTrue(ApiErrors.messageFor(ApiErrors.LOCAL_TIMEOUT).contains("超时"));
        assertTrue(ApiErrors.messageFor(ApiErrors.LOCAL_INTERRUPTED).contains("取消"));
        assertTrue(ApiErrors.messageFor(ApiErrors.LOCAL_MALFORMED).contains("格式"));
    }

    /** 未知码与 null 都有兜底文案，且未知码带上原码便于排查。 */
    @Test
    void fallsBackForUnknownCodes() {
        assertTrue(ApiErrors.messageFor("X999").contains("X999"));
        assertEquals("操作失败，请稍后重试", ApiErrors.messageFor(null));
    }

    /** ApiException 自动取文案并区分本地/服务端失败。 */
    @Test
    void apiExceptionCarriesCodeAndText() {
        ApiException forbidden = new ApiException(StatusCode.FORBIDDEN);
        assertEquals(StatusCode.FORBIDDEN, forbidden.getStatusCode());
        assertEquals(ApiErrors.messageFor(StatusCode.FORBIDDEN), forbidden.getMessage());
        assertFalse(forbidden.isLocal());

        ApiException local = new ApiException(ApiErrors.LOCAL_TIMEOUT);
        assertTrue(local.isLocal());

        ApiException unknown = new ApiException(null);
        assertNull(unknown.getStatusCode());
        assertFalse(unknown.isLocal());
    }
}
