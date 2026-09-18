package edu.seu.vcampus.server.shop;

/** 商店支付失败，携带可直接返回客户端的业务状态码。 */
final class ShopPaymentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String statusCode;

    ShopPaymentException(String statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    String getStatusCode() {
        return statusCode;
    }
}
