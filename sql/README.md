# vCampus 数据库配置说明

本目录包含 vCampus 项目的数据库初始化脚本和配置说明。

---

## 📁 文件说明

- **vCampus.sql** - 数据库建库脚本，包含所有表结构和测试数据

---

## 🚀 快速启动（推荐：使用 Docker）

### 方式 1：Docker 自动启动（最简单）⭐

#### 前置条件
- 安装 Docker Desktop：https://www.docker.com/products/docker-desktop
- 确保 Docker Desktop 已启动

#### 启动步骤

```bash
# 1. 进入项目根目录
cd D:\Vcampus

# 2. 启动 MySQL 容器（自动下载镜像、创建数据库、导入数据）
docker-compose up -d

# 3. 查看容器状态
docker-compose ps

# 4. 查看日志（可选）
docker-compose logs mysql
```

#### 停止和清理

```bash
# 停止容器（保留数据）
docker-compose stop

# 停止并删除容器（保留数据卷）
docker-compose down

# 完全清理（包括数据）
docker-compose down -v
```

#### 默认连接信息

| 配置项 | 值 |
|--------|-----|
| 主机 | localhost |
| 端口 | 3306 |
| 数据库名 | vCampus |
| 用户名 | vcampus |
| 密码 | vcampus123 |
| Root 密码 | root123 |

---

### 方式 2：本地安装 MySQL 8.0

#### Windows 安装步骤

1. **下载 MySQL 8.0**
   - 访问：https://dev.mysql.com/downloads/mysql/
   - 选择 Windows 版本下载

2. **安装 MySQL**
   - 运行安装程序
   - 选择 "Developer Default" 或 "Server only"
   - 设置 root 密码（记住这个密码）

3. **启动 MySQL 服务**
   ```powershell
   # 检查服务状态
   Get-Service MySQL80
   
   # 启动服务
   Start-Service MySQL80
   ```

4. **创建数据库并导入数据**
   ```bash
   # 方式 A：命令行导入（推荐）
   mysql -u root -p < sql/vCampus.sql
   
   # 方式 B：分步执行
   mysql -u root -p
   source D:/Vcampus/sql/vCampus.sql
   exit
   ```

5. **配置数据库连接**
   
   编辑 `vcampus-server/src/main/resources/db.properties`：
   ```properties
   db.url=jdbc:mysql://localhost:3306/vCampus?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
   db.user=root
   db.password=你的MySQL密码
   ```

---

## 🔧 验证数据库配置

### 方式 1：使用测试工具

项目根目录提供了数据库连接测试工具：

```bash
# 进入项目根目录
cd D:\Vcampus

# 编译并运行测试（需要先 mvn package）
mvn clean package
java -cp "vcampus-server/target/classes;vcampus-server/target/dependency/*" TestDbConnection
```

**成功输出示例：**
```
=== 数据库连接测试 ===
URL: jdbc:mysql://localhost:3306/vCampus?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
User: vcampus

✓ MySQL 驱动加载成功
✓ 数据库连接成功
✓ 查询执行成功
  当前数据库: vCampus
  MySQL 版本: 8.0.33
✓ 表 tblShopItem 存在

=== 测试完成：所有检查通过 ===
```

### 方式 2：直接启动服务端

```bash
mvn clean package -DskipTests
java -jar vcampus-server/target/vCampusServer.jar
```

观察日志输出：
```
[DbHelper] 已加载 db.properties
vCampus Server 已启动，监听端口 8888
```

---

## ❌ 常见问题

### 1. Docker 容器启动失败

**问题：** `Error response from daemon: Ports are not available`

**原因：** 本地 3306 端口被占用（可能已安装 MySQL）

**解决方案：**
```bash
# 方式 A：停止本地 MySQL 服务
Stop-Service MySQL80

# 方式 B：修改 docker-compose.yml 端口映射
# 将 "3306:3306" 改为 "3307:3306"
# 同时修改 db.properties 中的端口为 3307
```

### 2. 数据库连接失败

**错误信息：** `Communications link failure`

**可能原因：**
- MySQL 服务未启动
- 端口配置错误
- 防火墙拦截

**解决方案：**
```bash
# 检查 MySQL 是否运行（Docker）
docker-compose ps

# 检查 MySQL 是否运行（本地安装）
Get-Service MySQL80

# 测试端口连通性
telnet localhost 3306
```

### 3. 认证失败

**错误信息：** `Access denied for user 'vcampus'@'localhost'`

**原因：** 密码配置不匹配

**解决方案：**
- Docker 方式：密码固定为 `vcampus123`
- 本地安装：检查 `db.properties` 中的密码是否与 MySQL 设置一致

### 4. 数据库不存在

**错误信息：** `Unknown database 'vCampus'`

**解决方案：**
```bash
# Docker 方式：重新创建容器
docker-compose down -v
docker-compose up -d

# 本地安装：重新导入 SQL
mysql -u root -p < sql/vCampus.sql
```

---

## 📊 数据库结构概览

### 核心业务表

| 表名 | 说明 | 负责模块 |
|------|------|----------|
| tblUser | 用户账户表 | 用户管理 |
| tblUserCredential | 用户凭证表（认证） | 用户管理 |
| tblHumanInfo | 人员基本信息 | 用户管理 |
| tblStudent | 学生学籍表 | 学籍管理 |
| tblCourse | 课程表 | 选课系统 |
| tblBook | 图书馆藏表 | 图书馆 |
| tblBorrow | 借阅记录表 | 图书馆 |
| tblShop | 商店表 | 商店系统 |
| tblShopItem | 商品表 | 商店系统 |
| tblOrder | 订单表 | 商店系统 |
| tblBankAccount | 银行账户表 | 银行系统 |
| tblBankTransaction | 银行交易流水表 | 银行系统 |
| tblGlobalSequence | 全局序列表 | 基础设施 |

### 测试数据

脚本已包含以下测试账号：

| 登录名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| 001 | 1 | 学生 | 演示学生账号 |
| 002 | 1 | 教师 | 演示教师账号 |
| 003 | 1 | 管理员 | 演示管理员账号 |

商店测试数据：
- 2 个商店（校园便利店、校园文创店）
- 3 件商品（文化衫、笔记本、保温杯）

---

## 🛠️ 开发者信息

### 数据库配置文件位置

```
vcampus-server/src/main/resources/db.properties
```

### 配置优先级

```
db.properties > 环境变量 > 默认值
```

### 环境变量配置（可选）

适用于 CI/CD 环境：

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=vCampus
export DB_USER=vcampus
export DB_PASSWORD=vcampus123
```

---

## 📞 需要帮助？

- 查看项目根目录 `README.md` 获取完整文档
- 查看 `docs/adr/` 了解架构决策
- 联系数据库负责人
