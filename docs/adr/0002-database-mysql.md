# 数据库选用 MySQL（而非机房友好的 Access）

课程允许 Access 或 MySQL 二选一且全组统一。为支持 TDD 与 PR 自动检查，CI 需要在每次构建时自动重放一套真实可用的数据库环境——MySQL 是 GitHub Actions 的原生 service container，而 Access 依赖本机 ODBC 且 JDK 8 已移除 JDBC-ODBC Bridge（接入需 UCanAccess 第三方驱动）。因此选用 MySQL，提交物以"建库 + 测试数据"的 `sql/vCampus.sql` 脚本提供。

**Status**: accepted

**Considered Options**:

- Access：机房演示兼容性好，但无法在 CI 中可靠运行，且 JDK 8 下接入成本高。
- MySQL（采纳）：CI 集成测试顺畅，官方 Connector/J 驱动成熟，多线程/并发表现好。

**Consequences**: 机房若仅提供 Access 环境，答辩演示须自备电脑运行 MySQL；可另行用 UCanAccess 生成 Access 版测试库作为兜底。DAO 集成测试由 CI 的 MySQL service 提供真实连接。
