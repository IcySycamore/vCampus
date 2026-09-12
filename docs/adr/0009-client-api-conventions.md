# 客户端 API 约定：模块出逻辑 API，界面只做功能到控件的映射

业务约定：**每个功能模块对外提供自己功能的逻辑 API，界面同学只负责「功能 → 控件」的对应与组合**。
本 ADR 固化该约定在 vCampus 客户端的具体形态（API 长相、错误模型、身份来源、页面接线、断线处理、测试要求），
供六个模块的实现者共同遵守。设计明细见 `docs/客户端API与前端映射设计.md`。

**Status**: accepted

**Decision**:

## D1 模块 API 是同步阻塞的

`client.<业务>.XxxService` 的方法都是**同步阻塞**调用：调用即发请求、返回即拿到结果或抛出异常。
不在 API 内部起线程、不做回调、不返回 `Future`。

- 理由：Java 7（ADR-0001 禁止 lambda）下匿名类是唯一手段，回调式 API 会迫使每个页面写匿名类；
  同步 API 让界面代码是「按顺序读下来」的直线。
- 线程切换由横切工具 `client.view.UiTasks` 统一负责：页面把 API 调用包在 `UiTasks` 里，
  它在后台线程执行、在 EDT 回填结果。

## D2 直接返回共享实体/DTO；失败抛统一的非受检 `ApiException`

- 返回值就是 `vcampus-common` 里的实体或 DTO，**不再包一层客户端 VO**：`Message.data` 与 API 返回值一一对应。
- 失败统一抛 `client.api.ApiException extends RuntimeException`（**非受检**），携带 `statusCode` 与服务器文案。
  方法签名**不声明**受检异常，页面不需要写 `try/catch`。
- `AuthException` 并入 `ApiException`（删除 `AuthException`）。
- 状态码 → 中文文案集中在 `client.api.ApiErrors.messageFor(code)`，登录页硬编码的
  `messageFor(AuthException)` 收敛到这里。
- 客户端本地失败（连接断开、超时、被中断、响应格式异常）用 `Lxxx` 局部状态码表示，
  **只存在于客户端**，不上线协议。服务端失败沿用 `StatusCode`（`400/401/403/404/500/P100/P101/P102/B100`）。

## D3 每个模块一个具体类，方法名即业务动作

- 形态：`client.<业务>.XxxService`（**具体类**，不做接口+实现分离），与 `client.<业务>.XxxModule.register(dispatcher)`
  成对出现；`register` 创建服务并完成模块自身接线，返回该服务。
- 方法名用业务动作：`listUsers()`、`searchBooks(keyword, field)`、`recharge(amount)`、
  `queryMyProfile()`。**不得**出现命令码、`Message`、`dispatcher`、`send/request` 等协议词汇。
- 页面只认识 `XxxService`，不认识 `Message`；`Message` 的构造与解析是模块 API 的内部实现。

## D4 条件查询用 `XxxQuery` DTO；分页结果用 `PageResponse<T>`

- 查询条件超过两个参数时，在 `common.<业务>.dto` 定义 `XxxQuery`（字段可为 null 表示不过滤），
  而不是给 API 加一长串参数。
- 数据量可能大的列表**必须分页**：请求 DTO 带 `pageNumber/pageSize`（默认 1/20，上限 100），
  响应统一用 `common.message.PageResponse<T>{ items, total, pageNumber, pageSize }`。
- 确定不会大的简单列表（如「我的借阅」「我的课表」）直接返回 `List<Entity>`，不强行分页。
- 已有 `common.bank.dto.BankTransactionQueryRequest` / `BankTransactionListResponse` 是本约定的既有实例；
  后续新增按 `PageResponse<T>` 统一，银行侧是否对齐为可选项（不阻塞其他模块）。

## D5 客户端处理器机制保留为「预留能力」

`client.handler.ClientMessageHandler`、`UiCallback`、`dispatcher.register/registerFallback/setUiCallback`
**保留不改**（当前无生产使用者）。边界写进 Javadoc：

1. 只有「服务器主动发起、且命令码不与在途请求冲突」的消息才登记处理器；
2. 请求-响应一律走 `dispatcher.request(...)`，不用处理器；
3. `UiCallback` 仅用于处理器内部把界面更新切回 EDT；页面取数用 `UiTasks`；
4. 未匹配在途请求、又未登记处理器的消息 → 丢弃并记日志。

## D6 身份唯一来源是 `SessionEntry`；权限用共享的 `Capability` + `Permissions`

- 角色/权限**只**来自服务端会话记录 `SessionEntry`（登录后固定，不再从登录页选择回读）。
- 新增 `common.user.entity.Capability`（能力枚举，按模块分组、**只能追加不能重排**）与
  `common.user.entity.Permissions.can(Role, Capability)`，**双端共享**：
  服务端用它判 403（替换写死的 `requireAdmin`），客户端用它决定控件可见性。
- 客户端 `Role`/`Capability` 只用于「显示/隐藏控件」，**不作为安全边界**；服务端 403 才是最终防线。

## D7 客户端 API 不接收「我是谁」；管理操作显式传目标 ID

- 「我的」语义由服务端从 `SessionEntry` 解析：`queryMyProfile()`、`listMyBorrows()`、`queryMyAccount()`
  **不带身份参数**。
- 管理操作显式传目标：`changeStudentStatus(profileId, status)`、`toggleUserEnabled(userName, enabled)`。
- 跨模块统一标识只有 **uuid（String）**：`User.uuid` 由注册时生成，`SessionEntry.uuid` 是会话内的权威副本，
  学籍/选课/借阅/订单一律以 uuid 引用用户。
- 记录自己的主键（`StudentProfile.id`、`BorrowRecord.id`）仍是各模块的 DB 自增键，只在
  「管理操作指定目标」时出现在 API 签名里，不作为跨模块用户标识。
- **对齐项（银行）**：`BankAccount.userId`（Long）目前**没有权威来源**——`User` 与 `SessionEntry` 都只有 uuid。
  处理方式二选一，推荐前者：① 把账户归属改为 `ownerUuid`（String），`BankIdentityResolver` 改读
  `SessionEntry.getUuid()`，身份来源与全系统一致；② 保留 Long，但必须由用户模块给出 uuid → Long 的权威映射
  （不得在银行侧用哈希自行派生）。在完成对齐前，银行模块的联调以 ① 为准。

### D7 附则：查询 API 的三轨规则

「我的」语义交给会话后，管理端查询按下列三轨处理（**客户端 API 层分轨，命令码不翻倍**）：

| 轨道       | 方法形态                           | 身份/范围来源       | 准入                                  | 例                                                                   |
| ---------- | ---------------------------------- | ------------------- | ------------------------------------- | -------------------------------------------------------------------- |
| 我的轨     | 无身份参数                         | 会话 `SessionEntry` | 仅需登录                              | `queryMyProfile()`、`listMyBorrows()`、`listMyOrders()`              |
| 全量管理轨 | 显式传目标/条件 DTO                | 参数                | `Capability`                          | `listUsers(query)`、`queryProfile(profileId)`、`listStudents(query)` |
| 受限管理轨 | 显式传**资源**条件（不是用户条件） | 资源 + 会话         | `Capability` 准入，Service 按会话收窄 | `listSelections(courseUuid)`（教师限自己授的课）                     |

- **不翻倍**：同一命令码可同时承载两轨，靠「DTO 里身份字段是否可空 + 调用者角色」区分；
  只有**响应类型或语义真的不同**时才新增码（如 201 取一条 vs 208 分页检索）。
- **双保险，且禁止静默降级**：服务端一律「先 `Capability` 准入，再按会话收窄范围」；
  **不得**把「越权查询」静默改写成「只返回自己的数据」——那会让调用方以为拿到了全量，比 403 更危险。
- **没有管理端的模块不造管理轨**：银行只服务本人（管理员也不查他人余额）；商店管理端只做商品维护与订单状态推进。
- **管理员「看自己」走我的轨**（`currentSession()` 本地可得），不为其单开管理 API。

## D8 `ClientApis` 只读容器，逐页注入

- `client.api.ClientApis` 持有六个模块 API 的只读引用（`user()/student()/course()/library()/shop()/bank()`），
  在 `VCampusClientApp.connect()` 里由各 `XxxModule.register(dispatcher)` 组装而成。
- 装配链只有一条：`connect()` → `LoginFlow` → `MainFrame` → `MainContentPanel`。
  **每个页面构造器只接收自己那一个 API**（如 `LibraryPanel(LibraryService api)`），容器不往下传。
- 禁止服务定位器（静态 `getInstance`）、禁止 8 参数构造器。

## D9 断线即回登录页，页面不写连接状态逻辑

- 连接断开 → 会话清空 → 关连接 → 销毁主窗口 → 新建 `LoginFrame` 并提示「连接已断开，请重新登录」。
- 该逻辑只在主窗口一处实现（注册一个 `ConnectionListener`），**页面里零连接状态代码**。
- `UiTasks` 统一把 `ApiException` 转成提示，页面不写 `try/catch`。
- 推论：注册入口从登录页移到用户中心（注册是管理员操作）。

## D10 测试策略（落实 ADR-0005）

- **主战场是模块 API 单测**：用同包测试替身 `FakeDispatcher` 断言「方法 → 命令码 + 载荷」与
  「响应/状态码 → 返回值或 `ApiException` 文案」，先写失败测试（TDD）。
- **不写 GUI 自动化**（ADR-0005 已定）：页面靠人工冒烟，但必须「薄」到无需测试——
  判断逻辑一律下沉（回填用纯函数、错误用 `UiTasks`、权限用 `Permissions`、文案用 `ApiErrors`）。
- **新增横切件必须有单测**：`UiTasks`、`Permissions`、`ApiErrors`、`PageResponse` 相关的绑定函数。
- **集成测试只覆盖关键链路**：保留现有 3 条真实 socket 用例，P0 增 1 条
  「登录 → 用户列表 → 启用/禁用 → 登出」端到端。
- **服务端**新增：Service 单测（内存 DAO）+ Handler 单测（假 `MessageSender`）。

## D11 页面三条硬规则

1. 页面只做「建控件 + 绑事件 + 调 API + 回填」，不写业务判断；
2. 表格/列表回填逻辑抽成**纯函数**（如既有 `LibraryTableModels`），便于单测；
3. 页面构造器注入依赖，不在页面里 `new` 服务或读静态单例。

**Consequences**

- 六人并行开发互不阻塞：页面同学只需拿到 `XxxService` 的方法签名与 DTO，加上一份「控件 ↔ 方法」映射表。
- 换实现零成本：API 方法内从「发消息」换成别的传输方式，页面代码不动。
- 代价：同步 API 占用调用线程——必须经 `UiTasks` 调用，否则界面会卡；该规则靠评审与模板守住。
- 代价：`ApiException` 为非受检异常，编译器不再提示处理；用「页面不得出现 `try/catch`」的评审规则反向约束。
- 与 ADR-0003（纵向模块）、ADR-0008（包结构）一致：新增代码落在 `client.<业务>`、`client.api`、`common.<业务>`。
- 与 ADR-0004（PR 门禁）一致：新增横切件与其单测同 PR 提交，否则 completeness 检查不过。
