# 图书馆读者借阅、预约与罚款规则

更新日期：2026-09-14。

## 业务规则

- 在校学生和教师最多同时借阅 30 册，管理员只维护馆藏。
- 学生和教师建号时自动建立 1:1 图书馆账户，保存账户状态、借阅上限和建档时间；用户注销时软删除。暂停或已删除的账户不能新增借阅。
- 首次借期 30 天。无人预约时最多续借 2 次，每次从本次到期日继续增加 30 天。
- 没有公开可借馆藏时可预约，同一用户同一 ISBN 只允许一条有效预约，按申请时间先后排队。
- 图书归还后，系统将馆藏锁定给队首预约。到馆保留 15 天；到期未借自动释放并转给下一位。
- 逾期图书不能续借。还书时按每册每天 0.10 元固化滞纳金，不足一天按一天计算。
- 有逾期未还图书或未缴滞纳金时不能继续借书。缴费使用现有校园银行消费能力。

## 协议

| 命令 | 请求 | 响应 |
| --- | --- | --- |
| 404 `LIBRARY_RENEW` | `RecordRef` | `BorrowRecord` |
| 412 `LIBRARY_RESERVE` | `BookRef` | `BookReservation` |
| 413 `LIBRARY_LIST_RESERVATIONS` | `null` | `List<BookReservation>` |
| 414 `LIBRARY_CANCEL_RESERVATION` | `ReservationRef` | `BookReservation` |
| 415 `LIBRARY_PAY_FINE` | `RecordRef` | `BorrowRecord` |
| 416 `LIBRARY_ACCOUNT_QUERY` | `null` | `LibraryAccount` |

所有身份都从 token 对应的 `SessionEntry` 取得；客户端不发送或缓存另一份用户 UUID。

## 库存与支付一致性

预约变为 `READY` 时立即从公开可借库存扣除一册，预约人借书时直接消费这册锁定库存。取消或过期会先释放，再在同一事务中尝试晋级下一位。DAO 实现必须按接口 Javadoc 锁定图书和预约行，确保 FIFO 状态更新与库存变化一起提交或回滚。

还书与罚款计算在同一事务中完成。支付业务号固定为 `LIBRARY_FINE:<borrowRecordId>`，图书馆通过银行已有的 `consume` 接口扣款。用户需要先通过银行 604 开户并充值，余额不足、未开户或账户不可用时缴费失败，借阅记录仍保持待缴状态。支付结果写回失败时需要人工按业务号核对银行流水，避免重复扣款。

当前仓库已经完成协议、双端业务、Swing 入口、银行适配和 DAO 契约；`BookDao`、`BorrowDao`、`ReservationDao` 的 JDBC 实现及生产数据源仍由数据库侧接入，见 [图书馆数据库接口对接](library-database-interface.md)。
