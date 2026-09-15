# 图书馆数据库接口对接


图书馆模块提供 `LibraryAccountDao`、`BookDao`、`BorrowDao`、`ReservationDao` 四个 Java 接口及对应 `XxxDaoMemory` 占位实现。业务服务只依赖接口；JDBC 实现、建表、索引、SQL、实体映射和连接管理由数据库同学在 `DbHelper` 合入后补充，业务 Service 不需要修改。

## 用户关联键迁移

`BorrowRecord.userId` 及 `BorrowDao` 的 userId 参数现在一律是服务器会话 UUID。用户名只用于登录/显示；旧数据如按用户名关联，需通过账户表用户名 → UUID 映射迁移借阅归属，并检查未知或重复映射。上线前同步更新两端和 DAO，不采用用户名/UUID 双查询回退。

## 接入方式

接口和内存占位实现位于 `vcampus-server/src/main/java/edu/seu/vcampus/server/library/`。当前正式入口注入 `LibraryDataSourceMemory`、`LibraryAccountDaoMemory`、`BookDaoMemory`、`BorrowDaoMemory` 和 `ReservationDaoMemory`，用于启动与演示。

应用组装层取得数据库侧提供的 `DataSource` 和四个 DAO 实例后，注入服务：

```java
LibraryService service = LibraryService.getInstance(
        dataSource, libraryAccountDao, bookDao, borrowDao, reservationDao);
LibraryModule.register(dispatcher, sessionManager, service,
        new BankLibraryFinePayment(sharedBankService), accountProvisioning);
```

以后接入 JDBC 时，数据库同学新增四个 `XxxDaoJdbc` 并通过 `DbHelper` 取得连接提供器，在正式入口替换五个 Memory 实例即可。四个 JDBC DAO 应访问同一数据库。必须通过五参数 `getInstance` 初始化完整服务；任一依赖为 `null` 都会立即失败。`LibraryService`、消息处理器和客户端不需要随数据库实现变化。

## 图书馆账户

`tblLibraryAccount` 是学生、教师与用户账户之间的 1:1 读者档案。管理员只维护馆藏，不建立读者账户。账户由 `LibraryAccountProvisioner` 在用户建号时自动创建，模块启动时也会幂等补齐既有师生账号；用户注销时软删除。

建议表结构如下，`userUuid` 必须建立唯一索引：

```sql
CREATE TABLE tblLibraryAccount (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  userUuid    VARCHAR(36) NOT NULL,
  status      VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
  borrowLimit INT         NOT NULL DEFAULT 30,
  createdAt   DATETIME    NOT NULL,
  updatedAt   DATETIME    NOT NULL,
  deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
  PRIMARY KEY (id),
  UNIQUE KEY ukLibraryAccountUser (userUuid)
);
```

账户只保存稳定状态与额度。当前借阅数、预约数和待缴金额从借阅及预约记录查询，不在账户表重复保存。

`sessionManager` 必须与登录认证服务共享。处理器验证 token 后取 `SessionEntry.getUuid()` 作为 `userId`，忽略客户端 sender。正式 `main` 入口当前注入 Memory 实现；集成测试或嵌入式启动可使用 `VCampusServerApp.startServer(port, service)` 注入其他实现。

## 方法与返回值

完整签名和边界约定以接口 Javadoc 为准。

| 接口方法 | 数据库侧职责 | 返回值 |
| --- | --- | --- |
| `LibraryAccountDao.findByUserUuid` | 按 UUID 查询账户，包含软删除记录 | `LibraryAccount`，不存在为 `null` |
| `LibraryAccountDao.insert` | 幂等建档并回填主键，UUID 唯一 | 新增成功为 `true` |
| `LibraryAccountDao.update` | 保存账户状态、额度和更新时间 | 更新成功为 `true` |
| `LibraryAccountDao.softDelete` | 用户注销时软删除读者账户 | 成功或已删除为 `true` |
| `BookDao.search` | 按 `BookQuery` 模糊搜索、排序并执行 limit/offset，只统计未下架图书 | `PageResponse<Book>`，含准确 total |
| `BookDao.findByIsbn` | 在传入连接上查询指定 ISBN | `Book`，不存在为 `null` |
| `BookDao.adjustAvailable` | 原子增减可借数量，确保 `0 <= availableCopies <= totalCopies` | 成功为 `true`，不存在或越界为 `false` 且不修改 |
| `BookDao.searchCatalog` | 按 `BookQuery` 分页查询全部馆藏，含已下架 | `PageResponse<Book>`，含准确 total |
| `BookDao.insertBook` | 原子录入图书，ISBN 唯一 | 新增成功为 `true`，重复为 `false` |
| `BookDao.updateBook` | 在锁定记录上修改资料和服务端计算的库存 | 更新成功为 `true` |
| `BookDao.withdrawBook` | 在锁定记录上逻辑下架，不删除借阅历史 | 标记成功为 `true` |
| `BorrowDao.findByUser` | 查询用户全部借阅记录，按借出时间降序 | `List<BorrowRecord>`，无结果为空列表 |
| `BorrowDao.hasActive` | 判断该用户是否尚未归还该书 | `boolean` |
| `BorrowDao.insert` | 保存未归还记录并生成主键，保障并发唯一性 | 正数 `long` 记录号 |
| `BorrowDao.findActiveById` | 按记录号和未归还状态查找，返回记录中的用户 UUID 供业务层鉴权 | `BorrowRecord`，不存在或已归还为 `null` |
| `BorrowDao.findById` | 查询含已归还记录，供罚款缴费校验 | `BorrowRecord`，不存在为 `null` |
| `BorrowDao.markReturned` | 原子保存归还时间、固化罚款金额和是否结清 | 更新成功为 `true` |
| `BorrowDao.renew` | 原子保存新到期日和续借次数 | 更新成功为 `true` |
| `BorrowDao.markFinePaid` | 原子保存已缴状态及银行流水号 | 首次更新成功为 `true` |
| `ReservationDao` 全部方法 | 维护等待、到馆、完成、取消、过期状态及 FIFO 查询 | 详见接口 Javadoc |

数据库检索接收已经规范化的 `BookQuery`：关键词为空表示不限制，字段为 `title`、`author`、`isbn` 或 `all`，页码从 1 开始、每页最多 100。DAO 必须在数据库中分页并计算 total；普通检索的结果与 total 都排除下架图书，馆藏管理检索则包含下架图书。消息入口对不支持的范围返回 400。具体格式见 [图书馆请求校验](library-request-validation.md)。

数据模型复用公共工程的 `LibraryAccount`、`Book`、`BorrowRecord` 与 `BookReservation`。借阅表还需保存 `renewalCount`、`fineAmount`、`finePaid`、`fineTransactionId`。预约表需保存申请、到馆和保留截止时间及 `ReservationStatus`。请保持 UUID 和 ISBN 原值。完整规则见 [读者借阅、预约与罚款规则](library-reader-rules.md)。

## 事务与并发约定

管理员馆藏管理及新增 `Book.withdrawn` 字段映射见 [馆藏管理对接说明](library-catalog-management.md)。`findByIsbn` 须包含下架图书并锁定行至事务结束；普通检索排除下架书，借出与修改/下架共享行锁，下架后仍允许正向归还库存。

学生和教师最多同时借阅 30 本；未归还（含逾期）占用额度，已归还不占用。当前处理器复用 `findByUser` 统计数量，并在同一 JVM 内串行检查及借还；跨服务器进程仍需数据库提供原子额度保障。界面规则、测试范围和真实数据库验收步骤见 [借阅额度与验收](library-borrow-quota.md)。

- `search`、`searchCatalog`、`findByUser` 不接收连接，实现自行取得和释放查询连接。
- 其他方法必须使用业务层传入的同一个 `Connection`，只关闭自己创建的查询资源，不得关闭连接、切换自动提交状态、提交或回滚事务。
- 借书时，业务层先检查图书和重复借阅，再扣减库存、插入记录，全部成功后提交。还书时先按记录号读取未归还记录，再将记录的用户 UUID 与当前会话 UUID 比较；代还返回 403，归属正确才标记归还并增加库存。数据访问失败或业务拒绝时回滚。
- 数据库实现须保证库存不会因并发操作越界、同一用户同一本书最多一条未归还记录、并发重复归还最多成功一次。`hasActive` 的预检查不能替代插入时的唯一性保障。
- 数据访问失败须抛出 `SQLException`，不得用空列表或 `false` 隐藏连接故障。接口实例会被多线程复用，不得把每次调用的连接存为共享字段。

现有消息层将 `SQLException` 转换为 500 及“图书馆服务暂时不可用”；业务异常沿用原状态码。预检查发现重复借阅返回 400；插入阶段才发现的并发重复借阅按数据库失败处理并回滚。

## 验证责任

业务层测试使用模拟接口与连接，验证借还书流程、事务提交/回滚及消息响应，不需要真实数据库。数据库同学实现后需补充集成测试，验证字段映射、搜索规则、排序、主键生成、事务原子性，以及库存、重复借阅、重复归还的并发约束；这些能力不能由模拟接口测试证明。
