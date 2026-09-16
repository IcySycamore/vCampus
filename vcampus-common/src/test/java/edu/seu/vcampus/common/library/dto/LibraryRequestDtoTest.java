package edu.seu.vcampus.common.library.dto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** 图书馆请求 DTO 的 Java 序列化契约测试。 */
class LibraryRequestDtoTest {
    @Test
    void requestReferencesSurviveNetworkSerialization() throws Exception {
        BorrowRequest borrow = roundTrip(new BorrowRequest("9787302423287"));
        RecordRef record = roundTrip(new RecordRef(Long.MAX_VALUE));
        BookRef book = roundTrip(new BookRef("0321356683"));

        assertEquals("9787302423287", borrow.getIsbn());
        assertEquals(Long.MAX_VALUE, record.getRecordId());
        assertEquals("0321356683", book.getIsbn());
    }

    @SuppressWarnings("unchecked")
    private <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(bytes);
        output.writeObject(value);
        output.close();
        ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()));
        T result = (T) input.readObject();
        input.close();
        return result;
    }
}
