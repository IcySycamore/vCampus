# 客户端 API 与前端映射设计（vCampus 全功能）

> 状态：**设计稿，待各模块负责人确认**
> 依据：`docs/adr/0009-client-api-conventions.md`（本设计遵循的约定）、`docs/adr/0003/0005/0006/0008`
> 业务约定：**每个业务模块提供自己功能的逻辑 API；界面同学只负责「功能 → 控件」的对应与组合**

---

## 0. 这份报告怎么用

| 角色               | 看哪几节                                                      |
| ------------------ | ------------------------------------------------------------- |
| 界面同学（前端）   | §1 约定速查 + 各模块的「N.3 客户端 API」「N.4 控件映射」      |
| 模块负责人（后端） | 各模块的「N.1 命令码」「N.2 实体/DTO」「N.5 服务端改动点」    |
| 组长 / 评审        | §2 新增公共件、§11 装配、§12 缺口汇总、§13 交付顺序、§15 风险 |

约定细则（同步阻塞、异常模型、身份来源、断线回登录页、测试策略）**不在这里重复**，见 ADR-0009。
本报告只回答两件事：**每个功能的方法签名是什么**、**界面哪个控件调它、结果回到哪个控件**。

---

## 1. 约定速查

| 项目     | 结论                                                                                                                                                                                                    |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 调用形态 | 同步阻塞：`XxxService.方法(...)` 返回实体/DTO，失败抛 `ApiException`（非受检）                                                                                                                          |
| 线程     | 页面统一用 `UiTasks.run(task, onSuccess, onFailure)`；页面内不写 `try/catch`、不写线程                                                                                                                  |
| 方法命名 | 业务动作（`listUsers` / `searchBooks` / `recharge`），不出现命令码、`Message`、`dispatcher`                                                                                                             |
| 返回值   | 直接用 `common` 的实体/DTO；分页用 `PageResponse<T>`（默认 1/20，上限 100）                                                                                                                             |
| 身份     | 「我的」由服务端从 `SessionEntry` 解析，API 不传；管理操作显式传目标 ID                                                                                                                                 |
| 查询三轨 | **我的轨**（无身份参数）／**全量管理轨**（显式条件 + `Capability` 准入）／**受限管理轨**（资源条件 + 服务端按会话收窄）；同一命令码可承载两轨，**禁止静默降级**（不得把越权查询改写成「只返回自己的」） |
| 标识     | 跨模块引用用户一律 uuid（String）；各模块自己的记录主键（`StudentProfile.id`）只用于管理操作                                                                                                            |
| 权限     | `Permissions.can(role, Capability.X)` 决定控件可见；服务端 403 是最终防线                                                                                                                               |
| 装配     | `VCampusClientApp.connect()` → `ClientApis` → `LoginFlow` → `MainFrame` → `MainContentPanel` → 各页面（**每页只拿自己那个 API**）                                                                       |
| 断线     | 一处处理（主窗口的 `ConnectionListener`）：提示 + 回登录页；页面零连接状态代码                                                                                                                          |

---

## 2. 新增公共件清单

### 2.1 `vcampus-common` 新增（双端共享）

| 文件                                         | 说明                                                              |
| -------------------------------------------- | ----------------------------------------------------------------- |
| `common/message/PageResponse.java`           | 分页响应 `{ items, total, pageNumber, pageSize }`，`Serializable` |
| `common/user/entity/Capability.java`         | 能力枚举（按模块分组，**只能追加，不得重排**；序号不入库）        |
| `common/user/entity/Permissions.java`        | `can(Role, Capability)` 静态映射，双端共用                        |
| `common/user/dto/UserQuery.java`             | `{ keyword, role, enabled, pageNumber, pageSize }`                |
| `common/user/dto/UserUpdateRequest.java`     | `{ userName, displayName?, role 不可改 }`                         |
| `common/user/dto/ChangePasswordRequest.java` | `{ userName, proof, newSalt, newHash }`（见 §4.1）                |

### 2.2 `vcampus-client` 新增

| 文件                                      | 说明                                                                                       |
| ----------------------------------------- | ------------------------------------------------------------------------------------------ |
| `client/api/ApiException.java`            | 统一失败异常（吸收现有 `AuthException`），含 `getStatusCode()`                             |
| `client/api/ApiErrors.java`               | 状态码 → 中文文案；本地码 `L100` 断线 / `L101` 超时 / `L102` 被中断 / `L103` 响应格式异常  |
| `client/api/ClientApis.java`              | 六模块 API 只读容器：`user()/student()/course()/library()/shop()/bank()`                   |
| `client/view/UiTasks.java`                | `run(task, onSuccess, onFailure)`：后台执行 + EDT 回填 + `ApiException` → `ApiErrors` 文案 |
| `client/view/component/PageBarPanel.java` | 分页条（上一页/下一页/页码/总数），供列表页复用；页码计算是纯函数，需单测                  |

`AuthException` 删除，`LoginFlow` 的 `messageFor(...)` 改调 `ApiErrors.messageFor(...)`。

### 2.3 `Capability` 权限矩阵（三轨规则的判定依据）

「我的轨」**不需要能力**（登录即可）：`queryMyProfile` / `listMySelections` / `listMyBorrows` /
`listMyOrders` / `queryMyAccount` / `listMyTransactions` / `listGrades(自己)`。
「管理轨」按下表判定；服务端先 `can(role, cap)` 准入，再按会话收窄范围。

| Capability                            |  学生  |  教师  | 管理员 | 说明                                                       |
| ------------------------------------- | :----: | :----: | :----: | ---------------------------------------------------------- |
| `USER_MANAGE`                         |   ✗    |   ✗    |   ✓    | 用户查询/编辑/启停/注册/注销/重置密码（106–109）           |
| `STUDENT_VIEW_ALL`                    |   ✗    |   ✓    |   ✓    | 学籍列表与详情（208、201 指定他人）；教师按需再收窄        |
| `STUDENT_MODIFY_APPLY`                |   ✓    |   ✗    |   ✗    | 提交本人学籍修改申请（202）                                |
| `STUDENT_MODIFY_AUDIT`                |   ✗    |   ✓    |   ✓    | 待审列表与审核（207、203）                                 |
| `STUDENT_REGISTER` / `STUDENT_DELETE` |   ✗    |   ✗    |   ✓    | 登记（204）/ 注销（205）                                   |
| `STUDENT_CHANGE_STATUS`               |   ✗    |   ✓    |   ✓    | 改学籍状态（206）                                          |
| `COURSE_SELECT`                       |   ✓    |   ✗    |   ✗    | 选课 / 退课（303、304）                                    |
| `COURSE_GRADE_VIEW_ALL`               |   ✗    |   ✓    |   ✓    | 课程名单与成绩查询（306、309）；教师限自己授的课           |
| `COURSE_GRADE_EDIT`                   |   ✗    |   ✓    |   ✓    | 成绩录入（307）；教师限自己授的课                          |
| `COURSE_MANAGE`                       |   ✗    |   ✗    |   ✓    | 课程维护（308）                                            |
| `LIBRARY_BORROW`                      |   ✓    |   ✓    |   ✓    | 借书 / 还本人书（402、403）                                |
| `LIBRARY_BORROW_MANAGE`               |   ✗    |   ✗    |   ✓    | 借阅管理、代还（406、403 跨用户）                          |
| `LIBRARY_MANAGE`                      |   ✗    |   ✗    |   ✓    | 馆藏维护（405）                                            |
| `SHOP_BUY`                            |   ✓    |   ✓    |   ✓    | 下单 / 取消自己的订单（503、506）                          |
| `SHOP_ORDER_MANAGE`                   |   ✗    |   ✗    |   ✓    | 全量订单查询与状态推进（509、507）                         |
| `SHOP_MANAGE`                         |   ✗    |   ✗    |   ✓    | 商品维护（508）                                            |
| 银行                                  | 本人轨 | 本人轨 | 本人轨 | **无管理能力**：任何人（含管理员）都只能查自己的账户与流水 |

> 维护规则：能力枚举**只能追加，不得重排**（序号不入库，但避免 diff 噪音与误用）；
> 新增能力必须在 `Permissions` 单测里补一行矩阵，缺省为「拒绝」。

---

## 3. 模块总览

| 模块     | 号段    | 客户端 API 类                   | 客户端装配      | 服务端现状                                                         |
| -------- | ------- | ------------------------------- | --------------- | ------------------------------------------------------------------ |
| 用户管理 | 100–199 | `client.user.UserService`       | ✅ `UserModule` | 100/101/102/104/106/107/108/109/110 已可用；103/105 未实现         |
| 学生学籍 | 200–299 | `client.student.StudentService` | ⛔ 缺           | 201/202/204/205/206 已实现；**203 恒回 400**；无权限校验；内存 DAO |
| 选课系统 | 300–399 | `client.course.CourseService`   | ⛔ 缺           | **零实现**（无实体、无服务）                                       |
| 图书馆   | 400–499 | `client.library.LibraryService` | ⛔ 缺           | 服务/DAO 已实现但**未装配**；`LibraryMessageHandler` 签名不符契约  |
| 商店     | 500–599 | `client.shop.ShopService`       | ⛔ 缺           | **零实现**                                                         |
| 银行     | 600–699 | `client.bank.BankService`       | ⛔ 缺           | 服务/处理器已实现（内存）；`BankModule` **未在入口注册**           |

---

## 4. 用户管理（100–199）

### 4.1 命令码

| 命令码  | 常量                    | 状态       | 请求 `data`                                 | 响应 `data`                     | 权限                                 |
| ------- | ----------------------- | ---------- | ------------------------------------------- | ------------------------------- | ------------------------------------ |
| 100     | `USER_LOGIN`            | ✅         | `LoginRequest{userName, role}`              | `LoginChallenge{salt, nonce}`   | 匿名                                 |
| 110     | `USER_LOGIN_VERIFY`     | ✅         | `LoginVerify{userName, proof}`              | `LoginResponse{token, session}` | 匿名                                 |
| 101     | `USER_LOGOUT`           | ✅         | —                                           | —                               | 已登录                               |
| 102     | `USER_REGISTER`         | ✅         | `RegisterRequest{userName, role, password}` | —                               | 管理员                               |
| 103     | `USER_BATCH_REGISTER`   | ⛔         | `List<RegisterRequest>`                     | —                               | 管理员                               |
| 104     | `USER_UNREGISTER`       | ✅         | `UserRefRequest{userName}`                  | —                               | `USER_MANAGE`                        |
| 105     | `USER_BATCH_UNREGISTER` | ⛔         | `{ List<String> userNames }`                | —                               | 管理员                               |
| 106     | `USER_LIST`             | ✅         | `UserQuery`                                 | `PageResponse<User>`            | `USER_MANAGE`                        |
| 107     | `USER_UPDATE`           | ✅         | `UserUpdateRequest{userName, displayName}`  | —                               | `USER_MANAGE`                        |
| 108     | `USER_TOGGLE_ENABLED`   | ✅         | `UserEnabledRequest{userName, enabled}`     | —                               | `USER_MANAGE`                        |
| **109** | `USER_CHANGE_PASSWORD`  | **新增✅** | `ChangePasswordRequest`                     | —                               | 本人（proof）/ `USER_MANAGE`（重置） |

> ✅ = 本次已实现并接线；⛔ = 未实现（命令码已定义）。103/105 批量操作本次未做，
> 需要时再补（服务端需先确定「部分成功」的返回语义）。

> **109 为什么不用明文**：旧密码用挑战-应答校验（复用 100 + `NonceManager`，发送 `proof` 而非旧密码）；
> 新密码由**客户端生成新盐**，只提交 `newSalt` 与 `sha256(newSalt + 新密码)`，
> 服务端直接落库为新的 `H`，全程没有明文密码上线。本人改密时 `userName` 留空（我的轨，服务端取会话）；
> 管理员重置他人密码时填目标账号且无需 proof（管理轨，需 `USER_MANAGE`）——与 ADR-0009 D7 附则的三轨规则一致。

`User` 实体已新增 `displayName` 与 `enabled`（否则 107/108 无落点）；`enabled=false` 时 110 回 `P102`。

### 4.2 客户端 API：`client.user.UserService`

```java
// 会话
void login(String userName, Role role, String password);   // 成功后缓存 token 与服务端会话记录
void logout();
boolean isLoggedIn();
SessionEntry currentSession();                             // 未登录返回 null（角色以此为准，见 D6）
String currentToken();                                     // 供集成测试直接发请求

// 本人
void changePassword(String oldPassword, String newPassword);   // 内部走 100→110→109

// 管理（需 USER_MANAGE）
void register(String userName, Role role, String password);
void unregister(String userName);
PageResponse<User> listUsers(UserQuery query);
void updateUser(UserUpdateRequest request);
void toggleUserEnabled(String userName, boolean enabled);
```

**相对旧版的实际改动（✅ 已落地）**：去掉 `throws IOException, InterruptedException`（异常统一为 `ApiException`，
本地失败归一为 `L100/L101/L102/L103`）；`String role` → `Role` 枚举；`getSession()` 不再对外
（改 `currentSession()` / `currentToken()`，`ClientSession` 退回包内细节）。
`batchRegister/batchUnregister` **未实现**（对应 103/105），不预先摆出调不通的方法。

### 4.3 控件映射

| 页面                                  | 控件                                                       | 事件                         | 调用                                             | 回填                                                                                          |
| ------------------------------------- | ---------------------------------------------------------- | ---------------------------- | ------------------------------------------------ | --------------------------------------------------------------------------------------------- |
| 登录页 `LoginFrame`                   | 登录名框 / 密码框 / 身份切换（学生·教师·管理员）/ 登录按钮 | 点击登录                     | `user().login(name, role, password)`             | 成功 → `LoginFlow` 用 `currentSession()` 的角色开主窗口；失败 → 按钮下方红字 `ApiErrors` 文本 |
| 用户中心 `UserCenterPanel`（新）      | 头像 + 登录名 + 角色标签                                   | 页面显示                     | `currentSession()`                               | 文本回填（**只读**，不可编辑）                                                                |
| 用户中心                              | 「修改密码」按钮                                           | 点击                         | 打开 `ChangePasswordDialog`                      | —                                                                                             |
| `ChangePasswordDialog`                | 旧密码 / 新密码 / 确认新密码 / 确定                        | 点击确定（本地校验两次一致） | `changePassword(old, new)`                       | 成功 → 关闭 + 提示；失败 → 对话框内红字                                                       |
| 用户中心                              | 「退出登录」按钮                                           | 点击                         | `logout()` → `VCampusClientApp.stopQuietly()`    | 关闭主窗口 → 新建 `LoginFrame`                                                                |
| 用户中心（管理员，`UserManagePanel`） | 搜索框 + 角色下拉 + 状态下拉 + 「查询」                    | 点击查询 / 回车              | `listUsers(query)`                               | `UserTableModels.fill(model, page.items)`；`PageBarPanel` 显示 `total`                        |
| 同上                                  | 用户表格                                                   | 选中行                       | —                                                | 启用/禁用/编辑/重置密码按钮置为可用                                                           |
| 同上                                  | 「启用 / 禁用」                                            | 点击                         | `toggleUserEnabled(userName, !current.enabled)`  | 成功 → 重查当前页；失败 → 提示                                                                |
| 同上                                  | 「编辑」                                                   | 点击                         | 打开 `UserEditDialog` → `updateUser(request)`    | 成功 → 重查当前页                                                                             |
| 同上                                  | 「新建用户」                                               | 点击                         | 打开 `RegisterDialog`（**改为真调 `register`**） | 成功 → 重查当前页                                                                             |
| 同上                                  | 「注销」                                                   | 点击                         | 确认框 → `unregister(userName)`                  | 成功 → 重查当前页                                                                             |
| 分页条                                | 上一页 / 下一页                                            | 点击                         | 重新 `listUsers(query ± 1 页)`                   | 表格 + 页码回填                                                                               |

> 注册入口从登录页移到用户中心（ADR-0009 D9 推论）：注册是管理员操作，放在登录页会让匿名用户看到管理功能。

### 4.4 服务端改动点

| 文件                                                       | 改动                                                                                                               |
| ---------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `constant/Command.java`                                    | `USER_CHANGE_PASSWORD = 109`                                                                                       |
| `user/AuthServiceHandler.java`                             | 注册 104/106/107/108/109；`requireAdmin` → `requireCapability(..., Capability.USER_MANAGE)`；禁用账号登录回 `P102` |
| `user/AuthService.java`                                    | 增 `register(含姓名) / isEnabled / changePassword / setProvisioning`                                               |
| `user/UserAdminService.java`                               | **新增**：`listUsers`（关键词/角色/启用过滤 + 分页）、`updateUser`、`setEnabled`、`unregister`                     |
| `user/AccountProvisioner.java`、`AccountProvisioning.java` | **新增**：开户钩子与登记表（见 §4.5）                                                                              |
| `user/UserRepository.java`、`InMemoryUserRepository.java`  | 增 `save(Credential) / findByUuid / findAll / update / setEnabled / updateCredential / delete`；凭证增姓名与启用位 |
| `user/AuthModule.java`                                     | `register(dispatcher, provisioning)`：登记 9 条命令 + 接入开户钩子                                                 |
| `common/user/entity/User.java`                             | 增 `displayName`（可编辑）与 `enabled`（禁用不能登录）                                                             |

### 4.5 建号即建档（开户钩子）

管理员建号后，该账号的 1:1 档案应当**同步**建好，而不是等用户第一次访问时懒创建。
机制（`server.user.AccountProvisioner` + `AccountProvisioning`）：

```text
AuthService.register(...)
  ├─ 写入账户（tblUser）
  ├─ AccountProvisioning.provision(uuid, name, role)   // 广播给各模块
  │    ├─ StudentProvisioner：角色=学生 时建学籍档案（tblStudentRecord，幂等）
  │    └─ 其它模块的钩子（随模块装配逐个登记）
  └─ 任一步失败 → 回滚已建档案 + 删除刚写的账户 + 抛异常（不留「半个账户」）
```

**表与开户语义**（不是机械地「6 张表各插一行」）：

| 表                                                        | 性质                   | 注册时是否建行                                                           |
| --------------------------------------------------------- | ---------------------- | ------------------------------------------------------------------------ |
| `tblUser`                                                 | 账户本身               | ✅ 用户模块自己负责                                                      |
| `tblStudentRecord`                                        | 1:1 学籍档案           | ✅ 且仅角色=学生（`StudentProvisioner`）                                 |
| `tblBankAccount`                                          | 1:1 银行账户           | ❌ 由用户显式开户（604）——保留 `B100` 未开户语义与页面的「开通账户」入口 |
| `tblBorrowRecord` / `tblCourseSelection` / `tblShopOrder` | 行为记录（一账号多行） | ❌ 首次借书/选课/下单时写入；预建空行是脏数据                            |

注销（104）走反向：`AccountProvisioning.revoke(uuid)` 让各模块软删除自己的档案，再删账户。

> 演示账号同样走开户流程，因此预置的学生 001 一登录就能查到自己的学籍。

---

## 5. 学生学籍（200–299）

### 5.1 命令码

| 命令码  | 常量                    | 状态      | 请求 `data`                                        | 响应 `data`                          | 权限                            |
| ------- | ----------------------- | --------- | -------------------------------------------------- | ------------------------------------ | ------------------------------- |
| 201     | `STUDENT_QUERY`         | ✅        | `StudentQuery{profileId?, userUuid?}`              | `StudentProfile`                     | 本人 = 自己；教师/管理员 = 全部 |
| 202     | `STUDENT_MODIFY_APPLY`  | ✅        | `StudentModifyRequest{profileId, changes, reason}` | —                                    | 学生本人                        |
| 203     | `STUDENT_MODIFY_AUDIT`  | ⛔ 恒 400 | `ModifyAuditRequest{requestId, approved, comment}` | —                                    | `STUDENT_MODIFY_AUDIT`          |
| 204     | `STUDENT_REGISTER`      | ✅        | `StudentProfile`                                   | —                                    | `STUDENT_REGISTER`              |
| 205     | `STUDENT_DELETE`        | ✅        | `StudentDeleteRequest{profileId}`                  | —                                    | `STUDENT_DELETE`                |
| 206     | `STUDENT_CHANGE_STATUS` | ✅        | `StudentStatusRequest{profileId, status}`          | —                                    | `STUDENT_CHANGE_STATUS`         |
| **207** | `STUDENT_MODIFY_LIST`   | **新增**  | `ModifyRequestQuery{status?, 分页}`                | `PageResponse<StudentModifyRequest>` | `STUDENT_MODIFY_AUDIT`          |
| **208** | `STUDENT_LIST`          | **新增**  | `StudentQuery{keyword?, status?, 分页}`            | `PageResponse<StudentProfile>`       | `STUDENT_VIEW_ALL`              |

**203 的实现设计**：新增 `common.student.entity.StudentModifyRequest`
`{ requestId, profileId, applicantUuid, changesJson?, status(PENDING/APPROVED/REJECTED), reason, comment, appliedAt, auditedBy, auditedAt }`
与 `ModifyRequestStatus` 枚举；202 只落一条 PENDING 申请（**不直接改学籍**），203 通过时才把 `changes` 应用到 `StudentProfile`。
这样才能体现「学生申请 → 教务审核」的业务语义，也是把 203 从 400 变成真实功能的唯一办法。

### 5.2 客户端 API：`client.student.StudentService`

```java
StudentProfile queryMyProfile();                              // 201，服务端按会话取自己的 userUuid
StudentProfile queryProfile(long profileId);                 // 201（管理端查看）
PageResponse<StudentProfile> listStudents(StudentQuery query);   // 208
void applyModification(StudentModifyRequest request);        // 202（学生）
PageResponse<StudentModifyRequest> listModifyRequests(ModifyRequestQuery query); // 207
void auditModification(String requestId, boolean approved, String comment);      // 203
void registerStudent(StudentProfile profile);                // 204
void changeStatus(long profileId, EnrollmentStatus status);  // 206
void deleteStudent(long profileId);                          // 205
```

### 5.3 控件映射

| 页面 `StudentPanel`     | 控件                                    | 事件     | 调用                                                 | 回填                                            |
| ----------------------- | --------------------------------------- | -------- | ---------------------------------------------------- | ----------------------------------------------- |
| 我的学籍（学生）        | 只读表单：学号/uuid、入学年份、状态徽章 | 页面显示 | `queryMyProfile()`                                   | `FormFieldPanel` 各字段文本 + 状态徽章颜色      |
| 我的学籍                | 「申请修改」按钮                        | 点击     | 打开 `StudentModifyDialog`                           | —                                               |
| `StudentModifyDialog`   | 入学年份输入 + 修改理由                 | 确定     | `applyModification(request)`                         | 成功 → 关闭 + 提示「已提交，等待审核」          |
| 学籍管理（教师/管理员） | 关键词框 + 状态下拉 + 表格              | 查询     | `listStudents(query)`                                | `StudentTableModels.fill(...)` + `PageBarPanel` |
| 学籍管理                | 表格行                                  | 选中     | —                                                    | 「修改状态 / 注销」按钮启用                     |
| 学籍管理                | 「修改状态」                            | 点击     | 状态下拉 + `changeStatus(profileId, status)`         | 成功 → 重查；失败 → 提示                        |
| 学籍管理                | 「注销」                                | 点击     | 确认框 → `deleteStudent(profileId)`                  | 成功 → 重查                                     |
| 学籍管理                | 「新生登记」                            | 点击     | `StudentRegisterDialog` → `registerStudent(profile)` | 成功 → 重查                                     |
| 修改审核（教师/管理员） | 待审表格（申请人、字段、理由、时间）    | 页面显示 | `listModifyRequests(PENDING)`                        | `ModifyRequestTableModels.fill(...)`            |
| 修改审核                | 「通过 / 驳回」                         | 点击     | `auditModification(requestId, approved, comment)`    | 成功 → 重查列表                                 |

### 5.4 服务端改动点

| 文件                                                | 改动                                                                                                                          |
| --------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| `student/StudentService.java`                       | 把「待办 权限」注释落成真实校验：入参改为 `(SessionEntry actor, ...)` 或在 Handler 层判 `Permissions`；新增申请/审核/分页列表 |
| `student/StudentMessageHandler.java`                | 203 从固定 400 改为真实分支；新增 207/208 分支；权限判断改走 `Permissions.can`                                                |
| `student/StudentDao.java` / `StudentDaoMemory.java` | 增 `find(query, offset, limit) / count(query) / 申请单 CRUD`；`StudentDaoJdbc` 按 ADR-0002 补 MySQL 实现                      |
| `student/StudentModule.java`                        | 注册 207/208                                                                                                                  |

---

## 6. 选课系统（300–399，全新）

### 6.1 命令码（本模块首次分配）

| 命令码 | 常量                    | 请求 `data`                                           | 响应 `data`                       | 权限                                    |
| ------ | ----------------------- | ----------------------------------------------------- | --------------------------------- | --------------------------------------- |
| 301    | `COURSE_LIST`           | `CourseQuery{keyword?, semester?, department?, 分页}` | `PageResponse<Course>`            | 已登录                                  |
| 302    | `COURSE_DETAIL`         | `CourseRef{courseUuid}`                               | `Course`                          | 已登录                                  |
| 303    | `COURSE_SELECT`         | `CourseRef{courseUuid}`                               | `CourseSelection`                 | `COURSE_SELECT`（学生）                 |
| 304    | `COURSE_DROP`           | `SelectionRef{selectionUuid}`                         | —                                 | `COURSE_SELECT`（本人）                 |
| 305    | `COURSE_MY_SELECTIONS`  | —                                                     | `List<CourseSelection>`           | 已登录                                  |
| 306    | `COURSE_GRADE_LIST`     | `GradeQuery{studentUuid?, courseUuid?, semester?}`    | `List<CourseSelection>`（带成绩） | 本人 / `COURSE_GRADE_VIEW_ALL`          |
| 307    | `COURSE_GRADE_EDIT`     | `GradeEditRequest{selectionUuid, score}`              | —                                 | `COURSE_GRADE_EDIT`（教师，限自己课程） |
| 308    | `COURSE_UPSERT`         | `Course`                                              | `Course`                          | `COURSE_MANAGE`                         |
| 309    | `COURSE_SELECTION_LIST` | `CourseSelectionQuery{courseUuid?, 分页}`             | `PageResponse<CourseSelection>`   | `COURSE_GRADE_VIEW_ALL`                 |

### 6.2 公共实体与 DTO

| 类型                                                  | 字段要点                                                                                                                                             |
| ----------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Course`                                              | `courseUuid, code(课程号), name, teacherUuid, department, semester, credits, capacity, enrolled, schedule(时间地点), status`                         |
| `CourseStatus`                                        | `OPEN(开放选课) / CLOSED(停止选课) / FINISHED(已结课)`                                                                                               |
| `CourseSelection`                                     | `selectionUuid, courseUuid, courseName(冗余展示), studentUuid, selectedAt, status(SELECTED/DROPPED), score(Double，null=未出分), gradePoint(Double)` |
| `SelectionStatus`                                     | `SELECTED / DROPPED`                                                                                                                                 |
| `GradeScale`                                          | 纯函数 `gradePointOf(double score)`：90→4.0、80→3.0、70→2.0、60→1.0、<60→0.0（需单测）                                                               |
| `CourseQuery` / `GradeQuery` / `CourseSelectionQuery` | 见 §6.1；分页字段默认 1/20、上限 100                                                                                                                 |

> 设计取舍：**成绩挂在选课记录上**（一条选课记录 = 一次修读），不另开 `Grade` 表。
> 理由：退课/重修天然是多次「修读」，成绩随修读记录走，查询「我的成绩」就是查自己的选课记录，无需联表。

### 6.3 客户端 API：`client.course.CourseService`

```java
PageResponse<Course> listCourses(CourseQuery query);          // 301
Course getCourse(String courseUuid);                          // 302
CourseSelection selectCourse(String courseUuid);              // 303
void dropCourse(String selectionUuid);                        // 304
List<CourseSelection> listMySelections();                     // 305
List<CourseSelection> listGrades(GradeQuery query);           // 306
void updateGrade(String selectionUuid, double score);         // 307
Course saveCourse(Course course);                             // 308
PageResponse<CourseSelection> listSelections(CourseSelectionQuery query); // 309
```

### 6.4 控件映射

| 页面 `CoursePanel`      | 控件                                      | 事件     | 调用                                 | 回填                                                  |
| ----------------------- | ----------------------------------------- | -------- | ------------------------------------ | ----------------------------------------------------- |
| 选课（Tab 1）           | 关键词框 + 学期下拉 + 院系下拉 + 「查询」 | 点击     | `listCourses(query)`                 | `CourseTableModels.fill(...)`；`PageBarPanel`         |
| 选课                    | 课程表格                                  | 选中行   | —                                    | 「选课」按钮启用；`enrolled >= capacity` 时置灰       |
| 选课                    | 「选课」                                  | 点击     | `selectCourse(courseUuid)`           | 成功 → 提示 + 刷新列表；失败（`400` 重复/满员）→ 红字 |
| 我的课表（Tab 2）       | 表格：课程号/名称/学分/教师/时间地点      | 页面显示 | `listMySelections()`                 | `SelectionTableModels.fill(...)`                      |
| 我的课表                | 「退课」                                  | 点击     | 确认框 → `dropCourse(selectionUuid)` | 成功 → 重查                                           |
| 我的成绩（Tab 3）       | 表格：课程/学分/成绩/绩点                 | 页面显示 | `listGrades(本学期)`                 | 表格 + 顶部 GPA 汇总（`GradeScale` 计算）             |
| 成绩录入（教师，Tab 4） | 课程下拉 + 学生名单表格 + 分数输入        | 「保存」 | `updateGrade(selectionUuid, score)`  | 成功 → 行内更新 + 提示                                |
| 课程维护（管理员）      | 「新建 / 编辑课程」                       | 保存     | `saveCourse(course)`                 | 成功 → 重查                                           |

### 6.5 服务端改动点（全部新增）

`common/course/{entity,dto}` → `server/course/{CourseService, CourseMessageHandler, CourseModule, CourseDao, CourseDaoJdbc/InMemory}` → 入口 `VCampusServerApp` 注册。
选课必须校验：重复选课（同一 `studentUuid + courseUuid + 未退课`）、容量上限、课程状态为 `OPEN`；
并发选课用 DAO 层的条件更新（`UPDATE ... SET enrolled = enrolled + 1 WHERE enrolled < capacity`）保证不超卖。

---

## 7. 图书馆（400–499）

### 7.1 命令码

| 命令码          | 常量                   | 状态          | 请求 `data`                                         | 响应 `data`                  | 权限                           |
| --------------- | ---------------------- | ------------- | --------------------------------------------------- | ---------------------------- | ------------------------------ |
| 400             | `LIBRARY_SEARCH`       | ⚠️ 改载荷     | `BookQuery{keyword, field, 分页}`                   | `PageResponse<Book>`         | 已登录                         |
| 401             | `LIBRARY_LIST_BORROWS` | ⚠️ 改身份来源 | —                                                   | `List<BorrowRecord>`         | 本人                           |
| 402             | `LIBRARY_BORROW`       | ⚠️ 改载荷     | `BorrowRequest{isbn}`                               | `BorrowRecord`               | `LIBRARY_BORROW`               |
| 403             | `LIBRARY_RETURN`       | ⚠️ 改载荷     | `RecordRef{recordId}`                               | `BorrowRecord`               | 本人 / `LIBRARY_BORROW_MANAGE` |
| 404（新，可选） | `LIBRARY_RENEW`        | 新增          | `RecordRef{recordId}`                               | `BorrowRecord`               | 本人                           |
| 405（新，可选） | `LIBRARY_BOOK_UPSERT`  | 新增          | `Book`                                              | `Book`                       | `LIBRARY_MANAGE`               |
| 406（新）       | `LIBRARY_BORROW_LIST`  | 新增          | `BorrowQuery{userUuid?, isbn?, overdueOnly?, 分页}` | `PageResponse<BorrowRecord>` | `LIBRARY_BORROW_MANAGE`        |

**400/401/402/403 的三处必要修改**：

1. 请求载荷改为显式 DTO（现在是裸 `String[]` / `String` / `Number`，靠 `ClassCastException` 兜底）；
2. 身份来源改为会话 uuid —— 现在 `LibraryMessageHandler` 用 `request.getSender()` 当 userId，
   而 `sender` 在协议里是「发送方标识（预留）」，客户端一旦忘记填就是空指针式错误，且**可被伪造**（客户端随便填别人的名字就能借书）；
3. `LibraryMessageHandler` 实现 `common.message.MessageHandler`：`void handle(Message, MessageSender)`，与其余模块一致。

### 7.2 客户端 API：`client.library.LibraryService`

```java
PageResponse<Book> searchBooks(BookQuery query);   // 400
List<BorrowRecord> listMyBorrows();                // 401
BorrowRecord borrowBook(String isbn);              // 402
BorrowRecord returnBook(long recordId);            // 403
BorrowRecord renewBook(long recordId);             // 404（可选）
Book saveBook(Book book);                          // 405（可选，管理员）
PageResponse<BorrowRecord> listBorrows(BorrowQuery query); // 406（管理轨；`userUuid` 为空 = 全部）
```

> **归还的两种语义共用一个方法**：`returnBook(recordId)` 本人只能还自己的记录；
> 图书管理员（`LIBRARY_BORROW_MANAGE`）可代还任意记录。服务端先判 `Capability` 再收窄范围（ADR-0009 D7 附则）。

### 7.3 控件映射

| 页面 `LibraryPanel`    | 控件                                                 | 事件        | 调用                            | 回填                                                                 |
| ---------------------- | ---------------------------------------------------- | ----------- | ------------------------------- | -------------------------------------------------------------------- |
| 馆藏检索               | 关键词框 + 字段下拉（全部/书名/作者/ISBN）+ 「检索」 | 点击 / 回车 | `searchBooks(query)`            | `LibraryTableModels.showBooks(model, page.items)`；`PageBarPanel`    |
| 馆藏检索               | 结果表格                                             | 选中行      | —                               | 「借阅」按钮启用；`availableCopies == 0` 时置灰                      |
| 馆藏检索               | 「借阅」                                             | 点击        | `borrowBook(isbn)`              | 成功 → 提示 + 刷新该行可借数；失败（`404/400`）→ 红字                |
| 我的借阅               | 表格：书名/ISBN/借出日/应还日/状态                   | 页面显示    | `listMyBorrows()`               | `LibraryTableModels.showBorrows(...)`；逾期行标红                    |
| 我的借阅               | 「归还」                                             | 点击        | 确认框 → `returnBook(recordId)` | 成功 → 重查                                                          |
| 我的借阅               | 「续借」（可选）                                     | 点击        | `renewBook(recordId)`           | 成功 → 重查                                                          |
| 借阅管理（图书管理员） | 借阅人框 + ISBN 框 + 「仅看逾期」勾选框 + 表格       | 查询        | `listBorrows(query)`            | `LibraryTableModels.showBorrows(...)` + `PageBarPanel`（逾期行标红） |
| 借阅管理               | 「代还」                                             | 点击        | 确认框 → `returnBook(recordId)` | 成功 → 重查（服务端按 `Capability` 允许跨用户归还）                  |
| 馆藏维护（图书管理员） | 「新建 / 编辑馆藏」                                  | 保存        | `saveBook(book)`                | 成功 → 重查检索结果                                                  |

> 现有 `client.view.library.LibraryPanel` 是**孤儿**（未接入 `MainContentPanel`），且直接 `client.send(request)` 不带 uid
> ——在新契约下会抛 `IllegalArgumentException`。改造要求：构造器接收 `LibraryService`，删掉 `ClientSocketListener` 依赖与
> `UIUpdateHandler` 实现，改由 `MainContentPanel` 注册进 `PageNames.LIBRARY`。

### 7.4 服务端改动点

| 文件                                 | 改动                                                                                                     |
| ------------------------------------ | -------------------------------------------------------------------------------------------------------- |
| `library/LibraryMessageHandler.java` | 实现 `MessageHandler`，`handle(request, sender)` 内主动 `sender.send(...)`；错误码不再塞进 `data` 字符串 |
| `library/LibraryModule.java`         | **新增**：`register(dispatcher, sessions, libraryService)`；在 `VCampusServerApp.startServer` 里装配     |
| `library/LibraryService.java`        | 方法加 `SessionEntry actor` 或改由 Handler 判权；`search` 改分页                                         |
| `server/dao/DataSourceProvider.java` | **新增**：读 `db.properties` 提供 `DataSource`（ADR-0002），供 library 及其余 JDBC DAO 共用              |

---

## 8. 校园商店（500–599，全新）

### 8.1 命令码（本模块首次分配）

| 命令码    | 常量                 | 请求 `data`                                     | 响应 `data`               | 权限                       |
| --------- | -------------------- | ----------------------------------------------- | ------------------------- | -------------------------- |
| 501       | `SHOP_ITEM_LIST`     | `ShopItemQuery{keyword?, category?, 分页}`      | `PageResponse<ShopItem>`  | 已登录                     |
| 502       | `SHOP_ITEM_DETAIL`   | `ItemRef{itemUuid}`                             | `ShopItem`                | 已登录                     |
| 503       | `SHOP_ORDER_CREATE`  | `OrderCreateRequest{lines[itemUuid, quantity]}` | `ShopOrder`               | `SHOP_BUY`                 |
| 504       | `SHOP_ORDER_LIST`    | `OrderQuery{status?, 分页}`                     | `PageResponse<ShopOrder>` | 本人                       |
| 505       | `SHOP_ORDER_DETAIL`  | `OrderRef{orderUuid}`                           | `ShopOrder`               | 本人 / `SHOP_ORDER_MANAGE` |
| 506       | `SHOP_ORDER_CANCEL`  | `OrderRef{orderUuid}`                           | `ShopOrder`               | 本人（仅 `CREATED`）       |
| 507       | `SHOP_ORDER_ADVANCE` | `OrderStatusRequest{orderUuid, status}`         | `ShopOrder`               | `SHOP_ORDER_MANAGE`        |
| 508       | `SHOP_ITEM_UPSERT`   | `ShopItem`                                      | `ShopItem`                | `SHOP_MANAGE`              |
| 509（新） | `SHOP_ORDER_QUERY`   | `OrderQuery{buyerUuid?, status?, 分页}`         | `PageResponse<ShopOrder>` | `SHOP_ORDER_MANAGE`        |

> 504 与 509 共用 `OrderQuery`：504 是**我的轨**（服务端强制本人，忽略 `buyerUuid`），
> 509 是**全量管理轨**（可按买主筛选）；两轨不合并成一个方法，避免调用点看不出「查的是谁」。

### 8.2 公共实体与 DTO

| 类型              | 字段要点                                                                                                |
| ----------------- | ------------------------------------------------------------------------------------------------------- |
| `ShopItem`        | `itemUuid, name, description, category, price(BigDecimal), stock, status(ON_SALE/OFF_SHELF), createdAt` |
| `ShopOrder`       | `orderUuid, buyerUuid, totalAmount, status, createdAt, paidAt, items(List<ShopOrderItem>)`              |
| `ShopOrderItem`   | `itemUuid, name(下单时快照), unitPrice(快照), quantity, subtotal`                                       |
| `ShopOrderStatus` | `CREATED / PAID / SHIPPED / COMPLETED / CANCELLED`                                                      |

> 订单项保存**名称与单价快照**：商品改价后历史订单金额不得变化。

### 8.3 客户端 API：`client.shop.ShopService`

```java
PageResponse<ShopItem> listItems(ShopItemQuery query);   // 501
ShopItem getItem(String itemUuid);                       // 502
ShopOrder createOrder(List<OrderLineRequest> lines);     // 503
PageResponse<ShopOrder> listMyOrders(OrderQuery query);  // 504
ShopOrder getOrder(String orderUuid);                    // 505
ShopOrder cancelOrder(String orderUuid);                 // 506
ShopOrder advanceOrder(String orderUuid, ShopOrderStatus status); // 507
ShopItem saveItem(ShopItem item);                        // 508
PageResponse<ShopOrder> queryOrders(OrderQuery query);   // 509（管理轨；`buyerUuid` 为空 = 全部）
```

### 8.4 控件映射

| 页面 `ShopPanel`   | 控件                                   | 事件     | 调用                                         | 回填                                                                          |
| ------------------ | -------------------------------------- | -------- | -------------------------------------------- | ----------------------------------------------------------------------------- |
| 商品浏览           | 关键词框 + 分类下拉 + 「查询」         | 点击     | `listItems(query)`                           | `ShopTableModels.fill(...)` + `PageBarPanel`                                  |
| 商品浏览           | 商品表格 + 数量输入框                  | 选中行   | —                                            | 「加入购物车」启用；`stock == 0` 置灰                                         |
| 商品浏览           | 「加入购物车」                         | 点击     | —（**本地集合**，不发请求）                  | 购物车标签数 +1                                                               |
| 购物车             | 明细表（商品/单价/数量/小计）+ 合计    | 页面显示 | —（本地计算）                                | 表内计算（纯函数，可测）                                                      |
| 购物车             | 「提交订单」                           | 点击     | `createOrder(lines)`                         | 成功 → 清空购物车 + 跳「我的订单」并提示；失败（余额不足 `400`/`B100`）→ 红字 |
| 我的订单           | 状态下拉 + 表格：订单号/金额/状态/时间 | 页面显示 | `listMyOrders(query)`                        | `OrderTableModels.fill(...)`                                                  |
| 我的订单           | 「查看明细」                           | 点击     | `getOrder(orderUuid)`                        | 弹出明细对话框                                                                |
| 我的订单           | 「取消订单」                           | 点击     | 确认框 → `cancelOrder(orderUuid)`            | 成功 → 重查（后端同时退款，见 8.5）                                           |
| 订单查询（管理员） | 买主框 + 状态下拉 + 表格               | 查询     | `queryOrders(query)`                         | `OrderTableModels.fill(...)` + `PageBarPanel`                                 |
| 订单管理（管理员） | 「发货 / 完成」                        | 点击     | `advanceOrder(orderUuid, SHIPPED/COMPLETED)` | 成功 → 重查                                                                   |
| 商品维护（管理员） | 「新建 / 编辑商品」                    | 保存     | `saveItem(item)`                             | 成功 → 重查                                                                   |

### 8.5 与银行的联动（服务端内部调用，不过网络）

`ShopService` 构造时注入 `BankService`（已存在的 `consume`/`cashback` 正是为此预留的）：

| 动作     | 服务端行为                                                                                                                  |
| -------- | --------------------------------------------------------------------------------------------------------------------------- |
| 下单 503 | ① 校验库存并**条件扣减**（本地事务）→ ② `bankService.consume(buyer, total, orderUuid, "商城订单")` → ③ 失败则回滚库存并抛错 |
| 取消 506 | ① 订单置 `CANCELLED` → ② `bankService.cashback(buyer, total, orderUuid, "订单取消退款")`                                    |
| 未开户   | 银行抛 `BankAccountNotOpenedException` → 商店转 `B100`，客户端提示「请先开通校园银行账户」                                  |

**跨模块一致性**：库存与余额不在同一事务内，采用「先库存、后扣款、失败补偿」；以 `relatedOrderId = orderUuid` 作为对账键
（`BankTransaction.relatedOrderId` 已有该字段）。日终对账属可选增强，不阻塞 P3。

### 8.6 服务端改动点（全部新增）

`common/shop/{entity,dto}` → `server/shop/{ShopService, ShopMessageHandler, ShopModule, ShopDao}` → 入口注册。
`ShopModule.register(dispatcher, sessions, shopService)`；`ShopService` 由入口注入 `bankService`（**先装配银行模块拿到服务引用**，顺序在 `VCampusServerApp` 中固定）。

---

## 9. 校园银行（600–699）

### 9.1 命令码（已定义，无需新增）

| 命令码 | 常量                    | 请求 `data`                                               | 响应 `data`                   | 权限 |
| ------ | ----------------------- | --------------------------------------------------------- | ----------------------------- | ---- |
| 601    | `BANK_ACCOUNT_QUERY`    | —                                                         | `BankAccountResponse`         | 本人 |
| 602    | `BANK_RECHARGE`         | `BankRechargeRequest{amount}`                             | `BankRechargeResponse`        | 本人 |
| 603    | `BANK_TRANSACTION_LIST` | `BankTransactionQueryRequest{pageNumber, pageSize, type}` | `BankTransactionListResponse` | 本人 |
| 604    | `BANK_ACCOUNT_OPEN`     | —                                                         | `BankAccountResponse`         | 本人 |

### 9.2 客户端 API：`client.bank.BankService`

```java
BankAccountResponse queryMyAccount();                                  // 601
BankRechargeResponse recharge(BigDecimal amount);                      // 602
BankTransactionListResponse listMyTransactions(BankTransactionQueryRequest query); // 603
BankAccountResponse openAccount();                                     // 604
```

### 9.3 控件映射

| 页面 `BankPanel` | 控件                                                            | 事件     | 调用                        | 回填                                                         |
| ---------------- | --------------------------------------------------------------- | -------- | --------------------------- | ------------------------------------------------------------ |
| 账户概览         | 余额大字 + 账户号 + 状态徽章（`StatCardPanel`）                 | 页面显示 | `queryMyAccount()`          | 文本回填；**未开户（`B100`）→ 显示「开通账户」卡片而非报错** |
| 账户概览         | 「开通账户」                                                    | 点击     | `openAccount()`             | 成功 → 刷新概览                                              |
| 账户概览         | 「充值」                                                        | 点击     | 打开 `RechargeDialog`       | —                                                            |
| `RechargeDialog` | 金额输入（`FormFieldPanel`，校验 > 0 且两位小数）+ 快捷金额按钮 | 确定     | `recharge(amount)`          | 成功 → 关闭 + 刷新余额与流水；失败 → 对话框内红字            |
| 资金流水         | 类型下拉（全部/充值/消费/返现）+ 分页条                         | 切换     | `listMyTransactions(query)` | `BankTableModels.fill(...)`；金额按类型着色（收入绿/支出红） |
| 资金流水         | 表格：时间/类型/金额/变动后余额/说明                            | 页面显示 | 同上                        | 同上                                                         |

### 9.4 服务端改动点

| 文件                             | 改动                                                                                          |
| -------------------------------- | --------------------------------------------------------------------------------------------- |
| `VCampusServerApp.java`          | 加一行 `BankModule.register(dispatcher, bankService, identityResolver)`（当前**完全没接线**） |
| `bank/BankIdentityResolver.java` | 身份改从 `SessionEntry` 取（见 §9.5 对齐项），不再从 `Message.sender` 推断                    |
| `bank/BankService.java`          | 内存实现保留；后续若要落库，按 ADR-0002 换成事务 DAO（接口尽量不动）                          |

### 9.5 对齐项：账户归属的用户标识

`BankAccount.userId` 是 `Long`，但 `User` 与 `SessionEntry` 里**都没有 Long 主键**（只有 uuid），
因此现在这个 Long 没有权威来源。设计建议（ADR-0009 D7）：

- **推荐**：`BankAccount.ownerUuid`（String）+ `BankIdentityResolver` 返回 `SessionEntry.getUuid()`。
  银行尚未接线，改动面仅 `BankAccount` / `BankService` 方法签名 / `BankMessageHandler` / DTO / 测试，成本最低且一次到位。
- 备选：保留 `Long userId`，但必须由用户模块提供 uuid → Long 的权威映射，禁止在银行侧自行哈希派生。

> `BankService.consume/cashback` 供商店调用的那两个方法，同样改为接收 `ownerUuid`，与商店的 `buyerUuid` 一致。

---

## 10. 命令码分配总表

| 号段    | 模块     | 已用            | 本次新增                                              |
| ------- | -------- | --------------- | ----------------------------------------------------- |
| 0–99    | 网络层   | 1 心跳          | —                                                     |
| 100–199 | 用户管理 | 100/101/102/110 | 103–109（103–108 常量已在，仅缺接线；109 需新增常量） |
| 200–299 | 学生学籍 | 201–206         | 207 申请列表、208 学籍分页列表                        |
| 300–399 | 选课系统 | —               | 301–309                                               |
| 400–499 | 图书馆   | 400–403         | 404 续借、405 馆藏维护（可选）、406 借阅管理          |
| 500–599 | 商店     | —               | 501–509                                               |
| 600–699 | 银行     | 601–604         | —                                                     |

---

## 11. 客户端装配与页面接线

```text
main()
└─ LoginFrame（登录名/密码/身份切换）
   └─ LoginFlow（后台线程）
      └─ VCampusClientApp.connect(host, port)  // 建连接 + 六模块自装配，返回 ClientApis
         ├─ ClientMessageDispatcher（uid 发号 + 按命令码配对 + 处理器表）
         ├─ ClientSocketListener（连接/心跳/重连）
         └─ ClientApis = { UserModule.register, StudentModule.register, CourseModule.register,
                           LibraryModule.register, ShopModule.register, BankModule.register }
      └─ apis.user().login(name, role, password)
      └─ MainFrame(apis, session)              // 角色取 currentSession()，不取登录页选择
         └─ MainContentPanel(apis, session)
            ├─ PageNames.HOME     → OaDashboardPanel（问候 + 摘要）
            ├─ PageNames.USER     → UserCenterPanel(apis.user())
            ├─ PageNames.STUDENT  → StudentPanel(apis.student())
            ├─ PageNames.COURSE   → CoursePanel(apis.course())
            ├─ PageNames.LIBRARY  → LibraryPanel(apis.library())
            ├─ PageNames.SHOP     → ShopPanel(apis.shop())
            └─ PageNames.BANK     → BankPanel(apis.bank())
```

**断线处理（只写一处）**：`MainFrame` 构造时 `apis.user().addConnectionListener(...)`（或由 `ClientApis` 暴露 `addConnectionListener`），
回调里 `SwingUtilities.invokeLater` → 提示「连接已断开，请重新登录」→ `dispose()` 主窗口 → `new LoginFrame().setVisible(true)`。
各页面**不得**出现 `isConnected()` 之类的判断。

**页面写法模板**（前端同学照抄结构，不写业务判断）：

```java
refreshButton.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent event) {
        UiTasks.run(new UiTasks.Task<PageResponse<Book>>() {
            @Override
            public PageResponse<Book> run() {
                return api.searchBooks(readQueryFromWidgets());
            }
        }, new UiTasks.Success<PageResponse<Book>>() {
            @Override
            public void accept(PageResponse<Book> page) {
                LibraryTableModels.showBooks(model, page.getItems());
                pageBar.show(page.getPageNumber(), page.getPageSize(), page.getTotal());
            }
        });
    }
});
```

---

## 12. 服务端缺口与改动汇总

| #   | 缺口                                                                                 | 位置                                       | 归属              |
| --- | ------------------------------------------------------------------------------------ | ------------------------------------------ | ----------------- |
| 1   | ✅ 已解决：106/107/108/109 已注册；仅 103/105 批量操作仍未实现                       | `AuthServiceHandler` / `Command`           | 用户管理          |
| 2   | ✅ 已解决：仓库增分页查询/编辑/启停/改密/注销                                        | `UserRepository` 及其实现                  | 用户管理          |
| 3   | ✅ 已解决：`User` 增 `displayName` 与 `enabled`                                      | `common.user.entity.User`                  | 用户管理          |
| 3b  | ⛔ 待办：`tblUser` 增 uuid 列（`uId VARCHAR(8)` 与代码 `String uuid` 口径未统一）    | `sql/vCampus.sql`                          | 数据库 / 用户管理 |
| 4   | 203 恒回 400；无审核流实体                                                           | `StudentMessageHandler` / `common.student` | 学籍              |
| 5   | 学籍各方法无权限校验（仅注释）                                                       | `StudentService`                           | 学籍              |
| 6   | `StudentDaoMemory` 为占位，无 JDBC 实现                                              | `server.student`                           | 学籍              |
| 7   | `LibraryMessageHandler` 签名不符契约；用 `sender` 当身份；无 `LibraryModule`；未注册 | `server.library`                           | 图书馆            |
| 8   | `LibraryService` 需要 `DataSource`，但无提供者                                       | `server.dao`                               | 图书馆 / 数据库   |
| 9   | `BankModule` 未注册；身份来源是 `Message.sender`；`Long userId` 无来源               | `VCampusServerApp` / `server.bank`         | 银行              |
| 10  | 选课、商店零实现                                                                     | —                                          | 选课 / 商店       |
| 11  | 客户端只有 `UserService`；5 个页面是占位                                             | `client.*`                                 | 各模块            |
| 12  | `RegisterDialog` / `ChangePasswordDialog` 假成功                                     | `client.view.dialog`                       | 用户管理          |
| 13  | `LibraryPanel` 孤儿且绕过分发器（缺 uid 会抛异常）                                   | `client.view.library`                      | 图书馆            |
| 14  | 主界面角色来自登录页选择而非服务器返回                                               | `LoginFlow` / `MainFrame`                  | 用户管理          |
| 15  | 主界面无断线处理（会话清空但窗口留着）                                               | `MainFrame`                                | 用户管理          |

---

## 13. 交付顺序（可转 issue）

### P0 用户管理闭环 —— 让「能跑」变成「能用」（负责人：组长）

- **目标**：管理员能在界面上完整管理用户；任何角色能改自己的密码；断线能正确回到登录页。
- **已基**：100/101/102/110 + `AuthServiceHandler` + `UserService` + 登录页/主窗口骨架。
- **预期改动量**：服务端 6 个文件、客户端 12 个文件（含 3 个新页面/对话框）、测试 10 个文件。**中**。
- **任务清单**
  1. `Command` 加 109；`User` 加 `enabled`；DTO（`UserQuery`/`UserUpdateRequest`/`ChangePasswordRequest`）。
  2. `AuthService` + `UserRepository`（含内存实现）补 106/107/108/109 所需方法。
  3. `AuthServiceHandler` 注册新命令；`requireAdmin` → `Permissions.can(role, USER_MANAGE)`。
  4. `Capability` + `Permissions` 落地（`common.user.entity`），配单测。
  5. 客户端 `client.api`（`ApiException`/`ApiErrors`/`ClientApis`）+ `UiTasks`，配单测；删 `AuthException`。
  6. `UserService` 按 §4.2 补齐并去受检异常；`UserModule` 返回服务。
  7. `connect()` 返回 `ClientApis`；`LoginFlow` 用 `currentSession()` 的角色；`MainFrame` 收 `ClientApis`。
  8. `UserCenterPanel` + `UserManagePanel` + `UserEditDialog`（真接 API）；`ChangePasswordDialog` 真接；`RegisterDialog` 迁入用户中心。
  9. `MainFrame` 注册断线回调 → 提示 + 回登录页。
- **预期效果（验收）**
  - [ ] 管理员登录后可在用户中心分页查询、启用/禁用、编辑、新建、注销用户；
  - [ ] 任何角色可在用户中心修改自己的密码，改完能用新密码重新登录、旧密码失效；
  - [ ] 断电/服务端 kill 后客户端弹出「连接已断开」并回到登录页；
  - [ ] `mvn -B clean test` 全绿；新增横切件（`UiTasks`/`Permissions`/`ApiErrors`）均有单测；
  - [ ] 1 条端到端：登录 → 用户列表 → 禁用 → 登出。

### P1 图书馆 + 银行接线（张芸菲 / 周奥林）

- **目标**：两个「服务端已写好、界面没接上」的模块端到端可用。
- **任务**：`DataSourceProvider`；`LibraryMessageHandler` 改契约 + `LibraryModule` + 入口注册；图书馆身份改会话 uuid；
  借阅管理轨（406 + `Capability.LIBRARY_BORROW_MANAGE` + 代还的 Capability 判定）；
  `client.library.LibraryService`/`LibraryModule` + `LibraryPanel` 改造接入（含借阅管理 Tab）；
  `BankModule` 入口注册 + 身份对齐（§9.5）+ `client.bank.BankService`/`BankModule` + `BankPanel` + `RechargeDialog`。
- **验收**：学生能检索→借书→看到「我的借阅」→还书（可借数变化正确）；能开户→充值→看到流水；
  未开户时银行页显示「开通账户」而不是报错。

### P2 学籍接线（邬致远）

- **目标**：学籍从「只有内存 CRUD」变成有权限、有审核流的功能。
- **任务**：审核流实体 + 202/203/207/208；权限走 `Permissions`；`StudentDao` JDBC 实现；
  `client.student.StudentService`/`StudentModule` + `StudentPanel`（含学生申请、教务审核两个视角）。
- **验收**：学生提交修改申请 → 教务在待审列表看到 → 通过后学籍字段真的变了；学生查不到别人的学籍（403）。

### P3 选课 + 商店（赵芊雅 / 魏雨霏）

- **目标**：两个全新模块端到端跑通，商店通过银行完成真实扣款。
- **任务**：按 §6 / §8 从实体到页面全链路（含商店管理轨 509）；商店注入 `BankService` 完成扣款/退款。
- **验收**：选课不超容量、不重复选；成绩录入后学生能查到绩点；下单后银行余额减少且流水出现「商城订单」；
  取消订单后余额退回且流水出现「订单取消退款」。

### 可选增强（不阻塞主线）

图书馆续借 404 / 馆藏维护 405；商店对账任务；`BankService` 落库；`BankTransactionListResponse` 对齐 `PageResponse<T>`。

---

## 14. 测试要求（落实 ADR-0005 / ADR-0009 D10）

| 被测对象                                                         | 测试方式                                    | 必测点                                                                                                                     |
| ---------------------------------------------------------------- | ------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| `XxxService`（客户端模块 API）                                   | 同包 `FakeDispatcher` 单测                  | ① 命令码与载荷正确；② 成功 → 返回值映射正确；③ 各种 `statusCode` → `ApiException` 及文案；④ 「我的」类方法**不带**身份参数 |
| `UiTasks`                                                        | 单测（可注入执行器或直接用真实线程 + 闩子） | 后台执行、EDT 回填、失败回调拿到 `ApiException`、任务抛运行时异常不吞                                                      |
| `Permissions` / `Capability`                                     | 单测（角色 × 能力矩阵）                     | 三类角色 × 全部能力；未知组合默认拒绝                                                                                      |
| `ApiErrors`                                                      | 单测                                        | 每个已知码有中文文案；未知码有兜底文案                                                                                     |
| 表格/分页回填纯函数（`XxxTableModels`、`PageBarPanel` 页码计算） | 单测                                        | 空列表、单页、末页、越界页码                                                                                               |
| 服务端 `XxxService`                                              | 单测（内存 DAO / 假 DAO）                   | 业务规则：重复选课、容量上限、余额不足、审核流转                                                                           |
| 服务端 `XxxHandler`                                              | 单测（假 `MessageSender`）                  | 401/403 分支、未知命令 400、业务异常 → 正确状态码                                                                          |
| 集成（真实 socket）                                              | 保留现有 3 条 + P0/P1 各 1 条               | 登录鉴权、心跳、用户管理闭环、（P1）借还书闭环                                                                             |

> GUI 不做自动化（ADR-0005 既定）；页面「薄」是它的替代保障：判断逻辑全部下沉到可测的纯函数与横切件。

---

## 15. 风险与开放问题

| #   | 问题                                                      | 影响                           | 建议                                                                                         |
| --- | --------------------------------------------------------- | ------------------------------ | -------------------------------------------------------------------------------------------- |
| 1   | `BankAccount.userId`（Long）无权威来源                    | 银行与商店联调走不通           | 采纳 §9.5 首选方案（改 `ownerUuid`），P1 内完成                                              |
| 2   | `LibraryService` 依赖真实 `DataSource`，集成测试需 MySQL  | 本地/CI 环境门槛               | CI 已有 MySQL；本地用 `DB_NAME` 门控跳过（沿用 ADR-0005）                                    |
| 3   | 客户端上送密码相关字段仍是 TCP 明文（注册、改密的新哈希） | 局域网内可被嗅探               | 记录为已知限制；TLS 属课程外增强项                                                           |
| 4   | FileLength ≤200 与「每模块一个 Handler」冲突              | 新增 Handler 可能超行          | 分支逻辑下沉到 Service；单命令族拆多个 Handler 类                                            |
| 5   | main 已有 4 个文件破 checkstyle 上限                      | CI checkstyle 长期红           | 交由各文件归属人清理，不阻塞本设计                                                           |
| 6   | 登录页仍需选择身份（协议 100 需带 role）                  | 与「角色以服务器为准」看似矛盾 | 选择仅用于发起登录（服务器仍校验 `P101` 角色匹配），**主界面权限一律以 `SessionEntry` 为准** |
| 7   | 学籍「申请-审核」新增实体属于公共工程改动                 | 按 ADR-0007 需双端同步         | 一次性提交实体 + DTO，避免两端不同步                                                         |
| 8   | 商店/银行跨模块一致性                                     | 极端情况下库存与余额不一致     | 以 `relatedOrderId` 对账；失败即补偿回滚；日终对账为可选增强                                 |
