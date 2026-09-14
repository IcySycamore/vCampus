package edu.seu.vcampus.common.bank.exception;

/** 未开户业务异常，供服务端抛出并通过 Message.data 传递给客户端。 */
public final class BankAccountNotOpenedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** 创建未开户异常；作为业务响应传输，不携带服务端调用栈。 */
    public BankAccountNotOpenedException() {
        super("银行账户未开户，请先开户", null, false, false);
    }
}
