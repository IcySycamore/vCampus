# 包结构：双端对称、按业务模块分包

双端（client / server）与 common 采用一致的「横切设施 + 业务模块」两级划分；客户端与服务端对应角色**同名同层**，共享契约集中放 common，业务实体按业务模块归位。

**Status**: accepted

**Decision**: 按下列结构组织代码（包名与类名前缀双端成对）。

```text
common.message      Message / MessageSender / MessageHandler（双端共享的协议契约）
common.network      MessageStream
common.constant     Command / StatusCode / NetworkConstant
common.random       RandomGen
common.util         Sha256Util
common.<业务>/        dto、entity（user / bank / library / student）

server.VCampusServerApp   入口：装配各模块 + 启动监听
server.network            ServerSocketListener / ServerMessageDispatcher /
                          ServerMessageSender / ServerMessageReceiverThread
server.thread             ThreadPoolManager
server.<业务>/             user / student / library / bank
                          （Module 装配 + Service + Handler + Dao）

client.VCampusClientApp   入口：装配各模块 + 建立/关闭连接
client.network            ClientSocketListener / ClientMessageDispatcher /
                          ClientMessageSender / ClientMessageReceiverThread
                          + 心跳、重连、连接配置、优雅关闭
client.handler            ClientMessageHandler / UiCallback /
                          ConnectionListener / UIUpdateHandler
client.<业务>/             user（学籍、图书馆、商店、银行同级并列）
client.view.**            UI 组件 / 对话框 / 页面 / 主题
```

**Consequences**

- 双端同名角色一一对应（`ServerXxx` ↔ `ClientXxx`）：分发器、发送器、接收线程、连接监听四类职责在两端各自可寻。
- 协议契约单一来源：`Message` / `MessageSender` / `MessageHandler` 同处 `common.message`，不会出现「改一端忘另一端」；客户端特有的 `ClientMessageHandler` 因多一个 UI 回调参数，留在 `client.handler`。
- 业务模块自装配：入口只调用 `<业务>Module.register(dispatcher, 共享依赖)`，模块把处理器登记与自身接线规则带走。
- 连接生命周期只在入口：`VCampusClientApp` / `VCampusServerApp` 持有连接对象，分发器只依赖注入的 `MessageSender`。
- 测试包与被测类同包、同名（如 `server.network.ServerMessageDispatcherTest`）。
- 不采用 `biz / view / vo / dao` 横向分层——与 ADR-0003 的纵向模块划分保持一致。
