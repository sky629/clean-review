#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_DIR="$ROOT_DIR/infra/docker-compose"
COMPOSE_FILE="$COMPOSE_DIR/docker-compose.yml"
ENV_FILE="$COMPOSE_DIR/.env"
ENV_EXAMPLE="$COMPOSE_DIR/.env.example"

usage() {
  cat <<'USAGE'
Usage: ./run_local.sh <command>

Commands:
  up       Start local infra stack
  app      Start infra stack with backend and frontend apps
  worker   Start infra stack with review-analysis-worker
  full     Start infra, apps, and review-analysis-worker
  down     Stop local stack, keep volumes
  restart  Restart local infra stack
  logs     Follow logs
  status   Show service status
  clean    Stop stack and delete volumes
USAGE
}

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

require_env() {
  if [[ ! -f "$ENV_FILE" ]]; then
    echo "Missing $ENV_FILE"
    echo "Create it with:"
    echo "  cp $ENV_EXAMPLE $ENV_FILE"
    exit 1
  fi
}

cmd="${1:-}"

case "$cmd" in
  up)
    require_env
    compose up -d
    ;;
  app)
    require_env
    compose --profile app up -d
    ;;
  worker)
    require_env
    compose --profile worker up -d
    ;;
  full)
    require_env
    compose --profile app --profile worker up -d
    ;;
  down)
    require_env
    compose down
    ;;
  restart)
    require_env
    compose down
    compose up -d
    ;;
  logs)
    require_env
    compose logs -f --tail=200
    ;;
  status)
    require_env
    compose ps
    ;;
  clean)
    require_env
    echo "This deletes local Postgres/Kafka/Redis volumes."
    read -r -p "Type 'clean-review' to continue: " confirmation
    if [[ "$confirmation" != "clean-review" ]]; then
      echo "Aborted."
      exit 1
    fi
    compose down -v
    ;;
  -h|--help|help|"")
    usage
    ;;
  *)
    echo "Unknown command: $cmd"
    usage
    exit 1
    ;;
esac
