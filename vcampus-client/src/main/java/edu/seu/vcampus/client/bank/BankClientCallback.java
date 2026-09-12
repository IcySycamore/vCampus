package edu.seu.vcampus.client.bank;

/**
 * 银行客户端请求结果回调。
 *
 * @param <T> 成功结果类型
 */
public interface BankClientCallback<T> {

    /**
     * 请求成功时回调。
     *
     * @param result 服务端返回的业务结果
     */
    void onSuccess(T result);

    /**
     * 请求失败时回调。
     *
     * @param statusCode 服务端状态码
     * @param data 服务端返回的错误数据，可能为空
     */
    void onFailure(String statusCode, Object data);
}
