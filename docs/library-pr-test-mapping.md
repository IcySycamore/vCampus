# 图书馆 PR 改动与测试映射

按当前 main `a258c8a` 和迁移后实际路径整理。目录重构、会话复用及数据库边界见 [对齐说明](library-main-alignment.md)。测试替身不代表真实数据库验收。

## 改动文件

- vcampus-client/src/main/java/edu/seu/vcampus/client/VCampusClientApp.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/api/ClientApis.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/library/LibraryModule.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/library/LibraryService.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/library/LibraryTransport.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/component/StatCardPanel.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/library/LibraryBookEditor.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/library/LibraryCatalogPanel.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/library/LibraryPanel.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/library/LibraryQuotaControls.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/library/LibraryTableModels.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/library/LibraryViewBuilder.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/shell/LoginFlow.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/shell/LoginFrame.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/shell/MainContentPanel.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/shell/MainFrame.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/shell/MainHeaderPanel.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/shell/OaDashboardPanel.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/shell/PreviewAction.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/shell/SidebarPanel.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/theme/UiTheme.java
- vcampus-common/src/main/java/edu/seu/vcampus/common/constant/Command.java
- vcampus-common/src/main/java/edu/seu/vcampus/common/library/LibraryPolicy.java
- vcampus-common/src/main/java/edu/seu/vcampus/common/library/entity/Book.java
- vcampus-common/src/main/java/edu/seu/vcampus/common/library/entity/BorrowRecord.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/VCampusServerApp.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/library/BookDao.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/library/BorrowDao.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/library/LibraryCatalogService.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/library/LibraryCatalogValidator.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/library/LibraryException.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/library/LibraryMessageHandler.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/library/LibraryModule.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/library/LibraryRequestValidator.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/library/LibraryService.java

## 对应测试

- vcampus-client/src/test/java/edu/seu/vcampus/client/api/ClientApisTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/library/LibraryServiceTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/library/LibraryCatalogPanelTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/library/LibraryQuotaPanelTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/library/LibraryUiFixture.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/shell/LoginFrameTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/shell/MainContentPanelTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/shell/MainFrameTest.java
- vcampus-common/src/test/java/edu/seu/vcampus/common/library/LibraryPolicyTest.java
- vcampus-common/src/test/java/edu/seu/vcampus/common/library/entity/LibraryEntityTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/LibrarySessionIntegrationTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/library/LibraryBorrowConcurrencyTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/library/LibraryBorrowFlowTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/library/LibraryBorrowLimitTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/library/LibraryCatalogFixture.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/library/LibraryCatalogPermissionTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/library/LibraryCatalogServiceTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/library/LibraryMessageHandlerTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/library/LibraryRequestValidationTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/library/LibraryServiceTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/library/LibraryValidRequestTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/library/LibraryWithdrawalBorrowTest.java
