package edu.seu.vcampus.common.shop;

import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ShopOrder 实体测试：字段读写与跨模块序列化一致性（见 ADR-0006）。
 */
class OrderTest {

    /**
     * 全参构造后各字段应可正确读出。
     */
    @Test
    void constructorSetsAllFields() {
        Date now = new Date();
        String userUuid = "550e8400-e29b-41d4-a716-446655440000";
        ShopOrder order = new ShopOrder("O001", userUuid, "S001", 2, new BigDecimal("119.80"), now, ShopOrderStatus.UNPAID);

        assertEquals("O001", order.getoId());
        assertEquals(userUuid, order.getoUserUuid());
        assertEquals("S001", order.getoItemId());
        assertEquals(Integer.valueOf(2), order.getoQuantity());
        assertEquals(new BigDecimal("119.80"), order.getoTotal());
        assertEquals(now, order.getoTime());
        assertEquals(ShopOrderStatus.UNPAID, order.getoStatus());
    }

    /**
     * setter 写入的值应可由 getter 读回。
     */
    @Test
    void settersRoundTrip() {
        Date now = new Date();
        ShopOrder order = new ShopOrder();
        order.setoId("O002");
        String userUuid = "660e8400-e29b-41d4-a716-446655440000";
        order.setoUserUuid(userUuid);
        order.setoItemId("S002");
        order.setoQuantity(3);
        order.setoTotal(new BigDecimal("37.50"));
        order.setoTime(now);
        order.setoStatus(ShopOrderStatus.PAID);

        assertEquals("O002", order.getoId());
        assertEquals(userUuid, order.getoUserUuid());
        assertEquals("S002", order.getoItemId());
        assertEquals(Integer.valueOf(3), order.getoQuantity());
        assertEquals(new BigDecimal("37.50"), order.getoTotal());
        assertEquals(now, order.getoTime());
        assertEquals(ShopOrderStatus.PAID, order.getoStatus());
    }

    /**
     * 序列化再反序列化后，字段应与原对象一致；金额精度不得丢失。
     *
     * @throws Exception 序列化/IO 异常
     */
    @Test
    void serializationRoundTrip() throws Exception {
        ShopOrder original = new ShopOrder("O003", "770e8400-e29b-41d4-a716-446655440000", "S003", 1, new BigDecimal("88.00"),
                new Date(), ShopOrderStatus.PAID);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(original);
        oos.flush();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        ShopOrder copy = (ShopOrder) ois.readObject();

        assertEquals(original.getoId(), copy.getoId());
        assertEquals(original.getoUserUuid(), copy.getoUserUuid());
        assertEquals(original.getoItemId(), copy.getoItemId());
        assertEquals(original.getoQuantity(), copy.getoQuantity());
        assertEquals(original.getoTotal(), copy.getoTotal());
        assertEquals(original.getoTime(), copy.getoTime());
        assertEquals(original.getoStatus(), copy.getoStatus());
    }
}
