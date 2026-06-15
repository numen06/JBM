#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-/home/opt/portainer/compose/7/docker-compose.yml}"
COMPOSE_PROJECT="${COMPOSE_PROJECT:-jbm73}"
IMAGE_PREFIX="${IMAGE_PREFIX:-registry.cn-hangzhou.aliyuncs.com/51jbm}"
IMAGE_TAG="${IMAGE_TAG:-7.3}"
SPRING_PROFILE="${SPRING_PROFILE:-jaja7}"
MODE="${MODE:-overlay}"
WAIT_SECONDS="${WAIT_SECONDS:-45}"
TAIL_LINES="${TAIL_LINES:-160}"

SERVICES=(
  jbm-cluster-platform-center
  jbm-cluster-platform-auth
  jbm-cluster-platform-doc
  jbm-cluster-platform-gateway
  jbm-cluster-platform-logs
  jbm-cluster-platform-push
  jbm-cluster-platform-bigscreen
  jbm-cluster-platform-job
  jbm-cluster-platform-weixin
  jbm-admin
)

declare -A MODULES=(
  [jbm-cluster-platform-center]="jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center"
  [jbm-cluster-platform-auth]="jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-auth"
  [jbm-cluster-platform-doc]="jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-doc"
  [jbm-cluster-platform-gateway]="jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-gateway"
  [jbm-cluster-platform-logs]="jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-logs"
  [jbm-cluster-platform-push]="jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-push"
  [jbm-cluster-platform-bigscreen]="jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-bigscreen"
  [jbm-cluster-platform-job]="jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-job"
  [jbm-cluster-platform-weixin]="jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-weixin"
)

usage() {
  cat <<'EOF'
Usage:
  scripts/rebuild_service_container.sh <service|all> [--full] [--no-up] [--logs] [--status]

Examples:
  scripts/rebuild_service_container.sh jbm-cluster-platform-push
  scripts/rebuild_service_container.sh push --logs
  scripts/rebuild_service_container.sh admin --logs
  scripts/rebuild_service_container.sh all --full
  MODE=full WAIT_SECONDS=90 scripts/rebuild_service_container.sh gateway

Environment:
  COMPOSE_FILE      default /home/opt/portainer/compose/7/docker-compose.yml
  COMPOSE_PROJECT   default jbm73
  IMAGE_PREFIX      default registry.cn-hangzhou.aliyuncs.com/51jbm
  IMAGE_TAG         default 7.3
  MODE              overlay or full, default overlay
  WAIT_SECONDS      wait after recreate before status/logs, default 45
EOF
}

alias_service() {
  case "$1" in
    center|auth|doc|gateway|logs|push|bigscreen|job|weixin) printf 'jbm-cluster-platform-%s\n' "$1" ;;
    admin|jbm-admin) printf 'jbm-admin\n' ;;
    *) printf '%s\n' "$1" ;;
  esac
}

is_known_service() {
  local service="$1"
  local item
  for item in "${SERVICES[@]}"; do
    [[ "$item" == "$service" ]] && return 0
  done
  return 1
}

compose_has_service() {
  local service="$1"
  local services
  services="$(docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" config --services)"
  grep -Fxq "$service" <<<"$services"
}

image_for() {
  printf '%s/%s:%s\n' "$IMAGE_PREFIX" "$1" "$IMAGE_TAG"
}

package_backend() {
  local service="$1"
  local module="${MODULES[$service]}"
  echo "==> Maven package: $service ($module)"
  (cd "$ROOT_DIR" && mvn -pl "$module" -am -DskipTests package)
}

build_backend_overlay_image() {
  local service="$1"
  local image
  image="$(image_for "$service")"
  local jar="$ROOT_DIR/dist/$service.jar"
  [[ -f "$jar" ]] || {
    echo "Missing jar: $jar" >&2
    exit 1
  }
  echo "==> Docker overlay image: $image"
  local tmp_dir
  tmp_dir="$(mktemp -d)"
  cp "$jar" "$tmp_dir/app.jar"
  docker build -t "$image" -f - "$tmp_dir" <<EOF
FROM $image
COPY app.jar /app/app.jar
EOF
  rm -rf "$tmp_dir"
}

build_full_image() {
  local service="$1"
  local image
  image="$(image_for "$service")"
  echo "==> Docker full image: $image"
  local build_args="clean package"
  if [[ "$service" != "jbm-admin" ]]; then
    build_args="-pl ${MODULES[$service]} -am -DskipTests package"
  fi
  (cd "$ROOT_DIR" && docker build \
    -f allinone.Dockerfile \
    --target "$service" \
    --build-arg "MAVEN_BUILD_ARGS=$build_args" \
    -t "$image" .)
}

build_admin_overlay_image() {
  local image
  image="$(image_for jbm-admin)"
  local dist="$ROOT_DIR/jbm-admin-vue/dist"
  echo "==> npm build: jbm-admin-vue"
  (cd "$ROOT_DIR/jbm-admin-vue" && npm run build)
  [[ -d "$dist" ]] || {
    echo "Missing dist: $dist" >&2
    exit 1
  }
  echo "==> Docker overlay image: $image"
  local tmp_dir
  tmp_dir="$(mktemp -d)"
  cp -r "$dist" "$tmp_dir/dist"
  docker build -t "$image" -f - "$tmp_dir" <<EOF
FROM $image
COPY dist /usr/share/nginx/html
EOF
  rm -rf "$tmp_dir"
}

build_admin_image() {
  local image
  image="$(image_for jbm-admin)"
  echo "==> Docker image: $image"
  (cd "$ROOT_DIR" && docker build -f allinone.Dockerfile --target jbm-admin -t "$image" .)
}

recreate_container() {
  local service="$1"
  if ! compose_has_service "$service"; then
    echo "==> Skip compose recreate: $service is not in $COMPOSE_FILE"
    return 0
  fi
  echo "==> Compose recreate: $service"
  docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" up -d --no-deps --force-recreate "$service"
}

show_status() {
  local service="$1"
  echo "==> Status: $service"
  docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" ps "$service" || true
}

show_logs() {
  local service="$1"
  if compose_has_service "$service"; then
    echo "==> Logs: $service"
    docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" logs --tail "$TAIL_LINES" "$service" || true
  fi
}

rebuild_one() {
  local service="$1"
  [[ "$service" == "all" ]] && {
    local item
    for item in "${SERVICES[@]}"; do
      rebuild_one "$item"
    done
    return 0
  }
  service="$(alias_service "$service")"
  is_known_service "$service" || {
    echo "Unknown service: $service" >&2
    usage
    exit 2
  }

  if [[ "$service" == "jbm-admin" ]]; then
    if [[ "$MODE" == "full" ]]; then
      build_admin_image
    else
      build_admin_overlay_image
    fi
  elif [[ "$MODE" == "full" ]]; then
    build_full_image "$service"
  else
    package_backend "$service"
    build_backend_overlay_image "$service"
  fi

  if [[ "$NO_UP" != "1" ]]; then
    recreate_container "$service"
    sleep "$WAIT_SECONDS"
  fi
  [[ "$SHOW_STATUS" == "1" ]] && show_status "$service"
  [[ "$SHOW_LOGS" == "1" ]] && show_logs "$service"
}

TARGET="${1:-}"
[[ -n "$TARGET" ]] || {
  usage
  exit 2
}
shift || true

NO_UP=0
SHOW_LOGS=0
SHOW_STATUS=1

while [[ $# -gt 0 ]]; do
  case "$1" in
    --full) MODE=full ;;
    --overlay) MODE=overlay ;;
    --no-up) NO_UP=1 ;;
    --logs) SHOW_LOGS=1 ;;
    --no-status) SHOW_STATUS=0 ;;
    --status) SHOW_STATUS=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 2 ;;
  esac
  shift
done

rebuild_one "$TARGET"
