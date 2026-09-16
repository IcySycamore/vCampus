package edu.seu.vcampus.client.view.shell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Java 7 字符串回调测试。
 */
class StringHandlerTest {

    @Test
    void handlesStringValue() {
        final String[] result = new String[1];
        StringHandler handler = new StringHandler() {
            @Override
            public void handle(String value) {
                result[0] = value;
            }
        };

        handler.handle("library");

        assertEquals("library", result[0]);
    }
}
