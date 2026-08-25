# 通信协议：Message 信封 + Serializable + 命令码；Server 端线程池模型

客户端与服务器端通过统一的 **Message 信封**通信（uid / name / type / statusCode / data / sender），所有传输对象实现 `java.io.Serializable`，两端共享 `vcampus-common` 中同一份类定义以保证一致性（课程硬性要求）。命令码与状态码以常量统一维护，由组长审核保证协议一致。Server 端采用 **ClientThreadMan 线程池**管理"每客户端一线程"，并处理心跳与异常断开；Client 端负责连接建立、对象流收发与断线处理。

**Status**: accepted

**Considered Options**:

- 直接传输 JSON 文本：需自建序列化约定，且课程 DEMO 明确对象流 + Serializable，不采纳。
- 每客户端一线程 + 管理池（采纳）：满足课程"多客户端处理 + 线程管理"要求，实现直观、可测。

**Consequences**: 协议变更影响所有模块，Message 与命令码的改动必须走组长审核；网络三人组（Server 端 / Client 端 / 协议）应在早期冻结 v1 协议，再并行开发各业务模块。
