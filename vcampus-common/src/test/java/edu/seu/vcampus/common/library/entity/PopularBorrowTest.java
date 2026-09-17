package edu.seu.vcampus.common.library.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** 热门借阅排行消息实体测试。 */
class PopularBorrowTest {
    @Test
    void survivesMessageSerialization() throws Exception {
        PopularBorrow item = new PopularBorrow("978-7", "Java", 9);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new ObjectOutputStream(bytes).writeObject(item);
        PopularBorrow copy = (PopularBorrow) new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray())).readObject();
        assertEquals("978-7", copy.getIsbn());
        assertEquals("Java", copy.getTitle());
        assertEquals(9, copy.getBorrowCount());
    }
}
