# 公共代码（common）变更规则：加法向后兼容 + 剪枝协议

`vcampus-common` 是所有纵向模块共享的契约，任何改动都影响全局。为保证 6 人并行开发时冲突最小、且不破坏他人模块，特约定：

- **加法**：新增字段 / 常量 / 方法 / 命令码必须**向后兼容**——只增、不改、不删现有内容，可随时合入 main。
- **审核**：任何 `vcampus-common/` 与 `sql/` 的改动，由 `.github/CODEOWNERS` 自动请求组长 review；组长把关协议一致性与兼容性（ADR-0003）。
- **剪枝**（清理 / 删除 / 改名）：属**协调式操作**，仅在合流点（main 干净、各分支已合并）执行：
  1. 先 `@Deprecated` 软删并注明"待移除"，合入 main（不破坏任何人）；
  2. 宽限期后，用 Find Usages / grep 确认无引用；
  3. 再以独立 refactor PR 真删；以编译器 + 组长 review + CI 三检查为安全网。
- **协议**（Message / 命令码）尽量早冻结（ADR-0006）；剪枝时用 Find Usages 全量核查引用。

**Status**: accepted

**Consequences**: 个人功能分支很少因 common 产生冲突；common 的清理集中在里程碑、受控且低风险；公共契约保持整洁而不过度膨胀。
