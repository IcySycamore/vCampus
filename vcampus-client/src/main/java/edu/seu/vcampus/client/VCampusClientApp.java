package edu.seu.vcampus.client;

/**
 * vCampus 客户端入口。
 *
 * <p>占位实现：后续由界面设计负责（组员 C）在此启动 Swing 主界面。GUI 不做自动化
 * 测试，业务逻辑应抽到可测类中、view 层做薄（见 ADR-0005）。
 */
public final class VCampusClientApp {

    /**
     * 私有构造器，禁止实例化入口类。
     */
    private VCampusClientApp() {
    }

    /**
     * 程序入口。
     *
     * @param args 命令行参数（暂未使用）
     */
    public static void main(String[] args) {
        System.out.println("vCampus Client 启动占位（GUI 待实现）");
    }
}
