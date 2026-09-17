package edu.seu.vcampus.server.db;

/**
 * 存储后端开关。
 *
 * <p>
 * 「默认内存/文件，加 {@code -Dvcampus.store=jdbc} 才走 MySQL」这个决定此前在 5 个文件里各写了一遍 —— 判断语句逐字复制了 7
 * 次，其中两个文件还各自声明了一个同名常量。复制的不只是代码，更是 「哪个开关对应哪个实现」这条知识：图书馆曾只切了一半（上层换了后端、连接来源没换），
 * 商店则压根没接开关，都是这种分散装配的直接后果。
 *
 * <p>
 * 收敛到这里之后，各模块不应再直接读 {@code System.getProperty}。
 */
public final class StoreBackend {

    /** 切换存储后端的系统属性名。 */
    private static final String PROPERTY = "vcampus.store";

    private StoreBackend() {
    }

    /**
     * 是否使用数据库后端。
     *
     * @return 属性值为 {@code jdbc}（忽略大小写）时返回 true
     */
    public static boolean isJdbc() {
        return "jdbc".equalsIgnoreCase(System.getProperty(PROPERTY));
    }
}
