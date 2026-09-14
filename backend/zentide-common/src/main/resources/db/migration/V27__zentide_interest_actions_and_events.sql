CREATE TABLE IF NOT EXISTS ow_zentide_interest_action (
  post_id BIGINT UNSIGNED NOT NULL, user_id VARCHAR(10) NOT NULL, action VARCHAR(24) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(post_id,user_id), KEY idx_zentide_action_user(user_id,created_at),
  CONSTRAINT fk_zentide_action_post FOREIGN KEY(post_id) REFERENCES ow_zentide_interest_post(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_action_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ow_zentide_interest_event(hub_id,title,description,starts_at,venue,status)
SELECT h.hub_id,'下一场值得期待的科技发布','关注发布会、参数和第一手体验。','2026-09-10 19:00:00','线上发布会','UPCOMING'
FROM ow_zentide_hub h WHERE h.slug='tech-new' AND NOT EXISTS (SELECT 1 FROM ow_zentide_interest_event e WHERE e.title='下一场值得期待的科技发布');

INSERT INTO ow_zentide_interest_event(hub_id,title,description,starts_at,venue,status)
SELECT h.hub_id,'城市现场音乐周末','分享演出信息、购票提醒和现场体验。','2026-09-19 18:30:00','城市音乐现场','UPCOMING'
FROM ow_zentide_hub h WHERE h.slug='live-music' AND NOT EXISTS (SELECT 1 FROM ow_zentide_interest_event e WHERE e.title='城市现场音乐周末');
