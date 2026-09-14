#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
project_dir="$(cd "$script_dir/.." && pwd)"
chat_dir="$project_dir/backend/agent/src/main/python/chat"
python_bin="${ZENTIDE_AGENT_PYTHON:-python3}"

if [[ -z "${DASHSCOPE_API_KEY:-}" ]]; then
  echo "DASHSCOPE_API_KEY 未设置，无法调用通义千问。" >&2
  exit 1
fi

cd "$chat_dir"
exec "$python_bin" run_api.py
