#!/usr/bin/env python3
"""Generate varied local-development posts without touching existing rows.

The script only inserts new PUBLISHED posts and optional topic links. It uses
the mysql client so credentials stay in the user's normal login path/config.
"""
from __future__ import annotations

import argparse
import datetime as dt
import os
import random
import shlex
import subprocess
import sys
import uuid


def mysql_args(database: str, batch: bool = False) -> list[str]:
    args = [os.getenv("MYSQL_BIN", "mysql")]
    if os.getenv("MYSQL_LOGIN_PATH"):
        args.append(f"--login-path={os.environ['MYSQL_LOGIN_PATH']}")
    elif os.getenv("MYSQL_SOCKET"):
        args += ["--protocol=SOCKET", f"--socket={os.environ['MYSQL_SOCKET']}", "--user=" + os.getenv("MYSQL_USER", "root")]
    else:
        args += ["--protocol=TCP", "--host=" + os.getenv("MYSQL_HOST", "127.0.0.1"), "--port=" + os.getenv("MYSQL_PORT", "3306"), "--user=" + os.getenv("MYSQL_USER", "root")]
    if batch:
        args += ["--batch", "--skip-column-names"]
    args.append(database)
    return args


def query(database: str, sql: str) -> list[str]:
    result = subprocess.run(mysql_args(database, True), input=sql, text=True, capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr.strip() or "mysql 查询失败")
    return [line for line in result.stdout.splitlines() if line]


def sql(value: str | None) -> str:
    if value is None:
        return "NULL"
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n") + "'"


def main() -> int:
    parser = argparse.ArgumentParser(description="随机生成不同的社区帖子（仅建议用于开发库）")
    parser.add_argument("--count", type=int, default=10, help="生成数量，默认 10，最大 200")
    parser.add_argument("--hub-id", type=int, help="指定现场，不指定则随机选择 ACTIVE 公开现场")
    parser.add_argument("--author-id", help="指定作者，不指定则随机选择现有用户")
    parser.add_argument("--seed", type=int, help="固定随机种子，便于复现")
    parser.add_argument("--dry-run", action="store_true", help="只打印 SQL，不写入数据库")
    parser.add_argument("--database", default=os.getenv("MYSQL_DATABASE", "zentide"))
    args = parser.parse_args()
    if not 1 <= args.count <= 200:
        parser.error("--count 必须在 1 到 200 之间")
    rng = random.Random(args.seed)

    hubs = query(args.database, "SELECT hub_id FROM zentide_hub WHERE status='ACTIVE' AND visibility='PUBLIC' ORDER BY hub_id")
    authors = query(args.database, "SELECT user_id FROM user_info WHERE status=1 ORDER BY user_id")
    topics = query(args.database, "SELECT topic_id FROM zentide_topic WHERE status='ACTIVE' ORDER BY topic_id")
    if args.hub_id is not None:
        hubs = [str(args.hub_id)] if str(args.hub_id) in hubs else []
    if args.author_id is not None:
        authors = [args.author_id] if args.author_id in authors else []
    if not hubs or not authors:
        raise RuntimeError("找不到可用现场或作者，请先导入演示数据")

    adjectives = ["周末", "刚刚", "认真记录", "低成本", "意外发现", "实测", "新手视角", "通勤路上", "深夜整理", "朋友推荐"]
    subjects = ["城市现场", "AI 工具", "开源项目", "阅读清单", "桌面设备", "周末活动", "学习方法", "社区协作", "版本更新", "旅行路线"]
    questions = ["大家更看重哪一点？", "有没有更稳妥的做法？", "你们会怎么选择？", "这个方案值得继续投入吗？", "有没有类似经验可以分享？"]
    types = ["DISCUSSION", "QUESTION", "EXPERIENCE", "REVIEW", "EVENT"]
    now = dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    statements = ["START TRANSACTION;"]
    for index in range(args.count):
        adjective, subject = rng.choice(adjectives), rng.choice(subjects)
        post_type = rng.choice(types)
        token = uuid.uuid4().hex[:8]
        title = f"{adjective}{subject}：{rng.choice(questions)} #{token}"
        body = (f"这是第 {index + 1} 条随机生成的开发测试帖子，主题是“{subject}”。\n\n"
                f"我先记录三个观察：场景不同会影响选择，实际体验比参数更重要，后续还需要更多样本。"
                f"\n\n{rng.choice(questions)}（生成批次 {token}，时间 {now}）")
        hub = rng.choice(hubs)
        author = rng.choice(authors)
        topic = rng.choice(topics) if topics and rng.random() < 0.8 else None
        statements.append("INSERT INTO zentide_interest_post(hub_id,author_id,post_type,title,body,status) VALUES "
                          f"({hub},{sql(author)},{sql(post_type)},{sql(title)},{sql(body)},'PUBLISHED');")
        if topic:
            statements.append("SET @random_post_id=LAST_INSERT_ID();")
            statements.append("INSERT IGNORE INTO zentide_topic_link(topic_id,post_id,link_role,status,created_by) "
                              f"VALUES ({topic},@random_post_id,'POST_TOPIC','ACTIVE','random-seed');")
    statements.append("COMMIT;")
    text = "\n".join(statements) + "\n"
    if args.dry_run:
        print(text)
        return 0
    result = subprocess.run(mysql_args(args.database), input=text, text=True, capture_output=True)
    if result.returncode:
        print(result.stderr, file=sys.stderr)
        return result.returncode
    print(f"已生成 {args.count} 条不同的随机帖子，数据库：{args.database}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
