package edu.seu.vcampus.server.db;

/**
 * 真库测试用的定长标识片段。
 *
 * <p>
 * 测试常拿 {@code System.nanoTime()} 造唯一标识。它的十六进制长度<b>随机器运行时长变化</b>：刚开机的
 * CI 机器只有 9~10 位，跑了几十小时的本机有 12 位以上。于是
 * {@code Long.toHexString(stamp).substring(0, 12)} 在 CI 上抛
 * {@code StringIndexOutOfBoundsException: String index out of range: 12}，在本机却永远绿 ——
 * 银行、课程、学籍几处真库测试的 setUp 都踩过这一下，CI 只在其它用例被修好、测试终于跑到这里时才露出。
 *
 * <p>
 * 本工具把值规整成<b>恒定长度</b>：不足左补 0；超长则取<b>后</b> N 位而不是前 N 位 —— 十六进制的高位
 * 变化最慢，连续取几个值时会撞在一起。
 */
public final class TestIds {

    /** 私有构造器，禁止实例化工具类。 */
    private TestIds() {
    }

    /**
     * 取定长的十六进制片段。
     *
     * @param value  数值
     * @param length 需要的长度，必须为正
     * @return 长度恒为 {@code length} 的十六进制文本
     * @throws IllegalArgumentException length 不是正数
     */
    public static String hex(long value, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive");
        }
        String text = Long.toHexString(value);
        StringBuilder builder = new StringBuilder();
        for (int i = text.length(); i < length; i++) {
            builder.append('0');
        }
        builder.append(text);
        return builder.substring(builder.length() - length);
    }
}
