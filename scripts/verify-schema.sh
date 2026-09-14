#!/usr/bin/env bash
set -euo pipefail

project_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
snapshot_db=zentide_schema_snapshot_check
migration_db=zentide_schema_migration_check
mysql_host=${MYSQL_HOST:-127.0.0.1}
mysql_port=${MYSQL_PORT:-3306}
mysql_user=${MYSQL_USER:-root}
mysql_socket=${MYSQL_SOCKET:-}
if [[ -n "$mysql_socket" ]]; then
  mysql_client=(mysql --protocol=SOCKET --socket="$mysql_socket" --user="$mysql_user" --batch --skip-column-names)
else
  mysql_client=(mysql --protocol=TCP --host="$mysql_host" --port="$mysql_port" --user="$mysql_user" --batch --skip-column-names)
fi
snapshot_schema=$(mktemp /tmp/zentide-snapshot-schema.XXXXXX)
migration_schema=$(mktemp /tmp/zentide-migration-schema.XXXXXX)

cleanup() {
  "${mysql_client[@]}" -e "DROP DATABASE IF EXISTS \`$snapshot_db\`; DROP DATABASE IF EXISTS \`$migration_db\`;" >/dev/null 2>&1 || true
  rm -f "$snapshot_schema" "$migration_schema"
}
trap cleanup EXIT

"${mysql_client[@]}" -e "DROP DATABASE IF EXISTS \`$snapshot_db\`; DROP DATABASE IF EXISTS \`$migration_db\`; CREATE DATABASE \`$migration_db\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

sed \
  -e "s/CREATE DATABASE IF NOT EXISTS zentide/CREATE DATABASE IF NOT EXISTS $snapshot_db/" \
  -e "s/USE zentide;/USE $snapshot_db;/" \
  "$project_dir/zentide.sql" | "${mysql_client[@]}"

while IFS= read -r migration; do
  "${mysql_client[@]}" "$migration_db" < "$migration"
done < <(find "$project_dir/backend/zentide-common/src/main/resources/db/migration" -maxdepth 1 -type f -name 'V*.sql' | sort -V)

schema_query() {
  local schema=$1
  "${mysql_client[@]}" -e "
    SELECT CONCAT('TABLE|',table_name,'|',engine,'|',table_collation)
      FROM information_schema.tables
      WHERE table_schema='$schema' AND table_type='BASE TABLE';
    SELECT CONCAT('COLUMN|',table_name,'|',LPAD(ordinal_position,4,'0'),'|',column_name,'|',column_type,'|',is_nullable,'|',COALESCE(column_default,'<NULL>'),'|',extra,'|',COALESCE(collation_name,''))
      FROM information_schema.columns
      WHERE table_schema='$schema';
    SELECT CONCAT('INDEX|',table_name,'|',index_name,'|',non_unique,'|',LPAD(seq_in_index,4,'0'),'|',column_name,'|',COALESCE(sub_part,''))
      FROM information_schema.statistics
      WHERE table_schema='$schema';
    SELECT CONCAT('FK|',table_name,'|',constraint_name,'|',column_name,'|',referenced_table_name,'|',referenced_column_name,'|',ordinal_position)
      FROM information_schema.key_column_usage
      WHERE constraint_schema='$schema' AND referenced_table_name IS NOT NULL;
    SELECT CONCAT('RULE|',constraint_name,'|',table_name,'|',update_rule,'|',delete_rule)
      FROM information_schema.referential_constraints
      WHERE constraint_schema='$schema';
  " | sort
}

schema_query "$snapshot_db" | sed "s/$snapshot_db/<schema>/g" > "$snapshot_schema"
schema_query "$migration_db" | sed "s/$migration_db/<schema>/g" > "$migration_schema"

if ! diff -u "$snapshot_schema" "$migration_schema"; then
  echo "Schema mismatch: zentide.sql and Flyway V1-V49 do not produce the same structure." >&2
  exit 1
fi

table_count=$("${mysql_client[@]}" -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$snapshot_db' AND table_type='BASE TABLE'")
echo "Schema parity verified: $table_count tables from snapshot and Flyway V1-V49."
