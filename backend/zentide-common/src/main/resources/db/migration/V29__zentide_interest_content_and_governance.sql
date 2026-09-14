-- 内容组织：标签、媒体与投票
CREATE TABLE IF NOT EXISTS ow_zentide_interest_tag (
  tag_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  hub_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(60) NOT NULL,
  color VARCHAR(20) NULL,
  usage_count INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (tag_id), UNIQUE KEY uk_zentide_tag_hub_name (hub_id, name),
  KEY idx_zentide_tag_popularity (hub_id, usage_count),
  CONSTRAINT fk_zentide_tag_hub FOREIGN KEY (hub_id) REFERENCES ow_zentide_hub(hub_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_interest_post_tag (
  post_id BIGINT UNSIGNED NOT NULL,
  tag_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (post_id, tag_id),
  CONSTRAINT fk_zentide_post_tag_post FOREIGN KEY (post_id) REFERENCES ow_zentide_interest_post(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_post_tag_tag FOREIGN KEY (tag_id) REFERENCES ow_zentide_interest_tag(tag_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_interest_post_media (
  media_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  post_id BIGINT UNSIGNED NOT NULL,
  media_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
  media_url VARCHAR(2048) NOT NULL,
  thumbnail_url VARCHAR(2048) NULL,
  width INT UNSIGNED NULL,
  height INT UNSIGNED NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (media_id), KEY idx_zentide_post_media_order (post_id, sort_order),
  CONSTRAINT fk_zentide_post_media_post FOREIGN KEY (post_id) REFERENCES ow_zentide_interest_post(post_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_interest_poll_option (
  option_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  post_id BIGINT UNSIGNED NOT NULL,
  option_text VARCHAR(220) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  vote_count INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (option_id), KEY idx_zentide_poll_option_post (post_id, sort_order),
  CONSTRAINT fk_zentide_poll_option_post FOREIGN KEY (post_id) REFERENCES ow_zentide_interest_post(post_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_interest_poll_vote (
  post_id BIGINT UNSIGNED NOT NULL,
  option_id BIGINT UNSIGNED NOT NULL,
  user_id VARCHAR(10) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (post_id, user_id), KEY idx_zentide_poll_vote_option (option_id),
  CONSTRAINT fk_zentide_poll_vote_post FOREIGN KEY (post_id) REFERENCES ow_zentide_interest_post(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_poll_vote_option FOREIGN KEY (option_id) REFERENCES ow_zentide_interest_poll_option(option_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_poll_vote_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 兴趣对象：用户可以关注一个具体产品、歌手、游戏或展览，而不只是关注帖子
CREATE TABLE IF NOT EXISTS ow_zentide_interest_entity_follow (
  entity_id BIGINT UNSIGNED NOT NULL,
  user_id VARCHAR(10) NOT NULL,
  notification_mode VARCHAR(20) NOT NULL DEFAULT 'HIGHLIGHTS',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (entity_id, user_id), KEY idx_zentide_entity_follow_user (user_id, created_at),
  CONSTRAINT fk_zentide_entity_follow_entity FOREIGN KEY (entity_id) REFERENCES ow_zentide_interest_entity(entity_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_entity_follow_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_interest_entity_action (
  entity_id BIGINT UNSIGNED NOT NULL,
  user_id VARCHAR(10) NOT NULL,
  action VARCHAR(24) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (entity_id, user_id), KEY idx_zentide_entity_action_user (user_id, updated_at),
  CONSTRAINT fk_zentide_entity_action_entity FOREIGN KEY (entity_id) REFERENCES ow_zentide_interest_entity(entity_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_entity_action_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 活动提醒与社区治理
CREATE TABLE IF NOT EXISTS ow_zentide_interest_event_reminder (
  event_id BIGINT UNSIGNED NOT NULL,
  user_id VARCHAR(10) NOT NULL,
  remind_at DATETIME(3) NOT NULL,
  delivered_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (event_id, user_id), KEY idx_zentide_event_reminder_due (remind_at, delivered_at),
  CONSTRAINT fk_zentide_event_reminder_event FOREIGN KEY (event_id) REFERENCES ow_zentide_interest_event(event_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_event_reminder_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_interest_report (
  report_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reporter_id VARCHAR(10) NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT UNSIGNED NOT NULL,
  reason VARCHAR(40) NOT NULL,
  detail VARCHAR(1000) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  reviewed_by VARCHAR(10) NULL,
  reviewed_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (report_id), KEY idx_zentide_report_review (status, created_at),
  CONSTRAINT fk_zentide_report_reporter FOREIGN KEY (reporter_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_report_reviewer FOREIGN KEY (reviewed_by) REFERENCES user_info(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户可以把帖子、活动和对象整理成自己的兴趣清单
CREATE TABLE IF NOT EXISTS ow_zentide_interest_collection (
  collection_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_id VARCHAR(10) NOT NULL,
  title VARCHAR(120) NOT NULL,
  description VARCHAR(500) NULL,
  visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
  item_count INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (collection_id), KEY idx_zentide_collection_owner (owner_id, updated_at),
  CONSTRAINT fk_zentide_collection_owner FOREIGN KEY (owner_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_interest_collection_item (
  collection_id BIGINT UNSIGNED NOT NULL,
  item_type VARCHAR(20) NOT NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  note VARCHAR(500) NULL,
  sort_order INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (collection_id, item_type, item_id), KEY idx_zentide_collection_item_order (collection_id, sort_order),
  CONSTRAINT fk_zentide_collection_item_collection FOREIGN KEY (collection_id) REFERENCES ow_zentide_interest_collection(collection_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
