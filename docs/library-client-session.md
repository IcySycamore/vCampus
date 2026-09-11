# 图书馆客户端连接与登录身份

点击“登录”会调用现有认证协议，只有服务器确认登录成功后才进入在线主窗口。连接失败或密码错误会停留在登录窗口；界面选择的角色仅作为请求提示，在线主窗口使用服务器返回的真实角色。

调整界面时可直接点击“离线预览”，无需填写账号密码、启动服务器或连接数据库。此时窗口标题及侧栏均标记“离线预览”，可切换工作台、图书馆等页面；不会创建 token 或发送业务请求。输入的用户名和选择的角色仅用于预览显示。若点击预览时有登录请求正在等待，关闭登录窗口会释放该请求的连接。

## 已接入的流程

1. `LoginController` 在后台创建 `ClientSession` 并连接服务器，使用 `Command.USER_LOGIN`（100）请求盐和 nonce。
2. 客户端复用 `Sha256Util` 计算 `sha256(nonce + sha256(salt + password))`，通过 `Command.USER_LOGIN_VERIFY`（110）提交证明。密码不发送到网络，输入框在提交后清空，密码字符数组在登录结束时清零。
3. 验证成功后，从 `LoginResponse.m_token`、`m_role` 保存会话与真实角色。token 仅保存在当前进程内存中。
4. `LoginFrame → MainFrame → MainContentPanel → LibraryPanel` 传递同一个 `ClientSession`，后续检索和借还书复用登录时的连接。进入图书馆页面自动查询馆藏和个人借阅记录。
5. `LibraryRequestTask` 在后台发送请求，由会话统一设置 `Message.token` 和已通过登录验证的用户名。收到的图书馆响应回到对应页面，在 Swing 事件线程更新表格；其他模块响应和心跳不会触发图书馆刷新。
6. 收到 401 或连接断开时清除登录身份，主窗口返回带提示的登录窗口。底层连接自动恢复也不会恢复旧登录状态；再次登录使用新会话。关闭窗口时异步释放连接与后台网络资源。

每一步登录响应最多等待 10 秒；建立连接仍使用 `ClientNetworkConfig` 中的连接、读取超时与有限重试策略。现有认证响应没有回传请求 uid，因此登录按命令区分两个阶段，每个会话只允许一次登录尝试，失败后关闭连接；图书馆响应继续保留原请求 uid。

## 服务器地址

默认连接 `127.0.0.1:8888`。连接其他电脑时，在客户端 JVM 启动参数中设置：

```text
-Dvcampus.server.host=服务器地址 -Dvcampus.server.port=8888
```

这两个参数应放在 `java -jar` 的 `-jar` 之前。

## 服务器接入要求

`LibraryMessageHandler` 已实现统一 `MessageHandler` 接口，构造时需要注入 `LibraryService` 与认证模块共享的 `SessionManager`。图书馆的四个命令都需要有效 token；借阅身份取自该会话中的真实登录名，忽略请求的 `sender`。

当前图书馆 DAO 的字符串 `userId` 对应登录名，包含前导零时必须原样保留；账户 UUID 与登录名不是同一种标识。若数据库采用 UUID 作为关联键，需要数据库与认证同学协调映射。数据库接口见 [数据库对接说明](library-database-interface.md)。

应用组装层在取得数据源和 DAO 实现后，需要共享同一会话管理器并注册处理器，例如：

```java
LibraryService library = new LibraryService(dataSource, bookDao, borrowDao);
LibraryMessageHandler handler = new LibraryMessageHandler(library, sessionManager);
dispatcher.register(MessageType.LIBRARY_SEARCH, handler);
dispatcher.register(MessageType.LIBRARY_LIST_BORROWS, handler);
dispatcher.register(MessageType.LIBRARY_BORROW, handler);
dispatcher.register(MessageType.LIBRARY_RETURN, handler);
```

同一分发器还需注册认证处理器的 100、110 等命令，并由服务器连接线程驱动收发。目前 `VCampusServerApp` 仍在接受连接后直接关闭，正式入口的线程池/消息分发组装尚未完成；DAO 实现也由数据库同学提供。因此当前“登录”尚不能通过正式入口完成认证和数据库业务，可先使用“离线预览”调整界面。

## 验证范围

- `ClientSessionTest`：验证挑战应答、服务器真实角色、密码数组清理、token 与用户名注入、登录拒绝、缺失 token、401 和断线后的身份失效。
- `LibrarySessionIntegrationTest`：通过本机 Socket 协议测试对端完成登录，然后在同一条连接上加载图书馆页面的馆藏与借阅表格。此测试不使用数据库，也不启动正式服务器入口。
- `LibraryMessageHandlerTest`：验证统一分发器调用、保留 uid、拒绝无效/已登出 token，并确保伪造 sender 不能改变查询或借还书的用户归属。
- `LibraryServiceTest`：保留模拟 DAO 的借还书业务与事务测试。

现有注册和修改密码窗口仍需用户管理模块接入真实服务；它们的界面提示不代表数据已经写入账户存储。
