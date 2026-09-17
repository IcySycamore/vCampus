package edu.seu.vcampus.client.view.library;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证首页规则公告区。 */
class LibraryRulesNoticePanelTest {
    @Test
    void showsRuleCardBesideRanking() {
        LibraryRulesNoticePanel panel = new LibraryRulesNoticePanel();
        assertEquals("libraryHomeRules", panel.getName());
        assertTrue(panel.getComponentCount() >= 2);
    }
}
