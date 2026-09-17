package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.network.ClientMessageSender;
import edu.seu.vcampus.client.network.ClientNetworkConfig;
import edu.seu.vcampus.client.network.ClientSocketListener;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.client.view.shop.ShopOrderPanel;
import edu.seu.vcampus.common.shop.dto.OrderListResponse;
import edu.seu.vcampus.common.shop.dto.OrderQuery;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import edu.seu.vcampus.server.VCampusServerApp;
import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证真实客户端网络层能够完成 Shop 状态订单查询。 */
class ShopOrderNetworkIntegrationTest {

    private static final String USERNAME = "shop_network_user";
    private static final String PASSWORD = "shop_network_pwd_2026";
    private static Thread serverThread;
    private static int port;

    @BeforeAll
    static void startServer() throws Exception {
        File directory = Files.createTempDirectory("vcampus-shop-network").toFile();
        directory.deleteOnExit();
        File admins = new File(directory, "admins.tsv");
        writeLines(admins, USERNAME + "\tShop测试用户\t" + PASSWORD + "\t学生");
        System.setProperty("vcampus.users.file", new File(directory, "users.tsv").getPath());
        System.setProperty("vcampus.admins.file", admins.getPath());

        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    VCampusServerApp.startServer(0);
                } catch (IOException error) {
                    System.err.println("Shop 网络测试服务器退出: " + error.getMessage());
                }
            }
        }, "shop-network-server");
        serverThread.setDaemon(true);
        serverThread.start();

        long deadline = System.currentTimeMillis() + 5000L;
        while (port <= 0 && System.currentTimeMillis() < deadline) {
            port = VCampusServerApp.getPort();
            if (port <= 0) {
                Thread.sleep(20L);
            }
        }
        assertTrue(port > 0, "Shop 网络测试服务器未按时启动");
    }

    @AfterAll
    static void stopServer() throws Exception {
        VCampusServerApp.stopServer();
        serverThread.join(3000L);
    }

    @Test
    void filteredOrderListReturnsThroughRealNetwork() throws Exception {
        ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();
        ClientSocketListener socket = new ClientSocketListener("127.0.0.1", port,
                dispatcher, new ClientNetworkConfig(
                        3000, 10000, 0, 100L, 200L, 200L, 3000L));
        dispatcher.bindSender(new ClientMessageSender(socket));
        UserService users = new UserService(dispatcher, 5000L);
        final edu.seu.vcampus.client.shop.ShopService shop =
                new edu.seu.vcampus.client.shop.ShopService(dispatcher, users);
        dispatcher.addConnectionListener(users);
        dispatcher.addConnectionListener(shop);

        try {
            socket.connect();
            users.login(USERNAME, null, PASSWORD);
            ShopOrder created = shop.createOrder("S001", 1);
            assertNotNull(created);

            long started = System.currentTimeMillis();
            OrderListResponse response = shop.listMyOrders(new OrderQuery(
                    1, 10, ShopOrderStatus.UNPAID, null));

            assertEquals(1L, response.getTotalCount());
            assertEquals(created.getoId(), response.getOrders().get(0).getoId());
            assertTrue(System.currentTimeMillis() - started < 5000L,
                    "订单状态查询不应等待到客户端超时");

            final ShopOrderPanel[] panels = new ShopOrderPanel[1];
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    panels[0] = new ShopOrderPanel(shop);
                }
            });
            waitForPanelLoad(panels[0]);
            assertEquals(1, find(panels[0], JTable.class).getRowCount());
        } finally {
            socket.close();
        }
    }

    private static void waitForPanelLoad(ShopOrderPanel panel) throws Exception {
        long deadline = System.currentTimeMillis() + 5000L;
        while (System.currentTimeMillis() < deadline) {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    // Flush pending Swing callbacks.
                }
            });
            JLabel feedback = labelWithText(panel, "订单加载成功");
            if (feedback != null) {
                return;
            }
            Thread.sleep(10L);
        }
        assertNotNull(labelWithText(panel, "订单加载成功"),
                "订单面板不能一直停留在正在加载状态");
    }

    private static JLabel labelWithText(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JLabel
                    && text.equals(((JLabel) component).getText())) {
                return (JLabel) component;
            }
            if (component instanceof Container) {
                JLabel found = labelWithText((Container) component, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T extends Component> T find(Container parent, Class<T> type) {
        for (Component component : parent.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
            if (component instanceof Container) {
                T found = find((Container) component, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void writeLines(File file, String line) throws IOException {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file),
                Charset.forName("UTF-8"));
        try {
            writer.write(line);
            writer.write("\n");
        } finally {
            writer.close();
        }
    }
}
