# TDD 工作流：红-绿-重构，JUnit 5 + 单元/集成测试分层

课程要求 TDD 驱动式开发。采用 **JUnit 5**（JDK 8 满足其最低运行要求）+ **Mockito**；测试分两层：**单元测试**（biz 业务逻辑、工具类、Message 序列化，用 Mockito 隔离网络与数据库）与**集成测试**（DAO 真连 MySQL、网络本机 localhost socket 对测）；GUI 不强制自动化测试（Swing 测试成本高），以人工冒烟 + 薄 view 层代替；每次 PR 由 CI 自动执行全套测试。集成测试须以环境变量门控（如 `DB_NAME`），本地无 MySQL 时可跳过。

**Status**: accepted

**Considered Options**:

- JUnit 4：更贴近课程年代，但对入门者与 JUnit 5 差异极小，选 5 面向现代生态。
- GUI 自动化（AssertJ-Swing）：对初学团队成本高，不采纳，改用人工冒烟 + 薄 view。

**Consequences**: 每个功能必须"先写失败测试再实现"；改动没有配套测试的 PR 会被 completeness 检查拒绝；DAO/网络集成测试在 CI 中由 MySQL service + localhost socket 自动执行。
