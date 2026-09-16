package edu.seu.vcampus.server.shop;

/**
 * 持久化全局序列 DAO。
 */
public interface SequenceDao {

    /**
     * 初始化或重置序列。
     *
     * @param name 序列名
     * @param startValue 初始值
     * @return 是否成功
     */
    boolean initialize(String name, int startValue);

    /**
     * 获取当前值。
     *
     * @param name 序列名
     * @return 当前值
     */
    int currentValue(String name);

    /**
     * 返回当前值并递增，递增结果持久化到表中。
     *
     * @param name 序列名
     * @return 当前值
     */
    int nextValue(String name);
}
