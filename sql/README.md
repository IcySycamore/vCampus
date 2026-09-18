# vCampus 数据库

本目录有三份脚本，职责不同：

| 脚本               | 干什么                                                     | 能不能不跑            |
| ------------------ | ---------------------------------------------------------- | --------------------- |
| `vCampus.sql`      | 建库：26 张表、索引、外键                                  | 不能                  |
| `vCampus-data.sql` | 引用数据（学院、学院领域、教室、教室可用时间、课程）       | 测试库要，生产 不需要 |
| `vCampus-demo.sql` | 演示数据（可登录的师生账号、选课、成绩、图书、银行、商店） | 能：只要开发库好看    |

```bash
mysql -uroot -proot < sql/vCampus.sql
mysql -uroot -proot --default-character-set=utf8mb4 < sql/vCampus-data.sql
```

两份都跑完得到一个服务端跑得起来的库（不含任何可登录账号）。两份脚本都幂等，可从任意状态重跑。

学院本身也由服务端自己解决：首次启动会按 `data/colleges.tsv`（不存在则生成模板）建立学院池，所以**只跑第一份也能注册师生账号**。

## 为什么引用数据单独一份

学院的**正经来源不是这份脚本**，而是服务端的学院引导文件 `data/colleges.tsv`：服务端首次启动时把里面列出的学院写入库，并把它们作为新建学生/教师档案可挂靠的学院池（见 `CollegePoolBootstrap`）。全新部署因此不需要手工跑任何 SQL。

为什么学院必须有人预置：`tblCourseStudent.cstCollegeUuid` 与 `tblTeacher.tcCollegeUuid` 都是非空外键，而服务端在给学生/教师开户时会把档案挂到学院池里的一所。库里一行学院都没有时，开户会被数据库以 1452 拒掉，而且一旦开户钩子失败，整个注册流程回滚 —— 已经建好的账号一起被删掉。以前这个口子只能用本脚本堵，现在改由引导文件堵，与 `data/admins.tsv` 同一套做法。

那本脚本里为何还写着学院？为了让**测试库**能用：测试库每次运行按结构重建、一行数据也没有，`TestSchemaSetup` 随即执行本脚本，测试要用的行必须由脚本喂进去，不能让测试代码在 Java 里「先补一行」。它里面的学院按 uuid 覆盖写（不是先删后插）—— 档案可能正引用着那一行。

除学院之外的教室、时间槽与三门课纯粹是本地演示用，生产不需要（课由界面建）。

## 为什么建库脚本只有一份

以前是「`vCampus.sql` 建基础表 + `vCampus-extend.sql` 打补丁」两份，理由是课程给定的脚本要「保持不动」。代价是：

- 交付物里的建库脚本自己建不出完整的库，跑服务端前得记得再执行第二个脚本；
- 补丁里「给既有库补列」的 `ALTER` 段在全新库上必然报 1060，只能靠 `mysql --force` 盖过去 —— 一旦加了 `--force`，真正该中断的错误也一起被吞了；
- 同一个模块的表被拆在两份文件里（图书馆的 `tblBook` 在第一份、`tblLibraryAccount` 在第二份），改一个模块要两头找。

现在合成一份，**按模块分节**：全局序列 / 用户与认证 / 学籍 / 课程与选课 / 图书馆 / 银行 / 商店。一个模块的表全部挨在一起。

## 运行环境

MySQL 8.0（兼容 5.7）。两种起库方式：

**A. Docker（推荐）**

```bash
docker compose up -d          # 容器名 vcampus-mysql，端口 3306，root/root
docker compose down -v        # 连数据卷一起清掉，下次启动重新执行建库脚本
```

注意：初始化脚本只在**数据卷为空**时执行一次。改了 `sql/vCampus.sql` 想重新建库，要么 `down -v`，要么手工执行一遍脚本。

`docker-entrypoint-initdb.d` 只挂了建库脚本，所以**引用数据要手工补一次**（容器里 mysql 客户端默认 latin1，中文会被二次编码，必须带上字符集参数）：

```bash
docker exec -i vcampus-mysql mysql -uroot -proot --default-character-set=utf8mb4 < sql/vCampus-data.sql
```

**B. 本机 MySQL**

```bash
mysql -uroot -p < sql/vCampus.sql
mysql -uroot -p --default-character-set=utf8mb4 < sql/vCampus-data.sql
```

## 连接配置

服务端按 `db.properties` → 环境变量 → 缺省值的顺序取配置（见 `DbHelper`）：

```properties
# vcampus-server/src/main/resources/db.properties
# 从 db.properties.example 复制一份改，该文件不入库
db.url=jdbc:mysql://localhost:3306/vCampus?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
db.user=root
db.password=root
```

CI 用环境变量注入：`DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD`。`db.url` 整条给出时以它为准，否则由 `db.host`/`db.port`/`db.name` 拼出来。

## 表清单（26 张，按模块）

- **全局序列**：`tblGlobalSequence`
- **用户与认证**：`tblUserCredential`
- **学籍**：`tblStudentProfile`、`tblStudentModifyRequest`
- **课程与选课**：`tblCourse`、`tblCourseField`、`tblCourseSelection`、`tblScore`、`tblCourseStudent`、`tblTimeslot`、`tblCollege`、`tblCollegeField`、`tblTeacher`、`tblTeacherField`、`tblBuilding`、`tblClassroom`、`tblClassroomTag`
- **图书馆**：`tblBook`、`tblBorrow`、`tblLibraryAccount`、`tblReservation`
- **银行**：`tblBankAccount`、`tblBankTransaction`
- **商店**：`tblShop`、`tblShopItem`、`tblOrder`

`tblUserCredential` 是账户的唯一载体，其它模块的用户外键全部指向它的 `ucUuid`。

### 已删掉的表

设计书初稿里的六张表已删除：`tblUser`、`tblHumanInfo`、`tblUserHumanInfo`、`tblStudent`、`tblCourseSection`、`tblUserPurchaseRecord`。代码从不读写它们，其中 `tblUser` 还是个真陷阱 —— 它是六处外键的目标，而注册流程只写 `tblUserCredential`，于是任何真实账号去开户、选课、下单都会被数据库以 1452 拒掉（实测过）。设计书是初稿、随时可改，没有理由为了迁就它把死表留在库里。

## 关于账号与数据

脚本**不预置任何可登录账号**。登录只认 `tblUserCredential`，而这张表由管理员引导文件创建账号时写入（见 `AccountProvisioning` 与 `vcampus.admins.file`）。

商店、商品、课程目录这类业务数据也不预置，全部由界面创建。库里唯一不是「界面创建」的数据就是 `vCampus-data.sql` 里的引用数据（学院、教室、课表）。

## 测试用的库

服务端的真库测试不写开发库：测试启动时会把开发库的**表结构（含外键）**整体复制成 `<开发库名>_test`（`TestSchemaSetup`），本次 JVM 全部写进副本，每次运行重建。开发库只当结构模板、只读。

副本里一行数据也没有，所以 `TestSchemaSetup` 紧接着执行一遍 `sql/vCampus-data.sql` 把引用数据喂进去。测试要用的行必须来自脚本，测试代码不得在 Java 里「先补一行」：那样就把「库里缺必需数据」这个真实故障改写成测试内部的私事，部署到别的库上照样会炸。

外键必须跟着过来：测试库若只拷列与索引，那些「生产写入会被数据库拒掉」的引用完整性问题在测试里就会全部变成合法，测试反而给人虚假的安全感 —— 上面那个 1452 就是这么一直没被发现的。

因此本地跑测试前先确认开发库里已有完整表结构（即本脚本跑过一遍），否则测试按环境门控整体跳过。
