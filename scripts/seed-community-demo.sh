#!/usr/bin/env bash
set -euo pipefail

project_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
mysql_bin=${MYSQL_BIN:-mysql}
database=${MYSQL_DATABASE:-zentide}

if [[ -n "${MYSQL_LOGIN_PATH:-}" ]]; then
  mysql_args=(--login-path="$MYSQL_LOGIN_PATH")
elif [[ -n "${MYSQL_SOCKET:-}" ]]; then
  mysql_args=(--protocol=SOCKET --socket="$MYSQL_SOCKET" --user="${MYSQL_USER:-root}")
else
  mysql_args=(--protocol=TCP --host="${MYSQL_HOST:-127.0.0.1}" --port="${MYSQL_PORT:-3306}" --user="${MYSQL_USER:-root}")
fi

echo "Seeding demo community data into database: ${database}"
"$mysql_bin" "${mysql_args[@]}" "$database" < "$project_dir/scripts/seed-community-demo.sql"
