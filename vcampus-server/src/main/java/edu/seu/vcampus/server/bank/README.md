# 银行账户绑定与显式开户

银行账户归属使用 `Long userId`，语义与 `common.user.User.getUserId()` 一致。
这是用户存储分配的稳定主键；银行不生成用户 ID，不把登录名、请求 sender、
token、学号的数值转换或哈希当作该主键。当前 main 尚无独立学生学号模型。
`BankAccount.userId` 初始化后不可改绑。账户号独立生成，账户归属查询始终使用 userId。

## 开户和资金操作

| 命令 | 请求 data | 行为 |
| --- | --- | --- |
| 604 `Command.BANK_ACCOUNT_OPEN` | null | 显式开户，返回 BankAccountResponse |
| 601 `Command.BANK_ACCOUNT_QUERY` | null | 只查询已有账户 |
| 602 `Command.BANK_RECHARGE` | BankRechargeRequest | 给已有账户充值并记流水 |
| 603 `Command.BANK_TRANSACTION_LIST` | null 或 BankTransactionQueryRequest | 查询已有账户流水 |

四条命令统一定义在 `common.constant.Command` 中。
未开户业务码也统一使用 `Command.BANK_ACCOUNT_NOT_OPENED = "B100"`。
所有网络请求必须携带非空 token，归属由可信 BankIdentityResolver 返回的 userId 决定；
请求载荷不接受目标 userId。空 token 或未能取得可信正数主键返回 401。

`BankService.openAccount(userId)` 创建正常、零余额账户，记录真实调用时的开户时间。
同一用户重复或并发开户返回同一个账户，不重置余额、状态、开户时间或流水。
开户操作本身不产生资金流水。调用方负责校验身份、用户存在及开户资格。

查询、充值、消费、查流水均要求已开户；不存在时抛出
`common.bank.exception.BankAccountNotOpenedException`。
通过消息接口调用时，BankMessageHandler 返回 `statusCode = B100`（未开户），
并将同一异常对象放在 `Message.data` 中；不使用通用 404，也不会丢弃异常信息。
异常可序列化，客户端共享 common.bank 即可识别类型并显示“银行账户未开户，请先开户”。
对象流反序列化不会自动抛异常，客户端可判断 data 的异常类型，提示用户或主动抛出。
异常不携带服务端调用栈。服务层直接调用仍按普通 Java 异常捕获。
无效金额/请求返回 400，账户状态禁止操作返回 403，未预期错误返回 500。
内部 `consume(userId, amount, relatedOrderId, description)` 继续供其他业务调用。
内部 `cashback(userId, amount, relatedOrderId, description)` 供商城等业务给指定用户返现，
返现会增加余额并写入 `CASHBACK` 流水；用户必须先开户。
资金更新与流水追加使用同一账户锁，失败的金额校验或余额不足不修改资金和流水。
分页保持原有写入顺序，默认每页 20 条，最多 100 条，可按充值/消费类型过滤。

## 对接边界

应用组装层可调用 `BankModule.register(dispatcher, bankService, identityResolver)`，
注册四条命令。该方法在 bank 包内；这次没有修改服务器入口、认证模块或 SQL。
处理器不信任客户端 sender，也不会自行创建会话池。

目前 SessionManager 仅提供 username 和 role，没有真实用户主键；UserRepository
也没有 username 到 userId 的查询能力。因此生产用 BankIdentityResolver 仍需
认证/用户模块提供主键能力后接入。无法取得该主键时应拒绝请求，禁止临时造一个 ID。
测试注入的 resolver 只验证银行契约，不代表已经接通真实登录或学生资格校验。

银行模型的 userId 从 String 改成 Long，BankAccount 的 serialVersionUID 改为 3；
使用银行模型的两端需要同步构建。旧账户对象的 Java 序列化数据不能直接读入新版。
查询不会自动开户，旧调用方须先显式开户；自动开户不再是查询的副作用。

## 已覆盖测试

- BankAccountTest / BankAccountOwnershipTest：稳定主键绑定、禁止改绑、序列化及资金规则。
- CommandTest：四条银行命令的取值正确，且所有公共命令码互不冲突。
- BankServiceTest：独立开户、重复开户、未开户拒绝、账户隔离、消费和流水分页。
- BankConcurrencyTest：并发开户唯一、并发充值不丢余额、并发扣款不透支。
- BankMessageHandlerTest：覆盖 BankModule 注册、BankIdentityResolver 契约、
  未开户异常和 B100 的消息传递，及开户/充值/查询/流水的分发流程。
- BankNotOpenedResponseTest：覆盖未开户业务码和 BankAccountNotOpenedException，
  验证完整消息序列化往返保留异常类型、业务码和提示。

## 后续实现空间

当前仍为内存实现，重建 BankService 或重启进程后账户与余额丢失。
真实持久化应使用稳定 userId 外键及唯一约束，并在事务中更新余额和流水。
用户主键在数据库重新初始化后的复用问题也需要数据层约束，不能由 bank 自造映射规避。

优先补齐真实身份对接与数据库持久化，再考虑订单幂等扣款、退款、账户冻结/解冻的
权限入口，以及账单时间过滤。充值仍是直接加余额的演示逻辑，尚无支付确认；
重复充值/消费请求也不会自动去重。上述扩展没有在本次修改中实现。
