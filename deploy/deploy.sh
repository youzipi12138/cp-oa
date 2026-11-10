#!/usr/bin/env bash
# 在部署服务器（ubuntu@49.233.89.175）上执行：
#   1. 安装 CI 上传的后端 jar，重启容器并做健康检查，失败自动回滚；
#   2. 原子切换前端静态文件 /var/www/cp-oa。
#
# GitHub Actions 会先把产物放到 /opt/cp-oa/incoming/：
#   incoming/cp-oa-backend.jar  后端可执行 jar
#   incoming/dist/              前端构建产物（VITE_BASE_PATH=/cp-oa/）
#
# 手动执行：bash /opt/cp-oa/deploy.sh [release-id]
set -euo pipefail

APP_DIR=/opt/cp-oa
INCOMING="$APP_DIR/incoming"
JAR="$APP_DIR/backend/target/cp-oa-backend-0.1.0.jar"
BACKUP_DIR="$APP_DIR/backups"
WEB_ROOT=/var/www
WEB_DIR="$WEB_ROOT/cp-oa"
WEB_PREV="$WEB_ROOT/cp-oa.prev"
CONTAINER=cp-oa-backend
HEALTH_URL=http://127.0.0.1:8081/api/health
HEALTH_TRIES=${HEALTH_TRIES:-60}
HEALTH_INTERVAL=${HEALTH_INTERVAL:-2}
RELEASE="${1:-manual-$(date +%Y%m%d-%H%M%S)}"

log() { printf '[deploy %s] %s\n' "$(date +%H:%M:%S)" "$*"; }
die() { log "ERROR: $*"; exit 1; }

# --- 前置检查 ---------------------------------------------------------------
[ -f "$INCOMING/cp-oa-backend.jar" ] || die "缺少 $INCOMING/cp-oa-backend.jar"
[ -f "$INCOMING/dist/index.html" ] || die "缺少 $INCOMING/dist/index.html"
grep -q '/cp-oa/assets/' "$INCOMING/dist/index.html" \
    || die "前端产物不是 /cp-oa/ 子路径构建（检查 VITE_BASE_PATH）"
[ -f "$JAR" ] || die "目标 jar 不存在：$JAR"
sudo -n docker inspect "$CONTAINER" >/dev/null 2>&1 \
    || die "容器 $CONTAINER 不存在，请先按 README 恢复后端容器"

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

wait_healthy() {
    local i
    for ((i = 1; i <= HEALTH_TRIES; i++)); do
        if curl -fsS --max-time 3 "$HEALTH_URL" >/dev/null 2>&1; then
            log "健康检查通过（第 ${i} 次）"
            return 0
        fi
        sleep "$HEALTH_INTERVAL"
    done
    return 1
}

# 用 cat 覆盖而不是 cp/mv：容器是「文件级 bind mount」，
# 只有保持 inode 不变，docker restart 后才会挂到新内容。
install_jar() {
    cat "$1" >"$JAR"
}

rollback_backend() {
    log "回滚后端到上一版本"
    install_jar "$BACKUP_JAR"
    sudo docker restart "$CONTAINER" >/dev/null || true
    if wait_healthy; then
        log "回滚完成，服务已恢复"
    else
        log "回滚后健康检查仍失败，需要人工介入"
    fi
}

# --- 后端 -------------------------------------------------------------------
STAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_JAR="$BACKUP_DIR/cp-oa-backend-0.1.0-${STAMP}.jar"
cp -p "$JAR" "$BACKUP_JAR"
log "已备份当前 jar -> $BACKUP_JAR"

log "安装后端 jar（release ${RELEASE}）"
install_jar "$INCOMING/cp-oa-backend.jar"

log "重启容器 $CONTAINER"
sudo docker restart "$CONTAINER" >/dev/null

if ! wait_healthy; then
    log "新版本健康检查失败"
    rollback_backend
    exit 1
fi

# --- 前端 -------------------------------------------------------------------
deploy_frontend() {
    log "更新前端静态文件"
    sudo rm -rf "$WEB_DIR.new"
    sudo mkdir -p "$WEB_DIR.new"
    sudo rsync -a --delete "$INCOMING/dist/" "$WEB_DIR.new/" || return 1
    sudo chown -R root:root "$WEB_DIR.new" || return 1
    sudo find "$WEB_DIR.new" -type d -exec chmod 755 {} + || return 1
    sudo find "$WEB_DIR.new" -type f -exec chmod 644 {} + || return 1

    sudo rm -rf "$WEB_PREV"
    sudo mv "$WEB_DIR" "$WEB_PREV" || return 1
    if ! sudo mv "$WEB_DIR.new" "$WEB_DIR"; then
        sudo mv "$WEB_PREV" "$WEB_DIR" || true
        return 1
    fi

    # 切换后自检：入口存在且仍是 /cp-oa/ 子路径；不通过就换回上一版
    if [ ! -f "$WEB_DIR/index.html" ] || ! grep -q '/cp-oa/assets/' "$WEB_DIR/index.html"; then
        sudo rm -rf "$WEB_DIR"
        sudo mv "$WEB_PREV" "$WEB_DIR" || true
        return 1
    fi
    return 0
}

if ! deploy_frontend; then
    log "前端更新失败，回滚后端"
    rollback_backend
    exit 1
fi

# --- 记录与清理 -------------------------------------------------------------
{
    printf 'release=%s\n' "$RELEASE"
    printf 'deployed_at=%s\n' "$(date -Is)"
} >"$APP_DIR/RELEASE"

# 只保留最近 5 份后端备份
ls -1t "$BACKUP_DIR"/cp-oa-backend-0.1.0-*.jar 2>/dev/null \
    | tail -n +6 | xargs -r rm -f --

log "部署完成：${RELEASE}"
