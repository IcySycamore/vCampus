# 图书馆 PR 改动与测试映射

按当前 main `d232a82` 和迁移后实际路径整理。目录重构、会话复用及数据库边界见 [对齐说明](library-main-alignment.md)。测试替身不代表真实数据库验收。

最近一次在 JDK 8 下执行 `mvn verify`，Common 105、Client 176、Server 338，合计 619 项测试通过。

## 改动文件

- vcampus-client/src/main/java/edu/seu/vcampus/client/VCampusClientApp.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/api/ClientApis.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/library/LibraryModule.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/library/LibraryService.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/library/LibraryTransport.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/component/StatCardPanel.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/library/LibraryBookEditor.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/library/LibraryBookSearch.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/library/LibraryCatalogPanel.java
- vcampus-client/src/main/java/edu/seu/vcampus/client/view/library/LibraryPager.java
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
- vcampus-common/src/main/java/edu/seu/vcampus/common/library/dto/BookQuery.java
- vcampus-common/src/main/java/edu/seu/vcampus/common/library/dto/BookRef.java
- vcampus-common/src/main/java/edu/seu/vcampus/common/library/dto/BorrowRequest.java
- vcampus-common/src/main/java/edu/seu/vcampus/common/library/dto/RecordRef.java
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
- vcampus-server/src/main/java/edu/seu/vcampus/server/library/BookDaoMemory.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/library/BorrowDaoMemory.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/library/LibraryAccountDaoMemory.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/library/LibraryDataSourceMemory.java
- vcampus-server/src/main/java/edu/seu/vcampus/server/library/ReservationDaoMemory.java

本轮读者规则新增 `LibraryAccount`、`LibraryAccountStatus`、`BookReservation`、`ReservationStatus`、`ReservationRef`，以及客户端的 `LibraryBorrowPanel`、`LibraryReservationPanel`，服务端的 `LibraryAccountDao`、`LibraryAccountProvisioner`、`ReservationDao`、`LibraryCirculationService`、`LibraryReservationService`、`LibraryFineService` 和银行支付适配。完整规则与协议见 [读者借阅、预约与罚款规则](library-reader-rules.md)。

## 对应测试

- vcampus-client/src/test/java/edu/seu/vcampus/client/api/ClientApisTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/library/LibraryServiceTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/library/LibraryCatalogPanelTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/library/LibraryQuotaPanelTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/library/LibraryPagerTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/library/LibraryUiFixture.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/shell/LoginFrameTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/shell/MainContentPanelTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/shell/MainFrameTest.java
- vcampus-common/src/test/java/edu/seu/vcampus/common/library/LibraryPolicyTest.java
- vcampus-common/src/test/java/edu/seu/vcampus/common/library/dto/BookQueryTest.java
- vcampus-common/src/test/java/edu/seu/vcampus/common/library/dto/LibraryRequestDtoTest.java
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
- vcampus-server/src/test/java/edu/seu/vcampus/server/library/LibraryDaoMemoryTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/library/LibraryServiceTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/library/LibraryValidRequestTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/library/LibraryWithdrawalBorrowTest.java
- vcampus-client/src/test/java/edu/seu/vcampus/client/view/library/LibraryReaderPanelTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/library/LibraryReaderRulesTest.java
- vcampus-server/src/test/java/edu/seu/vcampus/server/bank/BankIdempotentConsumeTest.java
