# 图书馆 main 对齐与 PR #31 修复

更新日期：2026-09-13。迁移基线为 `a258c8a`，包含 PR #32（包结构与会话）、#33（客户端 API 约定）和 #29（分发器范围注册）。此前仅针对 `3c9cf9c` 的安排已被本次实现替代。

## 已落实

| 内容 | 当前落点 |
| --- | --- |
| 公共实体 | `common.library.entity` |
| 双端额度和角色规则 | `common.library.LibraryPolicy` |
| 服务器业务、DAO 接口、校验、装配 | `server.library` |
| 客户端同步 API 和请求适配 | `client.library` |
| 图书馆 Swing 页面 | `client.view.library` |
| 命令与状态 | `common.constant.Command` / `StatusCode` |
| 用户会话 | 直接复用 `client.user.UserService.currentSession()` / `currentToken()` |

客户端不再有 `client.auth` 会话、登录交换或会话包装类。`SessionEntry` 只由已有用户模块缓存；图书馆 API 只引用既有用户服务。主窗口保留 main 用户中心，以 `ClientApis.library()` 接入图书馆，使用 `UiTasks` 执行后台调用。

服务器从 token 对应的 `SessionEntry.getUuid()` 确认借阅归属。中英文角色使用同一规则：学生 3 本、教师 5 本、管理员 10 本，未知角色借阅额度为零。馆藏维护权限使用共享 `Permissions` / `Capability`。

本地馆藏管理、下架、参数校验与测试一并迁移；原始改动备份在 `target/pr31-before-fix-20260913.zip`，另保留迁移前 Git stash。

## 协议与数据库范围

400～403 保持原业务含义和载荷。馆藏管理使用 408～411，避开团队 API 设计中预留的 404～406，具体协议见 [馆藏管理](library-catalog-management.md)。本次未实现续借、分页或管理查询等额外设计项。

正式服务器入口支持 `startServer(port, libraryService)` 注入数据库业务服务，并通过 `LibraryModule` 注册。未提供服务时返回数据库未配置提示。DAO 保留接口注入边界，具体 JDBC 实现、连接参数和已有用户名关联记录迁移由数据库实现方对接，不提供模拟持久化冒充实际数据库。

## 验证与后续对接

回归覆盖会话复用、uid、超时/断线/401、中英文角色额度、UUID 归属、馆藏权限/下架以及正式双端入口 Socket 借还。`mvn verify` 成功，478 项测试全部通过，三模块均生成覆盖率报告。本次修改文件无 Checkstyle 违规；全仓库检查仍有最新 main 原有的 76 项问题。真实数据库仍需按 [额度验收](library-borrow-quota.md) 和 [数据库接口](library-database-interface.md) 完成持久化及并发验证。

实际路径及对应测试见 [PR 测试映射](library-pr-test-mapping.md)。
