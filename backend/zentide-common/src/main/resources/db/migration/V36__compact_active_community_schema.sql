-- Compact tables that represent the same active community relationship.
-- All active user state is copied before the legacy tables are removed.

SET @zentide_v36_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_info' AND column_name='handle')=0,
  'ALTER TABLE user_info ADD COLUMN handle VARCHAR(30) NULL AFTER avatar',
  'SELECT 1'
);
PREPARE zentide_v36_stmt FROM @zentide_v36_sql; EXECUTE zentide_v36_stmt; DEALLOCATE PREPARE zentide_v36_stmt;
SET @zentide_v36_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_info' AND column_name='bio')=0,
  'ALTER TABLE user_info ADD COLUMN bio VARCHAR(500) NULL AFTER handle',
  'SELECT 1'
);
PREPARE zentide_v36_stmt FROM @zentide_v36_sql; EXECUTE zentide_v36_stmt; DEALLOCATE PREPARE zentide_v36_stmt;
SET @zentide_v36_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_info' AND column_name='cover_url')=0,
  'ALTER TABLE user_info ADD COLUMN cover_url VARCHAR(2048) NULL AFTER bio',
  'SELECT 1'
);
PREPARE zentide_v36_stmt FROM @zentide_v36_sql; EXECUTE zentide_v36_stmt; DEALLOCATE PREPARE zentide_v36_stmt;
SET @zentide_v36_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_info' AND column_name='location')=0,
  'ALTER TABLE user_info ADD COLUMN location VARCHAR(80) NULL AFTER cover_url',
  'SELECT 1'
);
PREPARE zentide_v36_stmt FROM @zentide_v36_sql; EXECUTE zentide_v36_stmt; DEALLOCATE PREPARE zentide_v36_stmt;
SET @zentide_v36_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_info' AND column_name='website_url')=0,
  'ALTER TABLE user_info ADD COLUMN website_url VARCHAR(2048) NULL AFTER location',
  'SELECT 1'
);
PREPARE zentide_v36_stmt FROM @zentide_v36_sql; EXECUTE zentide_v36_stmt; DEALLOCATE PREPARE zentide_v36_stmt;
SET @zentide_v36_sql = IF(
  (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='user_info' AND index_name='uk_user_info_handle')=0,
  'ALTER TABLE user_info ADD UNIQUE KEY uk_user_info_handle (handle)',
  'SELECT 1'
);
PREPARE zentide_v36_stmt FROM @zentide_v36_sql; EXECUTE zentide_v36_stmt; DEALLOCATE PREPARE zentide_v36_stmt;

SET @zentide_v36_sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='ow_zentide_user_profile')=1,
  'UPDATE user_info u JOIN ow_zentide_user_profile p ON p.user_id=u.user_id SET u.handle=p.handle,u.bio=p.bio,u.cover_url=p.cover_url,u.location=p.location,u.website_url=p.website_url',
  'SELECT 1'
);
PREPARE zentide_v36_stmt FROM @zentide_v36_sql; EXECUTE zentide_v36_stmt; DEALLOCATE PREPARE zentide_v36_stmt;

SET @zentide_v36_sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='ow_zentide_interest_post')=1,
  'CREATE TABLE IF NOT EXISTS ow_zentide_interest_post_engagement (post_id BIGINT UNSIGNED NOT NULL,user_id VARCHAR(10) NOT NULL,reaction VARCHAR(20) NULL,action VARCHAR(24) NULL,bookmarked TINYINT(1) NOT NULL DEFAULT 0,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),PRIMARY KEY(post_id,user_id),KEY idx_zentide_post_engagement_user(user_id,updated_at),KEY idx_zentide_post_engagement_reaction(post_id,reaction),CONSTRAINT fk_zentide_post_engagement_post FOREIGN KEY(post_id) REFERENCES ow_zentide_interest_post(post_id) ON DELETE CASCADE,CONSTRAINT fk_zentide_post_engagement_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci',
  'DO 0'
);
PREPARE zentide_v36_stmt FROM @zentide_v36_sql; EXECUTE zentide_v36_stmt; DEALLOCATE PREPARE zentide_v36_stmt;

SET @zentide_v36_sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='ow_zentide_interest_reaction')=1,
  'INSERT INTO ow_zentide_interest_post_engagement(post_id,user_id,reaction,created_at,updated_at) SELECT post_id,user_id,reaction,created_at,created_at FROM ow_zentide_interest_reaction ON DUPLICATE KEY UPDATE reaction=VALUES(reaction),updated_at=GREATEST(ow_zentide_interest_post_engagement.updated_at,VALUES(updated_at))',
  'SELECT 1'
);
PREPARE zentide_v36_stmt FROM @zentide_v36_sql; EXECUTE zentide_v36_stmt; DEALLOCATE PREPARE zentide_v36_stmt;

SET @zentide_v36_sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='ow_zentide_interest_bookmark')=1,
  'INSERT INTO ow_zentide_interest_post_engagement(post_id,user_id,bookmarked,created_at,updated_at) SELECT post_id,user_id,1,created_at,created_at FROM ow_zentide_interest_bookmark ON DUPLICATE KEY UPDATE bookmarked=1,updated_at=GREATEST(ow_zentide_interest_post_engagement.updated_at,VALUES(updated_at))',
  'SELECT 1'
);
PREPARE zentide_v36_stmt FROM @zentide_v36_sql; EXECUTE zentide_v36_stmt; DEALLOCATE PREPARE zentide_v36_stmt;

SET @zentide_v36_sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='ow_zentide_interest_action')=1,
  'INSERT INTO ow_zentide_interest_post_engagement(post_id,user_id,action,created_at,updated_at) SELECT post_id,user_id,action,created_at,updated_at FROM ow_zentide_interest_action ON DUPLICATE KEY UPDATE action=VALUES(action),updated_at=GREATEST(ow_zentide_interest_post_engagement.updated_at,VALUES(updated_at))',
  'SELECT 1'
);
PREPARE zentide_v36_stmt FROM @zentide_v36_sql; EXECUTE zentide_v36_stmt; DEALLOCATE PREPARE zentide_v36_stmt;

SET @zentide_v36_sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='ow_zentide_interest_entity')=1,
  'CREATE TABLE IF NOT EXISTS ow_zentide_interest_entity_engagement (entity_id BIGINT UNSIGNED NOT NULL,user_id VARCHAR(10) NOT NULL,followed TINYINT(1) NOT NULL DEFAULT 0,notification_mode VARCHAR(20) NOT NULL DEFAULT ''HIGHLIGHTS'',action VARCHAR(24) NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),PRIMARY KEY(entity_id,user_id),KEY idx_zentide_entity_engagement_user(user_id,updated_at),KEY idx_zentide_entity_engagement_followed(entity_id,followed),CONSTRAINT fk_zentide_entity_engagement_entity FOREIGN KEY(entity_id) REFERENCES ow_zentide_interest_entity(entity_id) ON DELETE CASCADE,CONSTRAINT fk_zentide_entity_engagement_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci',
  'DO 0'
);
PREPARE zentide_v36_stmt FROM @zentide_v36_sql; EXECUTE zentide_v36_stmt; DEALLOCATE PREPARE zentide_v36_stmt;

SET @zentide_v36_sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='ow_zentide_interest_entity_follow')=1,
  'INSERT INTO ow_zentide_interest_entity_engagement(entity_id,user_id,followed,notification_mode,created_at,updated_at) SELECT entity_id,user_id,1,notification_mode,created_at,created_at FROM ow_zentide_interest_entity_follow ON DUPLICATE KEY UPDATE followed=1,notification_mode=VALUES(notification_mode),updated_at=GREATEST(ow_zentide_interest_entity_engagement.updated_at,VALUES(updated_at))',
  'SELECT 1'
);
PREPARE zentide_v36_stmt FROM @zentide_v36_sql; EXECUTE zentide_v36_stmt; DEALLOCATE PREPARE zentide_v36_stmt;

SET @zentide_v36_sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='ow_zentide_interest_entity_action')=1,
  'INSERT INTO ow_zentide_interest_entity_engagement(entity_id,user_id,action,created_at,updated_at) SELECT entity_id,user_id,action,created_at,updated_at FROM ow_zentide_interest_entity_action ON DUPLICATE KEY UPDATE action=VALUES(action),updated_at=GREATEST(ow_zentide_interest_entity_engagement.updated_at,VALUES(updated_at))',
  'SELECT 1'
);
PREPARE zentide_v36_stmt FROM @zentide_v36_sql; EXECUTE zentide_v36_stmt; DEALLOCATE PREPARE zentide_v36_stmt;

-- These tables have no active mapper, service, controller or UI consumer.
-- They are removed from the current product schema and can be introduced later
-- together with the feature that actually owns them.
DROP TABLE IF EXISTS ow_zentide_interest_post_tag;
DROP TABLE IF EXISTS ow_zentide_interest_tag;
DROP TABLE IF EXISTS ow_zentide_interest_post_media;
DROP TABLE IF EXISTS ow_zentide_interest_poll_vote;
DROP TABLE IF EXISTS ow_zentide_interest_poll_option;
DROP TABLE IF EXISTS ow_zentide_interest_event_reminder;
DROP TABLE IF EXISTS ow_zentide_interest_report;
DROP TABLE IF EXISTS ow_zentide_interest_collection_item;
DROP TABLE IF EXISTS ow_zentide_interest_collection;

DROP TABLE IF EXISTS ow_zentide_agent_tool_call;
DROP TABLE IF EXISTS ow_zentide_agent_checkpoint;
DROP TABLE IF EXISTS ow_zentide_agent_step;
DROP TABLE IF EXISTS ow_zentide_agent_run;
DROP TABLE IF EXISTS ow_zentide_human_review;

DROP TABLE IF EXISTS ow_zentide_interest_reaction;
DROP TABLE IF EXISTS ow_zentide_interest_bookmark;
DROP TABLE IF EXISTS ow_zentide_interest_action;
DROP TABLE IF EXISTS ow_zentide_interest_entity_follow;
DROP TABLE IF EXISTS ow_zentide_interest_entity_action;
DROP TABLE IF EXISTS ow_zentide_user_profile;
DROP TABLE IF EXISTS ow_platform_metadata;

SET @zentide_v36_sql = NULL;
