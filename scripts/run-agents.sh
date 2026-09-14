#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
project_dir="$(cd "$script_dir/.." && pwd)"
agent_root="$project_dir/backend/agent/src/main/python"

# Prefer the preconfigured Conda environment named "agent".  The explicit
# ZENTIDE_AGENT_PYTHON override is useful for CI or a project virtualenv.
if [[ -n "${ZENTIDE_AGENT_PYTHON:-}" ]]; then
  default_python="$ZENTIDE_AGENT_PYTHON"
elif command -v conda >/dev/null 2>&1; then
  conda_base="$(conda info --base 2>/dev/null || true)"
  if [[ -x "$conda_base/envs/agent/bin/python" ]]; then
    default_python="$conda_base/envs/agent/bin/python"
  else
    default_python="python3"
  fi
else
  default_python="python3"
fi

echo "Using Agent Python: $default_python"

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
