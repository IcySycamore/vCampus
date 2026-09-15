# 图书馆客户端连接与登录身份

2026-09-13：已按 main `a258c8a`（PR #32/#33/#29）迁移。客户端复用组长已有的 `client.user.UserService`，其内部 `ClientSession` 缓存服务器签发的 `SessionEntry` 与 token；图书馆不再维护独立身份缓存。

## 实际接线

`VCampusClientApp.connect` 创建同一条连接与 `ClientMessageDispatcher`，通过 `ClientApis.create` 装配用户和图书馆 API。`LibraryModule` 注入现有 `UserService`，不创建第二个用户服务或会话。

- 身份读取 `UserService.currentSession()`，令牌读取 `currentToken()`；每次图书馆请求读取当前值，不保存长期副本。
- `LoginFlow → MainFrame → MainContentPanel` 传递 `ClientApis`；`LibraryPanel` 构造器只接收 `apis.library()`。
- 页面用 `UiTasks` 调用同步图书馆 API，API 在共享分发器中分配 uid、发送请求、等待响应并检查状态码与载荷。页面不处理 `Message` 或网络连接。
- 当前分发器按命令码保留等待槽，图书馆传输适配器串行调用，防止同命令并发覆盖。请求超时、错误或异常载荷会结束页面等待状态。
- 用户模块登出/断线清理现有缓存，图书馆立即失去登录态。图书馆收到 401 时通知共享分发器的连接监听器，使同一份会话失效；主窗口统一返回登录页。
- 窗口关闭由应用入口异步关闭原连接，不存在图书馆自己的连接释放或登录交换类。

旧 `client.auth.ClientSession`、`LoginExchange`、`SessionCleanup` 和 `LoginController` 已移除。离线预览保留，预览页面没有模块 API，不能发业务请求。

## 服务器身份与接入

`server.library.LibraryMessageHandler` 使用认证模块提供的 `server.user.SessionManager` 校验 token，从公共 `SessionEntry.getUuid()` 取得借阅用户 ID；忽略请求中的 sender 和客户端声称的身份。

数据库对接约定见 [数据库接口](library-database-interface.md)。用户名如 `001` 仅用于登录和显示，UUID 如 `c...-...` 才是借阅归属，不能互换。既有按用户名保存的借阅记录需由数据库实现方迁移映射，不能运行时默默回退到用户名查询。

正式 `VCampusServerApp.main` 使用四个 `XxxDaoMemory` 占位实现组装图书馆服务，并将其注册到同一分发器、复用同一会话表。`startServer(port, libraryService)` 供集成测试或嵌入式启动显式注入；无服务的 `startServer(port)` 只供尚未覆盖图书馆的旧测试启动其他模块。

## 验证范围

- `client.library.LibraryServiceTest`：真实用户 API 缓存与分发器、换 token、登出/断线/401、uid 与载荷检查、超时。
- `server.LibrarySessionIntegrationTest`：正式双端入口完成真实 Socket 登录、查询、借还书、登出，核对借阅归属使用 UUID；数据库业务服务为测试替身。
- `server.library.LibraryMessageHandlerTest`：伪造 sender、UUID 归属、失效 token。
- `client.view.library` 页面测试：额度边界、失败恢复、旧结果不覆盖新结果、馆藏管理入口及保存。

自动化通过不等同于真实数据库验收完成。
