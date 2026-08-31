# Issue 框架（提案模板）

> 目的：让每个 issue 都清晰回答"做什么、为什么、改动多大、做完什么样"，便于组长审查、排期与并行认领。
> 适用：项目看板上的 issue、与 PR 关联的实现工单。

## 模板

### 类型

- [ ] enhancement 新功能/增强
- [ ] bug 缺陷修复
- [ ] refactor 重构/整理
- [ ] db 数据库结构
- [ ] docs 文档
- [ ] build/ci 构建/CI

### 目标

一句话说清：完成这个 issue 后系统能做什么、或解决什么问题。

### 已有基础

现状盘点：相关代码/接口/依赖已经存在哪些（可列文件或链接），避免重复造轮子。

### 预期改动量

- 涉及工程：vcampus-common / vcampus-server / vcampus-client / sql / CI
- 预计新增/修改文件数：约 N 个
- 规模：小（<3 文件）/ 中（3–8）/ 大（>8）

### 预期效果

验收时能看到什么（尽量可勾选）：
- [ ] 行为/命令码表现
- [ ] 测试结果
- [ ] 界面/运行效果

### MVP 截图（如有）

界面或运行效果截图路径/链接（有 UI 改动时建议附上，方便快速确认）。

---

## 示例（用户管理 · 登录闭环）

### 类型
- [x] enhancement 新功能/增强

### 目标
打通"登录 → 服务器校验 → 进入 OA 工作台"的完整闭环，账号密码正确且角色匹配即可登录。

### 已有基础
- `vcampus-common/user`：HumanInfo / User / Student 已建；`dto/` 的 SaltRequest/LoginRequest/LoginResult 已建。
- `vcampus-client`：UI 外壳（LoginFrame/MainFrame）已有；ClientSocket/MessageReceiver 已有。

### 预期改动量
- 涉及工程：vcampus-common、vcampus-server、vcampus-client
- 预计新增/修改文件：约 6 个
- 规模：中

### 预期效果
- [x] 用 `001/1`（学生）登录成功，进入 OA 工作台
- [x] 密码错误 / 角色不符 / 已禁用均被拒绝并提示
- [x] 单测（UserBiz 三分支）+ 集成测试通过

### MVP 截图（如有）
`docs/original-docs/demo-login.png`
