package edu.seu.vcampus.server;

/**
 * vCampus 服务器端入口。
 *
 * <p>占位实现：后续由网络小组（组员 A/B）在此启动 ServerSocket 与线程池
 * （见 ADR-0006）。服务端与客户端的端口号须保持一致，统一在公共常量中维护。
 */
public final class VCampusServerApp {

    /**
     * 私有构造器，禁止实例化入口类。
     */
    private VCampusServerApp() {
    }

    /**
     * 程序入口。
     *
     * @param args 命令行参数（暂未使用）
     */
    public static void main(String[] args) {
        System.out.println("vCampus Server 启动占位（网络服务待实现）");
    }
}
