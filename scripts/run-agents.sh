#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
project_dir="$(cd "$script_dir/.." && pwd)"
agent_root="$project_dir/backend/agent/src/main/python"
default_python="${ZENTIDE_AGENT_PYTHON:-python3}"

declare -a child_pids=()

cleanup() {
  trap - INT TERM EXIT
  for pid in "${child_pids[@]:-}"; do
    kill "$pid" 2>/dev/null || true
  done
  wait 2>/dev/null || true
}
trap cleanup INT TERM EXIT

start_agent() {
  local name="$1"
  local port_key="$2"
  local port="$3"
  local python_bin="$4"
  local workdir="$agent_root/$name"

  if [[ ! -d "$workdir" ]]; then
    echo "Agent directory does not exist: $workdir" >&2
    return 1
  fi

  (
    cd "$workdir"
    export "$port_key=$port"
    exec "$python_bin" run_api.py
  ) > >(sed -u "s/^/[$name] /") 2>&1 &
  child_pids+=("$!")
  echo "Started $name Agent on port $port"
}

start_agent chat ZENTIDE_AGENT_PORT 8090 "${ZENTIDE_CHAT_PYTHON:-$default_python}"
start_agent SmartPosting ZENTIDE_POST_REVIEW_PORT 8091 "${ZENTIDE_POST_REVIEW_PYTHON:-$default_python}"
start_agent Recommend ZENTIDE_RECOMMEND_PORT 8092 "${ZENTIDE_RECOMMEND_PYTHON:-$default_python}"
start_agent Governance ZENTIDE_GOVERNANCE_PORT 8093 "${ZENTIDE_GOVERNANCE_PYTHON:-$default_python}"

echo "All ZENTIDE Agents are running. Press Ctrl-C to stop all Agents."
wait
