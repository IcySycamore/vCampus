package edu.seu.vcampus.client.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 批量载荷切片测试（ADR-0010 D1：必须与 ProtocolLimit.MAX_BATCH_SIZE 一致）。
 */
class BatchSplitterTest {

    @Test
    void splitsIntoChunksOfMaxSize() {
        List<Integer> items = new ArrayList<Integer>();
        for (int index = 0; index < 1000; index++) {
            items.add(index);
        }

        List<List<Integer>> chunks = BatchSplitter.split(items, 50);

        assertEquals(20, chunks.size());
        for (List<Integer> chunk : chunks) {
            assertEquals(50, chunk.size());
        }
    }

    @Test
    void keepsOrderAndLastPartialChunk() {
        List<String> chunks = new ArrayList<String>();
        List<List<String>> result = BatchSplitter.split(Arrays.asList("a", "b", "c", "d", "e"), 2);

        for (List<String> chunk : result) {
            chunks.addAll(chunk);
        }

        assertEquals(3, result.size());
        assertEquals(1, result.get(2).size());
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), chunks);
    }

    @Test
    void returnsEmptyForNullOrEmptyInput() {
        assertTrue(BatchSplitter.split(null, 50).isEmpty());
        assertTrue(BatchSplitter.split(new ArrayList<String>(), 50).isEmpty());
    }

    @Test
    void rejectsNonPositiveMaxSize() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                BatchSplitter.split(Arrays.asList("a"), 0);
            }
        });
    }
}
