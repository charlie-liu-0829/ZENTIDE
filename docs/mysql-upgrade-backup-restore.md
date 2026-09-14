# MySQL 全库备份、升级与恢复（macOS）

当前机器检测到的是 `/usr/local/mysql` 下的 MySQL 5.7.31。跨大版本升级建议使用“逻辑导出 → 安装全新的 MySQL 8.4 LTS → 只恢复业务数据库”，不要让 MySQL 8.4 直接读取 5.7 数据目录，也不要把 5.7 的 `mysql` 系统库导入 8.4。

## 1. 保存本机登录信息

下面命令会交互式询问 root 密码，并把凭据加密保存在本机登录路径中，后续命令不用把密码写进终端历史：

```bash
/usr/local/mysql/bin/mysql_config_editor set \
  --login-path=localroot \
  --host=127.0.0.1 \
  --user=root \
  --password
```

确认连接和当前版本：

```bash
/usr/local/mysql/bin/mysql --login-path=localroot \
  -e "SELECT VERSION(); SHOW DATABASES;"
```

## 2. 升级前备份全部数据库

先停止 ZENTIDE 和其他会写入 MySQL 的应用，再创建只允许当前用户访问的备份目录：

```bash
BACKUP_DIR="$HOME/mysql-backups/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
```

### 2.1 完整灾难恢复备份

该文件包含全部业务库和 MySQL 5.7 系统库，只用于恢复到相同的 MySQL 5.7 版本或紧急取数：

```bash
/usr/local/mysql/bin/mysqldump \
  --login-path=localroot \
  --all-databases \
  --single-transaction \
  --quick \
  --routines \
  --events \
  --triggers \
  --hex-blob \
  --set-gtid-purged=OFF \
  --default-character-set=utf8mb4 \
  > "$BACKUP_DIR/mysql-5.7-all-databases.sql"
```

### 2.2 跨版本升级备份

该文件包含服务器上的全部业务数据库，但排除不能跨版本覆盖的系统库：

```bash
USER_DATABASES=("${(@f)$(/usr/local/mysql/bin/mysql \
  --login-path=localroot \
  --batch --skip-column-names \
  -e "SELECT schema_name FROM information_schema.schemata WHERE schema_name NOT IN ('mysql','sys','performance_schema','information_schema') ORDER BY schema_name")}")

printf '<%s>\n' "${USER_DATABASES[@]}"

/usr/local/mysql/bin/mysqldump \
  --login-path=localroot \
  --databases "${USER_DATABASES[@]}" \
  --single-transaction \
  --quick \
  --routines \
  --events \
  --triggers \
  --hex-blob \
  --set-gtid-purged=OFF \
  --default-character-set=utf8mb4 \
  > "$BACKUP_DIR/mysql-user-databases.sql"
```

保存数据库清单、用户授权信息和校验值：

```bash
/usr/local/mysql/bin/mysql --login-path=localroot \
  -e "SHOW DATABASES;" \
  > "$BACKUP_DIR/database-list.txt"

/usr/local/mysql/bin/mysql --login-path=localroot \
  -e "SELECT user,host,plugin FROM mysql.user ORDER BY user,host;" \
  > "$BACKUP_DIR/account-list.txt"

shasum -a 256 "$BACKUP_DIR"/*.sql \
  > "$BACKUP_DIR/SHA256SUMS"

ls -lh "$BACKUP_DIR"
```

两个 SQL 文件都必须大小正常且 `mysqldump` 命令没有报错。重要数据还应把整个备份目录复制到另一块磁盘。

## 3. 升级到 MySQL 8.4 LTS

MySQL 5.7.31 不建议原地跨越到 8.4。推荐步骤：

1. 完成并校验上面的两个逻辑备份。
2. 停止所有使用 MySQL 的应用。
3. 停止 MySQL 5.7：

```bash
sudo /usr/local/mysql/support-files/mysql.server stop
```

4. 在 MySQL 完全停止后，再做一份冷数据目录副本，作为最后的回滚保险：

```bash
sudo ditto /usr/local/mysql/data \
  "$BACKUP_DIR/mysql-5.7-data-directory"
```

5. 从 MySQL 官方网站下载与当前 Mac 芯片匹配的 MySQL 8.4 LTS DMG 安装包。
6. 安装到全新的 8.4 数据目录，不要把新服务指向旧的 5.7 `data` 目录。
7. 启动 MySQL 8.4，设置新的 root 密码，并重新执行第 1 节的 `mysql_config_editor set`。
8. 确认版本：

```bash
/usr/local/mysql/bin/mysql --login-path=localroot \
  -e "SELECT VERSION();"
```

## 4. 恢复到 MySQL 8.4

跨版本恢复只能使用 `mysql-user-databases.sql`：

如果已经打开了新的终端窗口，先把 `BACKUP_DIR` 重新设置为第 2 节实际生成的备份目录，例如 `BACKUP_DIR="$HOME/mysql-backups/20260828-140000"`。

```bash
/usr/local/mysql/bin/mysql \
  --login-path=localroot \
  --default-character-set=utf8mb4 \
  < "$BACKUP_DIR/mysql-user-databases.sql"
```

不要把 `mysql-5.7-all-databases.sql` 整体导入 MySQL 8.4，因为其中包含不兼容的 5.7 系统授权表。

恢复后检查数据库、表数和 ZENTIDE 的 Flyway 版本：

```bash
/usr/local/mysql/bin/mysql --login-path=localroot \
  -e "SHOW DATABASES; SELECT COUNT(*) AS zentide_tables FROM information_schema.tables WHERE table_schema='zentide'; SELECT version,description,success FROM zentide.flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
```

启动 ZENTIDE 后，Flyway 会继续执行尚未运行的迁移。当前最终版本应为 V37，业务表应为 45 张。

## 5. 恢复到相同版本的 MySQL 5.7

只有在目标仍是干净的 MySQL 5.7 实例时，才可以整体恢复完整备份：

同样要先确认 `BACKUP_DIR` 指向实际备份目录。

```bash
/usr/local/mysql/bin/mysql \
  --login-path=localroot \
  --default-character-set=utf8mb4 \
  < "$BACKUP_DIR/mysql-5.7-all-databases.sql"
```

如果只是恢复业务数据库，优先使用 `mysql-user-databases.sql`，避免覆盖现有账号和权限。

## 6. 回滚升级

如果 8.4 验证失败：

1. 停止应用和 MySQL 8.4。
2. 重新安装或启动相同版本的 MySQL 5.7。
3. 恢复冷数据目录副本，或者向干净的 5.7 实例导入 `mysql-5.7-all-databases.sql`。
4. 验证数据库后再恢复应用写入。

不要在 MySQL 服务运行时直接复制或覆盖 `data` 目录。
