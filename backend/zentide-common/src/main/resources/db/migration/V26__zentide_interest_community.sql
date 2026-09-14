CREATE TABLE IF NOT EXISTS ow_zentide_hub (
  hub_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  slug VARCHAR(80) NOT NULL,
  name VARCHAR(120) NOT NULL,
  description VARCHAR(500) NULL,
  category VARCHAR(40) NOT NULL DEFAULT 'GENERAL',
  cover_url VARCHAR(2048) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  member_count INT UNSIGNED NOT NULL DEFAULT 0,
  post_count INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(hub_id), UNIQUE KEY uk_zentide_hub_slug(slug), KEY idx_zentide_hub_status(status,member_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_hub_member (
  hub_id BIGINT UNSIGNED NOT NULL, user_id VARCHAR(10) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'MEMBER', notification_mode VARCHAR(20) NOT NULL DEFAULT 'HIGHLIGHTS',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(hub_id,user_id), KEY idx_zentide_hub_member_user(user_id,created_at),
  CONSTRAINT fk_zentide_hub_member_hub FOREIGN KEY(hub_id) REFERENCES ow_zentide_hub(hub_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_hub_member_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_interest_entity (
  entity_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, hub_id BIGINT UNSIGNED NOT NULL,
  entity_type VARCHAR(32) NOT NULL, name VARCHAR(180) NOT NULL, subtitle VARCHAR(300) NULL,
  metadata_json JSON NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(entity_id), KEY idx_zentide_entity_hub(hub_id,status),
  CONSTRAINT fk_zentide_entity_hub FOREIGN KEY(hub_id) REFERENCES ow_zentide_hub(hub_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_interest_event (
  event_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, hub_id BIGINT UNSIGNED NOT NULL, entity_id BIGINT UNSIGNED NULL,
  title VARCHAR(220) NOT NULL, description TEXT NULL, starts_at DATETIME(3) NULL, ends_at DATETIME(3) NULL,
  venue VARCHAR(220) NULL, source_url VARCHAR(2048) NULL, status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(event_id), KEY idx_zentide_event_hub_time(hub_id,starts_at),
  CONSTRAINT fk_zentide_event_hub FOREIGN KEY(hub_id) REFERENCES ow_zentide_hub(hub_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_event_entity FOREIGN KEY(entity_id) REFERENCES ow_zentide_interest_entity(entity_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_interest_post (
  post_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, hub_id BIGINT UNSIGNED NOT NULL, author_id VARCHAR(10) NOT NULL,
  entity_id BIGINT UNSIGNED NULL, event_id BIGINT UNSIGNED NULL, post_type VARCHAR(24) NOT NULL DEFAULT 'DISCUSSION',
  title VARCHAR(220) NULL, body TEXT NOT NULL, media_json JSON NULL, status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  like_count INT UNSIGNED NOT NULL DEFAULT 0, comment_count INT UNSIGNED NOT NULL DEFAULT 0, bookmark_count INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(post_id), KEY idx_zentide_post_feed(hub_id,status,created_at), KEY idx_zentide_post_author(author_id,created_at),
  CONSTRAINT fk_zentide_post_hub FOREIGN KEY(hub_id) REFERENCES ow_zentide_hub(hub_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_post_author FOREIGN KEY(author_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_post_entity FOREIGN KEY(entity_id) REFERENCES ow_zentide_interest_entity(entity_id) ON DELETE SET NULL,
  CONSTRAINT fk_zentide_post_event FOREIGN KEY(event_id) REFERENCES ow_zentide_interest_event(event_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_interest_comment (
  comment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, post_id BIGINT UNSIGNED NOT NULL, author_id VARCHAR(10) NOT NULL,
  parent_comment_id BIGINT UNSIGNED NULL, body VARCHAR(4000) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(comment_id), KEY idx_zentide_comment_post(post_id,status,created_at),
  CONSTRAINT fk_zentide_comment_post FOREIGN KEY(post_id) REFERENCES ow_zentide_interest_post(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_comment_author FOREIGN KEY(author_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_comment_parent FOREIGN KEY(parent_comment_id) REFERENCES ow_zentide_interest_comment(comment_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_interest_reaction (
  post_id BIGINT UNSIGNED NOT NULL, user_id VARCHAR(10) NOT NULL, reaction VARCHAR(20) NOT NULL DEFAULT 'LIKE', created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(post_id,user_id), CONSTRAINT fk_zentide_reaction_post FOREIGN KEY(post_id) REFERENCES ow_zentide_interest_post(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_reaction_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_interest_bookmark (
  post_id BIGINT UNSIGNED NOT NULL, user_id VARCHAR(10) NOT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(post_id,user_id), CONSTRAINT fk_zentide_bookmark_post FOREIGN KEY(post_id) REFERENCES ow_zentide_interest_post(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_bookmark_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO ow_zentide_hub(slug,name,description,category) VALUES
 ('tech-new','科技新品','发布会、硬件、应用和下一件想买的东西','TECHNOLOGY'),
 ('live-music','现场音乐','演唱会、音乐节、歌手现场和同好见面','MUSIC'),
 ('games','游戏现场','新作、版本、攻略和一起玩的日常','GAMES'),
 ('ai-lab','AI 实验室','模型、工具、工作流和真实使用体验','AI');
