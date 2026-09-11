package edu.seu.vcampus.client.view.shell;

/**
 * 接收字符串参数的 Java 7 兼容回调。
 */
public interface StringHandler {

    /**
     * 处理字符串参数。
     *
     * @param value 待处理的值
     */
    void handle(String value);
}
