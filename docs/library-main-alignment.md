# 图书馆 main 对齐与 PR #31 修复

更新日期：2026-09-13。迁移基线为 `a258c8a`，包含 PR #32（包结构与会话）、#33（客户端 API 约定）和 #29（分发器范围注册）。此前仅针对 `3c9cf9c` 的安排已被本次实现替代。

## 已落实

| 内容 | 当前落点 |
| --- | --- |
| 公共实体 | `common.library.entity` |
| 双端额度和角色规则 | `common.library.LibraryPolicy` |
| 服务器业务、DAO 接口、校验、装配 | `server.library` |
| 完整服务实例 | `LibraryService.getInstance(...)` 进程级单例 |
| 客户端同步 API 和请求适配 | `client.library` |
| 图书馆 Swing 页面 | `client.view.library` |
| 命令与状态 | `common.constant.Command` / `StatusCode` |
| 用户会话 | 直接复用 `client.user.UserService.currentSession()` / `currentToken()` |

客户端不再有 `client.auth` 会话、登录交换或会话包装类。`SessionEntry` 只由已有用户模块缓存；图书馆 API 只引用既有用户服务。主窗口保留 main 用户中心，以 `ClientApis.library()` 接入图书馆，使用 `UiTasks` 执行后台调用。

服务器从 token 对应的 `SessionEntry.getUuid()` 确认借阅归属。学生和教师建号时自动建立图书馆账户，默认额度 30 本，服务端以账户保存的状态和额度为准；管理员只维护馆藏。馆藏维护权限使用共享 `Permissions` / `Capability`。

本地馆藏管理、下架、参数校验与测试一并迁移；原始改动备份在 `target/pr31-before-fix-20260913.zip`，另保留迁移前 Git stash。

## 协议与数据库范围

400～403 保持原业务含义；404 为续借。馆藏管理使用 408～411；412～415 分别为预约、我的预约、取消预约和缴纳滞纳金，416 查询本人图书馆账户。具体规则见 [读者借阅、预约与罚款规则](library-reader-rules.md)。

正式 `main` 入口注入 `LibraryDataSourceMemory` 和四个 `XxxDaoMemory` 占位实现，再由 `LibraryModule` 注册。`startServer(port, libraryService)` 供集成测试和嵌入式启动显式注入。后续数据库同学通过 `DbHelper + XxxDaoJdbc` 替换入口依赖，`LibraryService` 不需要修改。

## 验证与后续对接

回归覆盖会话复用、uid、超时/断线/401、30 册额度、UUID 归属、预约晋级、15 天保留、续借起算、逾期计费、馆藏权限/下架以及正式双端入口 Socket 借还。图书罚款复用银行已有的 `consume` 接口；跨模块支付写回失败时仍需按业务号对账。真实数据库仍需按 [额度验收](library-borrow-quota.md) 和 [数据库接口](library-database-interface.md) 完成持久化及并发验证。

实际路径及对应测试见 [PR 测试映射](library-pr-test-mapping.md)。
