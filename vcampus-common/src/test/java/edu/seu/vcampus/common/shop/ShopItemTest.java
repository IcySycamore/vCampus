package edu.seu.vcampus.common.shop;

import edu.seu.vcampus.common.shop.entity.ShopItem;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ShopItem 实体测试：字段读写与跨模块序列化一致性（见 ADR-0006）。
 */
class ShopItemTest {

    /**
     * 全参构造后各字段应可正确读出。
     */
    @Test
    void constructorSetsAllFields() {
        ShopItem item = new ShopItem("S001", "校园文化衫", new BigDecimal("59.90"), 100, "纯棉短袖", "SHOP001");

        assertEquals("S001", item.getSiId());
        assertEquals("校园文化衫", item.getSiName());
        assertEquals(new BigDecimal("59.90"), item.getSiPrice());
        assertEquals(Integer.valueOf(100), item.getSiStock());
        assertEquals("纯棉短袖", item.getSiDesc());
        assertEquals("SHOP001", item.getSiShopId());
    }

    /**
     * setter 写入的值应可由 getter 读回。
     */
    @Test
    void settersRoundTrip() {
        ShopItem item = new ShopItem();
        item.setSiId("S002");
        item.setSiName("笔记本");
        item.setSiPrice(new BigDecimal("12.50"));
        item.setSiStock(30);
        item.setSiDesc("A5 横线本");
        item.setSiShopId("SHOP001");

        assertEquals("S002", item.getSiId());
        assertEquals("笔记本", item.getSiName());
        assertEquals(new BigDecimal("12.50"), item.getSiPrice());
        assertEquals(Integer.valueOf(30), item.getSiStock());
        assertEquals("A5 横线本", item.getSiDesc());
        assertEquals("SHOP001", item.getSiShopId());
    }

    /**
     * 序列化再反序列化后，字段应与原对象一致；金额精度不得丢失。
     *
     * @throws Exception 序列化/IO 异常
     */
    @Test
    void serializationRoundTrip() throws Exception {
        ShopItem original = new ShopItem("S003", "保温杯", new BigDecimal("88.00"), 20, "500ml", "SHOP001");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(original);
        oos.flush();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        ShopItem copy = (ShopItem) ois.readObject();

        assertEquals(original.getSiId(), copy.getSiId());
        assertEquals(original.getSiName(), copy.getSiName());
        assertEquals(original.getSiPrice(), copy.getSiPrice());
        assertEquals(original.getSiStock(), copy.getSiStock());
        assertEquals(original.getSiDesc(), copy.getSiDesc());
        assertEquals(original.getSiShopId(), copy.getSiShopId());
    }
}
