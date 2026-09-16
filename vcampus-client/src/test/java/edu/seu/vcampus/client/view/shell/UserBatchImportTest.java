package edu.seu.vcampus.client.view.shell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 批量导入分批数计算测试（提示文案与真实分片必须一致）。
 */
class UserBatchImportTest {

    @Test
    void batchCountRoundsUp() {
        assertEquals(1, UserBatchImport.batchCount(1));
        assertEquals(1, UserBatchImport.batchCount(50));
        assertEquals(2, UserBatchImport.batchCount(51));
        assertEquals(20, UserBatchImport.batchCount(1000));
    }

    @Test
    void batchCountIsZeroForEmptyInput() {
        assertEquals(0, UserBatchImport.batchCount(0));
        assertEquals(0, UserBatchImport.batchCount(-3));
    }
}
