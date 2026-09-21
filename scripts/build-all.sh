#!/usr/bin/env bash
#
# JvmEye 一键构建:前端构建产物 -> 后端 static -> 单 jar
#
# 用法:  scripts/build-all.sh [--skip-frontend]
#
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
STATIC_DIR="$PROJECT_ROOT/src/main/resources/static"

SKIP_FRONTEND=false
for arg in "$@"; do
  case "$arg" in
    --skip-frontend) SKIP_FRONTEND=true ;;
    *) echo "未知参数: $arg" >&2; exit 1 ;;
  esac
done

echo "==> [1/3] 构建前端 (Vue 3 + Vite)"
if [ "$SKIP_FRONTEND" = true ]; then
  echo "    跳过前端构建(使用已有 dist)"
else
  if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    echo "    安装前端依赖..."
    (cd "$FRONTEND_DIR" && npm install --no-audit --no-fund)
  fi
  (cd "$FRONTEND_DIR" && npm run build)
fi

if [ ! -f "$FRONTEND_DIR/dist/index.html" ]; then
  echo "找不到 $FRONTEND_DIR/dist/index.html,前端构建失败" >&2
  exit 1
fi

echo "==> [2/3] 复制 dist -> src/main/resources/static"
mkdir -p "$STATIC_DIR"
# 清空旧产物(保留 .gitkeep)
find "$STATIC_DIR" -mindepth 1 ! -name '.gitkeep' -exec rm -rf {} +
cp -r "$FRONTEND_DIR/dist/." "$STATIC_DIR/"
ls -1 "$STATIC_DIR"

echo "==> [3/3] Maven 打包"
(cd "$PROJECT_ROOT" && mvn -q -DskipTests clean package)

JAR="$PROJECT_ROOT/target/jvme-eye.jar"
if [ -f "$JAR" ]; then
  echo ""
  echo "构建完成: $JAR"
  echo "启动:     java -jar $JAR"
else
  echo "未找到 $JAR,构建失败" >&2
  exit 1
fi
