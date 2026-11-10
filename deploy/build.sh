#!/usr/bin/env bash
# 在部署服务器上从源码构建后端 jar 与前端 dist，然后调用 deploy.sh 完成安装。
#
# 为什么不直接上传 CI 产物：GitHub Actions 运行器在境外，直传 56MB 的 jar 到腾讯云
# 实测只有十几 KB/s；而服务器从 GitHub 拉取源码（<1MB）只需数秒，且本机 Maven /
# pnpm 缓存已预热，整套构建约 30 秒。CI 侧仍会完整构建验证，部署侧用同一 commit 重建。
#
# 用法：bash deploy/build.sh [release-id]
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_DIR=/opt/cp-oa
INCOMING="$APP_DIR/incoming"
RELEASE="${1:-manual-$(date +%Y%m%d-%H%M%S)}"

log() { printf '[build %s] %s\n' "$(date +%H:%M:%S)" "$*"; }
die() { log "ERROR: $*"; exit 1; }

mkdir -p "$INCOMING"

log "构建后端（release ${RELEASE}）"
(
    cd "$REPO_DIR/backend"
    MAVEN_OPTS="${MAVEN_OPTS:--Xmx512m}" mvn -B -q -DskipTests package
)
JAR=$(ls -1t "$REPO_DIR"/backend/target/cp-oa-backend-*.jar 2>/dev/null | head -1)
[ -n "${JAR:-}" ] || die "后端构建未产出 jar"
cp -f "$JAR" "$INCOMING/cp-oa-backend.jar"

log "构建前端（VITE_BASE_PATH=/cp-oa/）"
(
    cd "$REPO_DIR/frontend"
    VITE_BASE_PATH=/cp-oa/ pnpm install --frozen-lockfile --prefer-offline
    VITE_BASE_PATH=/cp-oa/ pnpm build
)
[ -f "$REPO_DIR/frontend/dist/index.html" ] || die "前端构建未产出 dist/index.html"
rsync -a --delete "$REPO_DIR/frontend/dist/" "$INCOMING/dist/"

log "构建完成，开始部署"
exec bash "$REPO_DIR/deploy/deploy.sh" "$RELEASE"
