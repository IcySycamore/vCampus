# 图书馆数据库接口对接

图书馆模块提供 `BookDao`、`BorrowDao` 两个 Java 接口及业务服务，具体数据库实现由负责数据库的同学完成。建表、索引、初始化数据、SQL、实体映射、连接管理和 DAO 实现均属于数据库侧交付；本模块不附带数据库实现。

## 接入方式

接口位于 `vcampus-server/src/main/java/edu/seu/vcampus/server/module/library/`。数据库同学分别编写实现这两个接口的类，方法均为公开接口，可在其他包实现。

应用组装层取得数据库侧提供的 `DataSource`、`BookDao` 和 `BorrowDao` 实例后，注入服务：

```java
LibraryService service = new LibraryService(dataSource, bookDao, borrowDao);
LibraryMessageHandler.register(ClientThread.getDispatcher(), service, sessionManager);
```

原先自动创建 DAO 的 `LibraryService(DataSource)` 构造方法已移除，调用方需要显式提供三个依赖。两个 DAO 应访问同一数据库，并能使用传入的数据源连接。目前仓库没有默认 DAO 实现；完成实现和组装后才能访问真实数据。

`sessionManager` 必须与登录认证服务共享。图书馆处理器验证 token，并从该会话取出登录名作为当前字符串 `userId`，忽略客户端填写的 `sender`。这与现有借阅接口的字符串用户标识一致；若数据库最终使用账户 UUID，需由认证与数据库负责同学统一调整映射，不能直接混用两种标识。

## 方法与返回值

完整签名和边界约定以接口 Javadoc 为准。

| 接口方法 | 数据库侧职责 | 返回值 |
| --- | --- | --- |
| `BookDao.search` | 按书名、作者、分类或全部字段模糊搜索，按书名升序 | `List<Book>`，无结果为空列表 |
| `BookDao.findByIsbn` | 在传入连接上查询指定 ISBN | `Book`，不存在为 `null` |
| `BookDao.adjustAvailable` | 原子增减可借数量，确保 `0 <= availableCopies <= totalCopies` | 成功为 `true`，不存在或越界为 `false` 且不修改 |
| `BorrowDao.findByUser` | 查询用户全部借阅记录，按借出时间降序 | `List<BorrowRecord>`，无结果为空列表 |
| `BorrowDao.hasActive` | 判断该用户是否尚未归还该书 | `boolean` |
| `BorrowDao.insert` | 保存未归还记录并生成主键，保障并发唯一性 | 正数 `long` 记录号 |
| `BorrowDao.findActiveById` | 同时按记录号、用户归属、未归还状态查找 | `BorrowRecord`，不匹配为 `null` |
| `BorrowDao.markReturned` | 仅将未归还记录原子更新为已归还 | 更新成功为 `true`，不存在或已归还为 `false` |

搜索关键词去除首尾空白，`null` 或空白表示不限制关键词；字段支持 `title`、`author`、`category`、`all`，其他值按 `all` 处理。这些规范化规则由 `BookDao.search` 的实现履行。

数据模型复用公共工程的 `Book` 与 `BorrowRecord`，无需新增数据库传输对象。`Book.isbn` 是字符串；借阅记录使用 `Long id`、字符串 `userId` 和 ISBN，并保留借出时书名快照。请保持用户标识原值（例如 `001`），不要自行转成数字。借出与应还时间由业务层给出，默认借期为 30 天；`returnedAt == null` 表示未归还。

## 事务与并发约定

学生最多同时借阅 3 本，教师最多 5 本；未归还（含逾期）占用额度，已归还不占用。当前处理器复用 `findByUser` 统计数量，并在同一 JVM 内串行检查及借还；数据库同学需保证查询返回完整、最新的记录。跨服务器进程的原子额度保障需进一步在数据库事务内实现。界面规则、测试范围和真实数据库验收步骤见 [借阅额度与验收](library-borrow-quota.md)。

- `search`、`findByUser` 不接收连接，实现自行取得和释放查询连接。
- 其他方法必须使用业务层传入的同一个 `Connection`，只关闭自己创建的查询资源，不得关闭连接、切换自动提交状态、提交或回滚事务。
- 借书时，业务层先检查图书和重复借阅，再扣减库存、插入记录，全部成功后提交。还书时，先校验记录归属，再标记归还、增加库存，全部成功后提交。数据访问失败或业务拒绝时回滚。
- 数据库实现须保证库存不会因并发操作越界、同一用户同一本书最多一条未归还记录、并发重复归还最多成功一次。`hasActive` 的预检查不能替代插入时的唯一性保障。
- 数据访问失败须抛出 `SQLException`，不得用空列表或 `false` 隐藏连接故障。接口实例会被多线程复用，不得把每次调用的连接存为共享字段。

现有消息层将 `SQLException` 转换为 500 及“图书馆服务暂时不可用”；业务异常沿用原状态码。预检查发现重复借阅返回 400；插入阶段才发现的并发重复借阅按数据库失败处理并回滚。

## 验证责任

业务层测试使用模拟接口与连接，验证借还书流程、事务提交/回滚及消息响应，不需要真实数据库。数据库同学实现后需补充集成测试，验证字段映射、搜索规则、排序、主键生成、事务原子性，以及库存、重复借阅、重复归还的并发约束；这些能力不能由模拟接口测试证明。
