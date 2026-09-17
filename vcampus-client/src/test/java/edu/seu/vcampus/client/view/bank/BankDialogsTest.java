package edu.seu.vcampus.client.view.bank;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 银行弹窗的居中计算：落在可用区域内、尊重区域原点、窗口过大时不留负坐标。 */
class BankDialogsTest {

    /** 窗口小于可用区域时居中于其中。 */
    @Test
    void centersWithinAvailableArea() {
        Point location = BankDialogs.centeredLocation(new Dimension(400, 200),
                new Rectangle(0, 0, 1000, 800));
        assertEquals(300, location.x);
        assertEquals(300, location.y);
    }

    /** 可用区域原点非零（任务栏占位）时结果随之偏移。 */
    @Test
    void honorsAreaOrigin() {
        Point location = BankDialogs.centeredLocation(new Dimension(400, 200),
                new Rectangle(0, 40, 1000, 700));
        assertEquals(300, location.x);
        assertEquals(290, location.y);
    }

    /** 窗口大于可用区域时贴住区域原点，不产生负坐标。 */
    @Test
    void clampsWhenWindowExceedsArea() {
        Point location = BankDialogs.centeredLocation(new Dimension(2000, 1500),
                new Rectangle(10, 20, 1000, 800));
        assertEquals(10, location.x);
        assertEquals(20, location.y);
    }
}
