## PR 类型

- [√] enhancement 新功能/增强
- [ ] bug 缺陷修复
- [ ] refactor 重构/代码整理
- [ ] docs 文档
- [ ] build/ci 构建/CI 配置
- [ ] db 数据库结构
- [ ] other 其他

## 详细说明

本次 PR 初步构建 `vcampus-server` 的服务端线程管理模块，主要涉及客户端连接处理与线程池管理两个核心组件：

### 1. **`ClientThread` 核心逻辑完善**：
   - 基于 `MessageStream` 构建请求/响应通信循环，确保消息的可靠收发。
   - 补充对端断开（`EOFException` / `SocketException`）时的平滑退出捕获，避免抛出未捕获异常导致线程崩溃。
   - 增加业务逻辑异常隔离（`try-catch`），确保单个请求报错不影响底层 Socket 状态与工作线程的持续服务能力。
   - 规范 `try-finally` 资源清理逻辑，确保连接关闭时消息流与 Socket 均能安全释放，防止资源泄漏。

### 2. **`ThreadPoolManager` 线程池重构**：
   - 按照 ADR 0006 规范，使用显式参数的 `ThreadPoolExecutor` 替代无界线程池，防止高并发场景下的内存溢出（OOM）。
   - 动态计算核心线程数为 CPU 核数的 2 倍，最大线程数为核心线程数的 2 倍，配合 256 容量的有界队列。
   - 增加自定义 `ThreadFactory`，生成统一格式（`vcampus-worker-%d`）的线程名称，便于诊断、日志追踪与性能监控。
   - 实现优雅关闭（Graceful Shutdown）机制，支持配置超时等待、阶段式强制关闭与中断恢复，避免服务突然中断。

### 3. **`RequestDispatcher` 接口定义**：
   - 定义简洁的请求分发接口，作为业务处理层的抽象入口。
   - 支持异常穿透，允许业务层异常在通信层被捕获与记录。

### 4. **规范与文档对齐**：
   - 按照 Javadoc 规范补充了类、方法、参数及异常注释，提高代码可维护性。
   - 补全了对应模块的单元测试套件，确保线程管理的正确性与健壮性。

## 改动文件

### 核心线程管理模块
- vcampus-server/src/main/java/edu/seu/vcampus/server/thread/ClientThread.java（103 行）
- vcampus-server/src/main/java/edu/seu/vcampus/server/thread/ThreadPoolManager.java（123 行）
- vcampus-server/src/main/java/edu/seu/vcampus/server/handler/RequestDispatcher.java（15 行）

### 基础网络通信框架
- vcampus-server/src/main/java/edu/seu/vcampus/server/network/MessageStream.java（消息序列化/反序列化）
- vcampus-server/src/main/java/edu/seu/vcampus/server/network/ServerSocketListener.java（Socket 监听与连接接受）
- vcampus-server/src/main/java/edu/seu/vcampus/server/VCampusServerApp.java（启动入口，修改）

## 对应测试

- vcampus-server/src/test/java/edu/seu/vcampus/server/thread/ClientThreadTest.java（覆盖 ClientThread 生命周期与消息通信）
- vcampus-server/src/test/java/edu/seu/vcampus/server/thread/ThreadPoolManagerTest.java（覆盖 ThreadPoolManager 单例、并发执行与资源管理）
- vcampus-server/src/test/java/edu/seu/vcampus/server/network/MessageStreamTest.java（覆盖消息序列化与反序列化）
- vcampus-server/src/test/java/edu/seu/vcampus/server/network/ServerSocketListenerTest.java（覆盖 Socket 监听与连接接受）

## 自查清单（检查后全选）

- [√] 每个类/接口都有文档注释（Javadoc），每个方法有适当注释
- [√] 每个 Java 文件不超过 200 行
- [√] 已在「改动文件」列出改动路径，并在「对应测试」标注覆盖测试（可多文件对应一个测试）
- [√] JDK 8 下运行本地 `mvn test` 全部通过（9 + 1 + 6 = 16 个测试全部通过）
- [√] 无 TODO / FIXME / XXX 残留
- [√] 如涉及数据库，已同步更新 `sql/vCampus.sql` 并说明影响（本 PR 不涉及数据库）
- [√] 如涉及文档/接口/协议，已说明影响范围（见下方「影响范围」）

## 影响范围

### 仅影响 vcampus-server 模块

- **`vcampus-server` 网络与并发框架**：`ClientThread` 与 `ThreadPoolManager` 是服务端并发模型的基础设施，所有后续业务处理器（biz 层）都将通过这两个组件完成并发请求处理。
- **`RequestDispatcher` 接口**：定义了业务处理层的抽象入口，后续所有 biz 实现类需实现此接口。
- **对 vcampus-common 的依赖**：仅使用了 `Message` 类，无新增依赖。
- **对其他模块无直接影响**：当前 PR 未涉及 vcampus-common 或 vcampus-client 的改动。

### 后续影响

- 后续业务模块（如用户管理 biz、课程管理 biz 等）需通过实现 `RequestDispatcher` 接口来集成本框架。
- 服务启动逻辑（见 VCampusServer 或类似主类）需配置 `ThreadPoolManager` 的关闭钩子，确保优雅关闭。

## 关联

- Closes #（如适用，填写对应 issue 号）

## 截图

（当前为基础设施 PR，无 UI 变化，可跳过）

### 测试运行结果概览
```
Reactor Summary for vCampus 虚拟校园系统 1.0.0:
[INFO] vCampus 虚拟校园系统 ..................................... SUCCESS
[INFO] vCampus Common ..................................... SUCCESS
[INFO] vCampus Client ..................................... SUCCESS
[INFO] vCampus Server ..................................... SUCCESS
[INFO] BUILD SUCCESS
```

---

**测试细节**：
- `ThreadPoolManagerTest#testSingleton`：验证单例模式正确
- `ThreadPoolManagerTest#testConcurrentExecution`：验证 20 个并发任务在 3 秒内全部执行完毕
- `ClientThreadTest#testClientThreadLifecycle`：验证客户端连接、消息接收、业务分发、响应返回的完整生命周期

