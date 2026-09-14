package edu.seu.vcampus.client.user;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量载荷切片：把任意长度的列表切成每片不超过 {@code maxSize} 条（见 ADR-0010 D1）。
 *
 * <p>
 * 纯函数、无状态，便于单测。抽出来的原因：切片规则属于协议约定（必须与
 * {@code ProtocolLimit.MAX_BATCH_SIZE} 一致），不该混在发请求的代码里。
 */
public final class BatchSplitter {

    /**
     * 私有构造器，禁止实例化工具类。
     */
    private BatchSplitter() {
    }

    /**
     * 把列表切成若干片，保持原顺序。
     *
     * @param items   待切分的列表；null 或空返回空列表（调用方无需特判）
     * @param maxSize 单片最大条数
     * @param <T>     元素类型
     * @return 切片列表
     * @throws IllegalArgumentException maxSize 不为正
     */
    public static <T> List<List<T>> split(List<T> items, int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        List<List<T>> chunks = new ArrayList<List<T>>();
        if (items == null || items.isEmpty()) {
            return chunks;
        }
        for (int start = 0; start < items.size(); start += maxSize) {
            int end = Math.min(start + maxSize, items.size());
            chunks.add(new ArrayList<T>(items.subList(start, end)));
        }
        return chunks;
    }
}
