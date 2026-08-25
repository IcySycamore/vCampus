# vCampus 虚拟校园系统

东南大学暑期专业技能实训项目：**虚拟校园系统**（C/S 架构，Java）。客户端提供图形界面，服务器端承载业务与数据访问，两端通过统一的 `Message` 消息协议通信。

> 本项目采用 **TDD（红-绿-重构）** 驱动开发，**纵向划分**功能模块，所有改动经 **Pull Request + CI 自动检查** 合入。关键决策见 [docs/adr/](./docs/adr/)，领域词汇见 [CONTEXT.md](./CONTEXT.md)。

## 技术栈与硬约束（来自课程 + ADR-0001/0002）

| 项       | 选型              | 说明                                                                      |
| -------- | ----------------- | ------------------------------------------------------------------------- |
| 语言     | Java              | 编译 **level=1.7**（课程要求）→ 用 **JDK 8** 作为 Maven 编译工具链        |
| 构建     | Maven 多模块      | 父工程 `vcampus` + `vcampus-common` / `vcampus-client` / `vcampus-server` |
| 数据库   | MySQL             | 建库脚本 `sql/vCampus.sql`；CI 用 GitHub Actions 的 MySQL service         |
| GUI      | Swing             | 业务逻辑抽到可测类，view 层做薄                                           |
| 测试     | JUnit 5 + Mockito | 单元测试 + 集成测试；GUI 人工冒烟                                         |
| 版本控制 | Git + GitHub      | GitHub Flow：`main` 受保护，功能分支 + PR 合入                            |

> ⚠️ **JDK 版本提醒**：Maven 构建必须在 **JDK 8** 下运行（否则 enforcer 会拒绝，防止产出 8 字节码）。本地执行前：
>
> ```powershell
> $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.492.9-hotspot"
> ```

## 目录结构

```
vcampus/
├── pom.xml                     # 父工程（聚合 + 统一配置）
├── checkstyle.xml              # 编码规范/注释检查规则
├── CONTEXT.md                  # 领域词汇表
├── docs/
│   ├── adr/                    # 架构决策记录（0001-0006）
│   └── roles.md                # 职责分工表
├── sql/vCampus.sql             # 建库 + 测试数据脚本
├── scripts/                    # PR 完整性检查脚本
├── .github/
│   ├── workflows/ci.yml        # CI：3 个状态检查
│   └── PULL_REQUEST_TEMPLATE.md
├── vcampus-common/             # 实体/Message/常量/工具（两端共享）
├── vcampus-client/             # GUI 客户端
└── vcampus-server/             # 业务/网络/DAO
```

## 团队职责分工

详见 [docs/roles.md](./docs/roles.md)。概要：**纵向 6 模块一人一个 + 网络三人小组 + 数据库/界面/文档/部署专人横切负责**。

## 本地开发

```bash
mvn clean test                 # 编译 + 跑全部测试
mvn package                    # 打包（产出 vCampusClient.jar / vCampusServer.jar）
mvn checkstyle:check           # 注释/规范检查
```

集成测试（DAO/网络）需要数据库或本机 socket，通过环境变量门控，本地无环境时自动跳过（见 ADR-0005）。

## Git 工作流（GitHub Flow）

1. 从最新 `main` 建分支：`git checkout -b feature/<模块>-<内容>`
2. 按 TDD 写代码：先写失败测试 → 最小实现 → 重构
3. 提交并推送：`git push -u origin <分支>`
4. 创建 PR，**勾选自查清单**（模板自动生成）
5. CI 三个检查全绿 + 1 人 review 通过 → 合入 `main`

> 规则：所有改动必须走 PR；`main` 禁止直推；涉及 `vcampus-common` 或 `sql/` 的改动由组长重点 review。

## 编码规范要点（CI 强制）

- 每个类/接口必须有文档注释（Javadoc）
- 每个 public 方法必须有 Javadoc（含 @param/@return）
- 每个 Java 文件 ≤ 200 行（课程硬性）
- 行宽 ≤ 100；缩进 4 空格；if/for/while 必须带大括号
- 命名：类 PascalCase、方法/变量 camelCase、常量 UPPER_SNAKE
  > 注：课程附带的《Java 编码规范.doc》沿用了 Sun 旧版"实例变量加下划线前缀"约定，该约定早已废弃；本项目按现代 Java 惯例**不加下划线**（`MemberName` 规则强制）。
- 禁止 Java 8+ 语法（lambda/stream/var 等）——由 `-source 1.7` 编译期拦截
- 无 TODO / FIXME / XXX 残留方可合入

## 提交物对照（课程清单）

- 可执行文件：`vCampusClient.jar` / `vCampusServer.jar`（Maven shade 打包）
- 数据库：`sql/vCampus.sql`（建库 + 测试数据）
- 源代码帮助文档：Javadoc（每人确保 Javadoc 完整，CI 强制）
- 文档：软件设计说明书、系统使用说明、进度报告等（见 docs/roles.md 分工）
