# 采用 Maven 多模块 + JDK 8 工具链，编译产物为 Java 7 字节码

课程要求"JDK 可使用高版本，但编译 level 必须为 1.7"。JDK 21 的 javac 最低只支持 `--release 8`，无法产出 1.7 字节码；实测本机 JDK 8（Temurin 8.0.492）以 `-source 1.7 -target 1.7` 可产出 major=51 的 Java 7 字节码。因此**编译工具链锁定 JDK 8**，JDK 21 仅作 IDE 运行时；项目采用 Maven 多模块（父工程 `vcampus` + `common/client/server` 三模块），Eclipse 中 compiler compliance 设为 1.7。

**Status**: accepted

**Considered Options**:

- 方案 A：JDK 21 + `--release 8` + 禁用 Java 8+ 语法 lint —— 产物非字面 1.7 字节码，且需额外维护语法拦截规则。
- 方案 B（采纳）：JDK 8 编译到 1.7 —— 字面满足课程要求，且 `-source 1.7` 会在编译期直接拦截 lambda/stream/var 等新语法，无需额外 lint。

**Consequences**: 团队不能使用 Java 8+ 语言特性；Maven 构建必须在 JDK 8 下运行（用 maven-enforcer-plugin 强制，防止误用其他 JDK 产出 8 字节码）；本地构建前需将 `JAVA_HOME` 指向 JDK 8。
