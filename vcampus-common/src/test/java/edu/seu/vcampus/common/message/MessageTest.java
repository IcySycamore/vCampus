package edu.seu.vcampus.common.message;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Message 序列化往返测试：验证对象流收发前后字段一致（协议地基，见 ADR-0006）。
 */
class MessageTest {

    /**
     * 序列化再反序列化后，字段应与原对象一致。
     *
     * @throws Exception 序列化/IO 异常
     */
    @Test
    void serializationRoundTrip() throws Exception {
        Message original = new Message(101, "hello");
        original.setUid(100L);
        original.setStatusCode("200");
        original.setSender("001");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(original);
        oos.flush();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        Message copy = (Message) ois.readObject();

        assertEquals(original.getUid(), copy.getUid());
        assertEquals(original.getCommand(), copy.getCommand());
        assertEquals(original.getStatusCode(), copy.getStatusCode());
        assertEquals(original.getSender(), copy.getSender());
        assertEquals(original.getData(), copy.getData());
    }
}
