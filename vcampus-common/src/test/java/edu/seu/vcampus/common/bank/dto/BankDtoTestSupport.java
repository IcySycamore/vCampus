package edu.seu.vcampus.common.bank.dto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/** 银行 DTO 测试共用的对象流往返工具。 */
final class BankDtoTestSupport {

    private BankDtoTestSupport() {
    }

    @SuppressWarnings("unchecked")
    static <T> T roundTrip(T value) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(bytes);
        output.writeObject(value);
        output.close();
        ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()));
        Object result = input.readObject();
        input.close();
        return (T) result;
    }
}
