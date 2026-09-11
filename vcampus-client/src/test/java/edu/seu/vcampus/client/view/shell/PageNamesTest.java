package edu.seu.vcampus.client.view.shell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 页面标识集中定义测试。
 */
class PageNamesTest {

    @Test
    void exposesStablePageNames() {
        assertEquals("home", PageNames.HOME);
        assertEquals("library", PageNames.LIBRARY);
        assertEquals("bank", PageNames.BANK);
    }
}
