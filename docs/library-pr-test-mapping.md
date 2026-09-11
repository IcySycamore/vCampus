# 图书馆 PR 测试映射修复

将下方两个章节替换到现有 PR 描述中的同名章节；其他改动文件和测试如有遗漏，应保留并补充。
检查脚本相对于仓库根目录查找文件，仅写测试文件名会报「不存在」。
这是 ADR-0004 的宽松文件映射，不代表完整的行为覆盖；LibraryServiceTest 使用模拟 DAO 接口及连接验证业务事务，不验证真实数据库。
数据库对接范围与实现责任见 [图书馆数据库接口对接](library-database-interface.md)。
客户端登录、连接复用及服务器接入要求见 [客户端连接与登录身份](library-client-session.md)。
提交此文档不会自动修改 GitHub 上已有的 PR 描述。

## 改动文件

- vcampus-client/src/main/java/edu/seu/vcampus/client/view/library/LibraryQuotaControls.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/shell/OaDashboardPanel.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/library/LibraryPanel.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/library/LibraryTableModels.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/library/LibraryViewBuilder.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/library/LibraryRequestTask.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/shell/LoginFrame.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/shell/LoginController.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/shell/MainFrame.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/shell/MainContentPanel.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/auth/ClientSession.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/auth/LoginExchange.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/auth/SessionCleanup.java
- vcampus-common/src/main/java/edu/seu/vcampus/common/entity/Book.java
- vcampus-common/src/main/java/edu/seu/vcampus/common/entity/BorrowRecord.java
- vcampus-common/src/main/java/edu/seu/vcampus/common/message/MessageType.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/module/library/BookDao.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/module/library/BorrowDao.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/module/library/LibraryException.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/module/library/LibraryMessageHandler.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/module/library/LibraryService.java

## 对应测试

- vcampus-client/src/test/java/edu/seu/vcampus/client/auth/LibraryQuotaPanelTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/module/library/LibraryBorrowLimitTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/module/library/LibraryBorrowConcurrencyTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/module/library/LibraryBorrowFlowTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/shell/OaDashboardPanelTest.java
- vcampus-common/src/test/java/edu/seu/vcampus/common/entity/LibraryEntityTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/module/library/LibraryServiceTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/module/library/LibraryMessageHandlerTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/auth/ClientSessionTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/auth/LibrarySessionIntegrationTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/shell/MainContentPanelTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/shell/MainFrameTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/shell/LoginFrameTest.java
