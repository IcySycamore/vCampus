# vCampus 虚拟校园系统

专业技能实训项目：**虚拟校园系统**。客户端提供图形界面，服务器端承载业务与数据访问，两端通过统一的 `Message` 消息协议通信。

> 本项目采用 **TDD（红-绿-重构）** 驱动开发，**纵向划分**功能模块，所有改动经 **Pull Request + CI 自动检查** 合入。关键决策见 [docs/adr/](./docs/adr/)，领域词汇见 [CONTEXT.md](./CONTEXT.md)。

## 技术栈与硬约束（see ADR-0001/0002）

| 项       | 选型                | 说明                                                                     |
| -------- | ------------------- | ------------------------------------------------------------------------ |
| 语言     | Java                | build level=1.7+tool kit JDK8                                            |
| 构建     | Maven               | 父工程`vcampus` + `vcampus-common` / `vcampus-client` / `vcampus-server` |
| 客户端   | Java Swing + Nimbus | -                                                                        |
| 数据库   | MySQL 8.0           | Connector/J 8.0.33+ MySQL service                                        |
| IDE      | VS Code             | -                                                                        |
| 测试     | JUnit 5 + Mockito   | 单元 + 集成测试；GUI 人工冒烟/agent+mcp                                  |
| 版本控制 | Git + GitHub        | see GitHub Flow                                                          |

> ⚠️ **JDK 版本提醒**：Maven 构建**必须**在 **JDK 8** 下运行 —— `pom.xml` 的 enforcer 要求 `[1.8,1.9)`，用 JDK 21 跑会直接报 `RequireJavaVersion failed`。
>
> `JAVA_HOME` 是**每个新终端都要重新设一次**的（不影响已开着的窗口），所以下面每条命令都带着它；路径请换成你自己机器上的 JDK 8 目录。
>
> ```powershell
> $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.492.9-hotspot"
> mvn -v   # 应显示 "Java version: 1.8.x"，而不是 21.x
> ```
>
> 用 `cmd`：`set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-8.0.492.9-hotspot`
> 用 Git Bash：`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-8.0.492.9-hotspot"`

## 目录结构

```
vcampus/
├── pom.xml                     # 父工程
├── checkstyle.xml              # 编码规范/注释检查规则
├── CONTEXT.md                  # 领域词汇表
├── docs/
│   ├── adr/                    # 架构决策记录（ADR0001-0006）
│   └── roles.md                # 职责分工表
├── sql/vCampus.sql             # 建库 + 测试数据脚本
├── scripts/                    # PR 完整性检查脚本
├── .github/
│   ├── workflows/ci.yml        # CI：3 个状态检查
│   └── PULL_REQUEST_TEMPLATE.md
├── vcampus-common/             # 实体/Message/常量/工具
├── vcampus-client/             # GUI 客户端
└── vcampus-server/             # 业务/网络/DAO
```

## 团队职责分工

详见 [docs/roles.md](./docs/roles.md)。概要：**纵向 6 模块一人一个 + 网络三人小组 + 数据库/界面/文档/部署专人横切负责**。

## 本地开发

```powershell
# 每个新终端先切 JDK 8（否则 enforcer 直接拒绝构建）
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.492.9-hotspot"   # ← 换成你的 JDK 8 路径
mvn clean test                 # 编译 + 跑全部测试
mvn package                    # 打包
mvn checkstyle:check           # 注释/规范检查
```

> 想一条命令跑完，直接用 `;` 连接：
> `$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-8.0.492.9-hotspot"; mvn clean test`

集成测试需要数据库或本机 socket，通过环境变量门控，在本地无环境时自动跳过（see ADR-0005）。

## 环境准备

### MySQL 数据库

项目需要 **MySQL 8.0** 数据库。推荐使用 Docker 方式（最简单）：

#### 方式 1：Docker（推荐）⭐

```bash
# 1. 安装 Docker Desktop (https://www.docker.com/products/docker-desktop)
# 2. 启动 MySQL 容器（自动创建数据库并导入数据）
docker-compose up -d

# 验证容器运行状态
docker-compose ps

# 停止容器
docker-compose down
```

**默认连接信息：**
- 主机：localhost:3306
- 数据库：vCampus
- 用户：vcampus / vcampus123

#### 方式 2：本地安装 MySQL 8.0

```bash
# 1. 下载安装 MySQL 8.0 (https://dev.mysql.com/downloads/mysql/)
# 2. 导入数据库
mysql -u root -p < sql/vCampus.sql

# 3. 配置连接信息
# 编辑 vcampus-server/src/main/resources/db.properties
```

详细说明见 [sql/README.md](./sql/README.md)。

## 运行项目（本地）

前置：**JDK 8**（课程要求 `-source 1.7`，见 ADR-0001） + **MySQL 8.0**（见上方环境准备），且**每个新终端**都要先把 `JAVA_HOME` 指到 JDK 8。

**终端 A —— 打包 + 起服务端：**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.492.9-hotspot"   # ← 换成你的 JDK 8 路径
mvn clean package -DskipTests                                                  # 产出两个可执行 jar
& "$env:JAVA_HOME\bin\java.exe" -jar vcampus-server\target\vCampusServer.jar  # 服务端，监听 50865
```

**终端 B —— 起客户端**（新开的终端，同样先设 JDK 8）：

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.492.9-hotspot"
& "$env:JAVA_HOME\bin\java.exe" -jar vcampus-client\target\vCampusClient.jar  # 客户端（Swing 窗口）
```

> 这里不用裸的 `java -jar`：**设 `JAVA_HOME` 并不会改变 PATH 里的 `java`**，裸命令很可能用成 JDK 21。
> 写全 `& "$env:JAVA_HOME\bin\java.exe"` 就能确保跑的是 JDK 8。

打包产物是**自包含**的：服务端 jar 含 common 与 MySQL 驱动，客户端 jar 含 common 与图标资源，直接运行上面那条 `java.exe -jar` 命令即可（无需额外 classpath）。

**首次启动服务端**会在工作目录生成两个文件（`.gitignore` 已忽略 `data/`）：

| 文件              | 说明                                                                                                                                                  |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| `data/admins.tsv` | 账号引导文件，格式`登录名<Tab>姓名<Tab>口令<Tab>角色`（角色可省略，默认管理员）。改这里增删管理员，重启生效；已存在的账号会跳过，不会覆盖已改过的口令 |
| `data/users.tsv`  | 账户库，自动维护，**只保存加盐哈希**。删掉该文件后重启即可重置全部账号                                                                                |

**默认账号**：`admin` / `admin123`（登录时身份选「管理员」）。

登录后是**工作台**；账号相关能力分两处：

- **右上角账户区（所有人）**：点一下弹出账户窗口——姓名、身份、登录名、标识，以及「修改密码」「退出登录」
- **侧栏「用户管理」（仅管理员可见）**：表格查询（关键词/角色/状态、分页），启用/禁用、重置密码、新建用户、注销，
  以及「批量注册(文件)」与「批量注销(选中)」；新建或导入账号时会自动同步建立各模块档案
- 学生 / 教师的侧栏里**没有**「用户管理」入口；账号由管理员统一分配，登录页不提供注册入口
- 批量导入按**每批 50 条**发送并显示进度（1000 条 = 20 批），超限由服务端拒绝而不是截断
- 连接意外断开时会自动回到登录页并提示重新登录（不再出现「界面还在、点什么都失败」）

批量注册文件格式（UTF-8，逗号或 Tab 分隔，`#` 开头为注释；角色支持 学生/教师/管理员 或 STUDENT/TEACHER/ADMIN）：

```text
# 登录名,姓名,角色,口令
2025001,张三,学生,init1234
2025002,李四,教师,init5678
```

覆盖默认账户文件路径（多实例部署或自动化测试用）：

```bash
java -Dvcampus.users.file=/tmp/users.tsv -Dvcampus.admins.file=/tmp/admins.tsv \
     -jar vcampus-server/target/vCampusServer.jar
```

常见问题：

- 端口被占用 → 先结束占用 50865 的进程，或改 `NetworkConstant.DEFAULT_PORT`（端口的唯一权威源，客户端与服务端共用）后重新打包
- 构建报 `RequireJavaVersion failed`（`Rule 0: ... RequireJavaVersion`）→ 当前 `JAVA_HOME` 不是 JDK 8；按上面「JDK 版本提醒」重新设一次，用 `mvn -v` 确认显示 1.8.x
- `Could not find or load main class` → 先 `mvn clean package`（`clean` 是必要的，重构后残留的旧 class 会导致加载失败）
- 客户端点登录后长时间无响应 → 确认服务端终端打印了「vCampus Server 已启动，监听端口 50865」；公网演示时在登录页右上角齿轮里改服务器地址

## GitHub WorkFlow

1. 从最新 `main` 建分支：`git checkout -b feature/<模块>-<内容>`
2. TDD ：先写失败测试 → 最小实现 → 重构
3. 提交并推送：`git push -u origin <分支>`
4. 创建 PR，**勾选自查清单**
5. CI 三个检查全绿 + 1 人 review 通过 → 合入 `main`

## 编码规范要点

- 每个类/接口必须有文档注释（Javadoc）
- 每个 public 方法必须有 Javadoc（含 @param/@return）
- 每个 Java 文件 ≤ 200 行
- 行宽 ≤ 100；缩进 4 空格；if/for/while 必须带大括号
- 命名：类 PascalCase、方法/变量 camelCase、常量 UPPER_SNAKE
- 禁止 Java 8+ 语法
- 无 TODO / FIXME / XXX 残留方可合入

## 交付物

- 可执行文件：`vCampusClient.jar` / `vCampusServer.jar`
- 数据库：`sql/vCampus.sql`
- 源代码帮助文档：Javadoc
- 文档：软件设计说明书、系统使用说明、进度报告等（see docs/roles.md）
