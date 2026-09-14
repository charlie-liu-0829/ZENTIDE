CREATE TABLE IF NOT EXISTS ow_zentide_hub_topic (
  hub_id BIGINT UNSIGNED NOT NULL,
  topic_id BIGINT UNSIGNED NOT NULL,
  featured TINYINT(1) NOT NULL DEFAULT 0,
  post_count INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (hub_id, topic_id), KEY idx_zentide_hub_topic_featured (hub_id, featured, post_count),
  CONSTRAINT fk_zentide_hub_topic_hub FOREIGN KEY (hub_id) REFERENCES ow_zentide_hub(hub_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_hub_topic_topic FOREIGN KEY (topic_id) REFERENCES ow_zentide_topic(topic_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_interest_post_topic (
  post_id BIGINT UNSIGNED NOT NULL,
  topic_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (post_id, topic_id), KEY idx_zentide_post_topic_topic (topic_id, created_at),
  CONSTRAINT fk_zentide_post_topic_post FOREIGN KEY (post_id) REFERENCES ow_zentide_interest_post(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_post_topic_topic FOREIGN KEY (topic_id) REFERENCES ow_zentide_topic(topic_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ow_zentide_topic(canonical_name, topic_type, aliases_json)
SELECT '抢票攻略', 'INTEREST', JSON_ARRAY('购票','开票') WHERE NOT EXISTS (SELECT 1 FROM ow_zentide_topic WHERE canonical_name='抢票攻略');
INSERT INTO ow_zentide_topic(canonical_name, topic_type, aliases_json)
SELECT '真实体验', 'INTEREST', JSON_ARRAY('上手','使用感受') WHERE NOT EXISTS (SELECT 1 FROM ow_zentide_topic WHERE canonical_name='真实体验');
INSERT INTO ow_zentide_topic(canonical_name, topic_type, aliases_json)
SELECT '版本变化', 'INTEREST', JSON_ARRAY('更新','补丁') WHERE NOT EXISTS (SELECT 1 FROM ow_zentide_topic WHERE canonical_name='版本变化');
INSERT INTO ow_zentide_topic(canonical_name, topic_type, aliases_json)
SELECT '工作流实践', 'INTEREST', JSON_ARRAY('效率','实战') WHERE NOT EXISTS (SELECT 1 FROM ow_zentide_topic WHERE canonical_name='工作流实践');

INSERT IGNORE INTO ow_zentide_hub_topic(hub_id, topic_id, featured)
SELECT h.hub_id, t.topic_id, 1 FROM ow_zentide_hub h JOIN ow_zentide_topic t
  ON (h.slug='live-music' AND t.canonical_name='抢票攻略')
  OR (h.slug='tech-new' AND t.canonical_name='真实体验')
  OR (h.slug='games' AND t.canonical_name='版本变化')
  OR (h.slug='ai-lab' AND t.canonical_name='工作流实践');
