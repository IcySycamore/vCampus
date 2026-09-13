#!/usr/bin/env bash
# =============================================================================
# vCampus 一键启动：服务器跑在后台，客户端跑在前台
#
#   bash sample/run.sh              # 直接启动（已打包则跳过构建，约 3 秒）
#   bash sample/run.sh --rebuild    # 强制重新打包再启动
#   bash sample/run.sh --fresh      # 清空 data/（重置全部账号与数据）后启动
#
# 预置账号（首次启动自动创建，重复启动不会覆盖）：
#   管理员  admin    / admin123     ← 登录页身份选「管理员」
#   教师    teacher  / 1            ← 登录页身份选「教师」
#   学生    student  / 1            ← 登录页身份选「学生」
#
# 登录后点左侧菜单「个人信息」，就是学籍模块页面。
#
# 环境变量可覆盖：
#   VCAMPUS_JAVA_HOME  JDK 路径（需 JDK 8，项目的 source/target 是 1.7）
#   VCAMPUS_MVN        Maven 可执行文件路径
# =============================================================================

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# 本脚本有两个落脚点：仓库内 sample/（推荐，组内拉下代码就有），以及仓库外层、
# 与 vCampus/ 同级的工作区根目录（本地图方便）。两种都支持：先看上一级有没有 pom.xml。
if [ -f "$SCRIPT_DIR/../pom.xml" ]; then
  ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
elif [ -f "$SCRIPT_DIR/../../vCampus/pom.xml" ]; then
  ROOT_DIR="$(cd "$SCRIPT_DIR/../../vCampus" && pwd)"
else
  ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)/vCampus"
fi
DATA_DIR="$ROOT_DIR/data"
LOG_FILE="$DATA_DIR/server.log"
PORT=8888

REBUILD=0
FRESH=0
for arg in "$@"; do
  case "$arg" in
    --rebuild) REBUILD=1 ;;
    --fresh)   FRESH=1 ;;
    -h|--help) sed -n '2,20p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "未知参数：$arg（可用：--rebuild / --fresh）"; exit 1 ;;
  esac
done

# ---------------------------------------------------------------- JDK
# 项目 source/target 是 1.7，JDK 20+ 已不再支持，必须用 JDK 8。
if [ -z "${VCAMPUS_JAVA_HOME:-}" ]; then
  if [ -x "/d/Tools/jdk8u502-b07/bin/java" ]; then
    VCAMPUS_JAVA_HOME="/d/Tools/jdk8u502-b07"
  elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    VCAMPUS_JAVA_HOME="$JAVA_HOME"
  fi
fi
if [ -z "${VCAMPUS_JAVA_HOME:-}" ] || [ ! -x "$VCAMPUS_JAVA_HOME/bin/java" ]; then
  echo "!! 找不到 JDK。请设置 VCAMPUS_JAVA_HOME 指向 JDK 8，例如："
  echo "   export VCAMPUS_JAVA_HOME=/d/Tools/jdk8u502-b07"
  exit 1
fi
if ! "$VCAMPUS_JAVA_HOME/bin/java" -version 2>&1 | grep -q '"1\.8'; then
  echo "!! 注意：$VCAMPUS_JAVA_HOME 不是 JDK 8。项目 source/target 为 1.7，"
  echo "   在 JDK 20+ 上编译会失败。如构建报错，请改用 JDK 8。"
fi
export JAVA_HOME="$VCAMPUS_JAVA_HOME"

# ---------------------------------------------------------------- Maven
MVN="${VCAMPUS_MVN:-}"
if [ -z "$MVN" ]; then
  if command -v mvn >/dev/null 2>&1; then
    MVN="mvn"
  elif [ -x "/d/Tools/apache-maven-3.9.16/bin/mvn" ]; then
    MVN="/d/Tools/apache-maven-3.9.16/bin/mvn"
  else
    echo "!! 找不到 mvn。请设置 VCAMPUS_MVN 指向 Maven 可执行文件。"
    exit 1
  fi
fi
export PATH="$JAVA_HOME/bin:$(dirname "$MVN"):$PATH"

if [ ! -f "$ROOT_DIR/pom.xml" ]; then
  echo "!! 没有找到工程目录：$ROOT_DIR"
  echo "   （脚本应放在 vCampus/ 仓库的 sample/ 下，或放在与 vCampus/ 同级的工作区根目录下）"
  exit 1
fi

# ---------------------------------------------------------------- 重置
if [ "$FRESH" = "1" ]; then
  echo "==> --fresh：清空 $DATA_DIR"
  rm -rf "$DATA_DIR"
fi
mkdir -p "$DATA_DIR"

# ---------------------------------------------------------------- 预置账号
# data/admins.tsv 是「账户引导文件」，Tab 分隔四列：
#   登录名 <TAB> 姓名 <TAB> 初始口令 <TAB> 角色
# 第 4 列留空则为管理员。首次启动导入，已存在的账号会跳过（幂等）。
# 注意：登录名不能含小数点，否则服务端解析会当成别的东西。
ADMINS_FILE="$DATA_DIR/admins.tsv"
if [ ! -f "$ADMINS_FILE" ]; then
  echo "==> 写入账户引导文件 $ADMINS_FILE"
  {
    printf '# vCampus 账户引导文件（Tab 分隔：登录名 <TAB> 姓名 <TAB> 初始口令 <TAB> 角色）\n'
    printf '# 首次启动导入下列账号；已存在的账号会跳过，改这里不会覆盖已有口令。\n'
    printf '# 要全部重置：删除 data/ 目录后重启，或跑 bash sample/run.sh --fresh\n'
    printf 'admin\t系统管理员\tadmin123\t管理员\n'
    printf 'teacher\t演示教师\t1\t教师\n'
    printf 'student\t演示学生\t1\t学生\n'
  } > "$ADMINS_FILE"
fi

# ---------------------------------------------------------------- 构建
SERVER_JAR="$ROOT_DIR/vcampus-server/target/vCampusServer.jar"
CLIENT_JAR="$ROOT_DIR/vcampus-client/target/vCampusClient.jar"

if [ "$REBUILD" = "1" ] || [ ! -f "$SERVER_JAR" ] || [ ! -f "$CLIENT_JAR" ]; then
  echo "==> 构建中（首次约 1 分钟，之后会快很多）..."
  if ! "$MVN" -f "$ROOT_DIR/pom.xml" -DskipTests -q package; then
    echo "!! 构建失败。可单独重跑看完整输出："
    echo "   cd \"$ROOT_DIR\" && \"$MVN\" -DskipTests package"
    exit 1
  fi
  echo "==> 构建完成"
else
  echo "==> 已有构建产物（需要重新打包时加 --rebuild）"
fi

if [ ! -f "$SERVER_JAR" ] || [ ! -f "$CLIENT_JAR" ]; then
  echo "!! 打包产物不完整："
  echo "   $SERVER_JAR"
  echo "   $CLIENT_JAR"
  exit 1
fi

# ---------------------------------------------------------------- 端口占用检查
if command -v netstat >/dev/null 2>&1 && netstat -ano 2>/dev/null | grep -q ":$PORT .*LISTENING"; then
  echo "!! 端口 $PORT 已被占用，可能有一个旧的服务端还在跑。"
  echo "   先关掉它："
  echo "   netstat -ano | grep \":$PORT\" | grep LISTENING   然后 taskkill /F /PID <PID>"
  exit 1
fi

# ---------------------------------------------------------------- 启动服务器（后台）
: > "$LOG_FILE"
echo "==> 启动服务器（后台），日志：$LOG_FILE"
cd "$ROOT_DIR"
# -Dfile.encoding 等：让写进日志的中文是 UTF-8，便于直接打开看（默认是 GBK）
"$JAVA_HOME/bin/java" -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 \
  -Dsun.stderr.encoding=UTF-8 -jar "$SERVER_JAR" >> "$LOG_FILE" 2>&1 &
SERVER_PID=$!

cleanup() {
  if kill -0 "$SERVER_PID" 2>/dev/null; then
    echo ""
    echo "==> 关闭服务器（PID $SERVER_PID）"
    kill "$SERVER_PID" 2>/dev/null
    wait "$SERVER_PID" 2>/dev/null
  fi
}
trap cleanup EXIT INT TERM

# 等服务器真正开始监听。
# 按端口判断，不去 grep 日志文案：JVM 在 Windows 上写出的中文是平台编码（GBK），
# 用 UTF-8 的 grep 永远匹配不到，会白白等到超时。
READY=0
for _ in $(seq 1 60); do
  if netstat -ano 2>/dev/null | grep -q ":$PORT .*LISTENING"; then READY=1; break; fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then break; fi
  sleep 0.5
done
if [ "$READY" != "1" ]; then
  echo "!! 服务器没能在 30 秒内启动，日志如下："
  iconv -f GBK -t UTF-8 "$LOG_FILE" 2>/dev/null || sed -n '1,40p' "$LOG_FILE"
  exit 1
fi
echo "==> 服务器就绪，监听端口 $PORT"

# ---------------------------------------------------------------- 客户端（前台）
cat <<EOF

================================================================
  客户端即将打开。登录账号（注意身份要和账号匹配）：

    角色        登录名      密码
    ----------------------------------
    管理员      admin       admin123
    教师        teacher     1
    学生        student     1

  登录后点左侧菜单「个人信息」：
    学生           → 只有「我的档案」
    教师 / 管理员   → 多出「学籍管理」页签
    管理员         → 再多出「修改审核」页签
  关闭客户端窗口即结束，服务端会自动一起关掉。
================================================================

EOF

CLIENT_LOG="$DATA_DIR/client.log"
"$JAVA_HOME/bin/java" -Dfile.encoding=UTF-8 -jar "$CLIENT_JAR" 2> "$CLIENT_LOG"
CLIENT_STATUS=$?

# 客户端告一段落。能看到 libpng / Swing 之类的无害警告，但只在真的失败时才show。
# 另外把 stderr 落到文件，控制台就不会因为 Java 写 stderr 而被 PowerShell 标成
# NativeCommandError（那会让正常退出看起来像报错）。
if [ "$CLIENT_STATUS" -ne 0 ]; then
  echo "!! 客户端异常退出（退出码 $CLIENT_STATUS），日志：$CLIENT_LOG"
  sed -n '1,40p' "$CLIENT_LOG"
else
  echo "==> 客户端已关闭"
fi
