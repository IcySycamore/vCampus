# 银行账户绑定与显式开户

银行账户归属使用 `String ownerUuid`，语义与 `SessionEntry.getUuid()` 一致。
这是用户存储分配的稳定主键；银行不生成用户 ID，不把登录名、请求 sender、
token、学号的数值转换或哈希当作该主键。当前 main 尚无独立学生学号模型。
`BankAccount.ownerUuid` 初始化后不可改绑。账户号独立生成，账户归属查询始终使用 ownerUuid。

## 开户和资金操作

| 命令                                | 请求 data                           | 行为                                                     |
| ----------------------------------- | ----------------------------------- | -------------------------------------------------------- |
| 604 `Command.BANK_ACCOUNT_OPEN`     | BankOpenRequest                     | 校验独立校园会话并设置银行密码，返回 BankAccountResponse |
| 601 `Command.BANK_ACCOUNT_QUERY`    | null                                | 只查询已有账户                                           |
| 602 `Command.BANK_RECHARGE`         | BankRechargeRequest                 | 给已有账户充值并记流水                                   |
| 603 `Command.BANK_TRANSACTION_LIST` | null 或 BankTransactionQueryRequest | 查询已有账户流水                                         |

四条命令统一定义在 `common.constant.Command` 中。
未开户业务码也统一使用 `Command.BANK_ACCOUNT_NOT_OPENED = "B100"`。
所有网络请求必须携带非空 token，归属由可信 BankIdentityResolver 返回的 ownerUuid 决定；
请求载荷不接受目标 ownerUuid。空 token 或未能取得可信非空 UUID 返回 401。

`BankService.openAccount(ownerUuid)` 创建正常、零余额账户，记录真实调用时的开户时间。
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
内部付款使用 `consumeWithPassword(ownerUuid, password, amount, relatedOrderId, description)`。
旧 `consume(...)` 仅兼容未设置密码的内部账户；新开户账户必须验证银行密码。
内部 `cashback(ownerUuid, amount, relatedOrderId, description)` 供商城等业务给指定用户返现，
返现会增加余额并写入 `CASHBACK` 流水；用户必须先开户。
资金更新与流水追加使用同一账户锁，失败的金额校验或余额不足不修改资金和流水。
分页保持原有写入顺序，默认每页 20 条，最多 100 条，可按充值/消费类型过滤。

## 对接边界

应用组装层可调用 `BankModule.register(dispatcher, bankService, identityResolver)`，
注册四条命令。当前正式服务器入口已完成注册；未修改认证模块或 SQL。
处理器不信任客户端 sender，也不会自行创建会话池。

当前入口提供的 BankIdentityResolver 从共享 SessionManager 校验 token 后获取
SessionEntry.uuid。银行不信任 Message.sender，不维护第二套用户身份映射。

银行模型的 ownerUuid 使用 String，BankAccount 的 serialVersionUID 为 3；
使用银行模型的两端需要同步构建。旧账户对象的 Java 序列化数据不能直接读入新版。
查询不会自动开户，旧调用方须先显式开户；自动开户不再是查询的副作用。

## 已覆盖测试

- BankAccountTest / BankAccountOwnershipTest：稳定主键绑定、禁止改绑、序列化及资金规则。
- CommandTest：四条银行命令的取值正确，且所有公共命令码互不冲突。
- BankStoreJdbcTest：账户/凭据/流水与流水序号的落库读取（真库集成，连不上则跳过）。
- BankAdminWriteThroughTest：管理端冻结与重置密码确实写库（用假 BankStore 断言写入发生）。
- BankNotOpenedResponseTest：覆盖未开户业务码和 BankAccountNotOpenedException，
  验证完整消息序列化往返保留异常类型、业务码和提示。

## 后续实现空间

账户与流水已落库（`BankStoreJdbc`），但 `BankService` 仍在内存里持有账户锁与业务规则，
启动时把库里的状态读回内存。当前是单服务端实例，进程内锁足够；若将来要多实例共享库，
需要把余额变动改成数据库事务。

后续可补充充值与退款幂等、账户冻结/解冻的权限入口，以及账单时间过滤。充值仍是直接加余额的演示逻辑，尚无支付确认；
充值和旧内部消费请求不会自动去重；带密码的消费按订单号去重，见下文。

## 银行密码与开户表单（2026-09-14）

银行客户端调用：

```java
bank.openAccount(username, campusPasswordChars, bankPasswordChars);
```

开户界面位于 `client/view/bank/OpenAccountDialog.java`，由 BankPanel 打开。
输入当前校园账号、校园登录密码、8–64 字符银行密码和确认密码。
查询、充值、流水不要求银行密码；页面用 UiTasks 执行网络请求。

开户复用原 100/110 挑战应答流程，通过一次性 UserService 实例拿到独立验证会话，
不替换共享用户服务的 token。604 携带 BankOpenRequest（用户名、验证 token、盐、摘要），
服务端用原请求的可信 ownerUuid 核对验证会话，禁止直接提交原 token、跨用户验证和重复使用。
成功处理后销毁验证会话；客户端也尽力清理失败流程的临时会话。
独立会话证明另一份有效的登录凭据；现有用户协议未提供专门的银行开户挑战，
因此这里不能保证该令牌一定是在最近一次开户操作中签发的。

银行使用 PBKDF2-HMAC-SHA256（210000 次，128 位随机盐，256 位摘要）。
摘要只在服务器 BankRecord/BankCredential 中保存，不进入账户响应或流水。
开户摘要属于敏感认证材料，现有 TCP 协议仍没有 TLS；本改动不修改网络层。
密码与账户一样保存在内存中，重启服务端会丢失，尚未实现数据库持久化。
重复开户不会覆盖已设置的密码；已有无密码的内部账户可经完整验证补设一次。

商店服务端对接方法：

```java
bank.consumeWithPassword(ownerUuid, bankPasswordChars, amount, orderUuid, description);
```

调用方必须从已认证会话获取 ownerUuid，从服务端订单计算金额。
密码验证和余额、流水变更处于同一账户锁内；错误密码不扣款、不写流水。
连续 5 次错误后暂停验证一分钟。相同用户、订单号、金额重复支付返回原流水，
相同订单改变金额则拒绝。调用方用完后清除密码字符数组。
已设密码的账户调用旧 consume(...) 会拒绝，不允许绕过密码。
内部 openAccount(ownerUuid) 仅保留给已有可信服务端调用/测试，不面向网络开户；
旧无密码账户仍保留旧 consume 行为，正式网络开户必须使用完整 BankOpenRequest。
商店页面、商店协议及银行密码修改/找回均未在本次实现。

BankFlowIntegrationTest 覆盖真实入口的错误校园密码、主会话保留、开户、充值和重新登录
（走 `VCampusServerApp.startServer` 的真实装配路径，连真库）。
