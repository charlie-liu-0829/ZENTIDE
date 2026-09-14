#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
project_dir="$(cd "$script_dir/.." && pwd)"
review_dir="$project_dir/backend/agent/src/main/python/SmartPosting"
python_bin="${ZENTIDE_AGENT_PYTHON:-python3}"

cd "$review_dir"
exec "$python_bin" run_api.py
