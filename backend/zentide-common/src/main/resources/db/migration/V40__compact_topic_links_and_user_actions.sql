-- Collapse six topic-association tables and nine user-interaction tables into
-- two typed relationship tables without losing existing state.

CREATE TABLE zentide_topic_link (
  topic_link_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  topic_id BIGINT UNSIGNED NOT NULL,
  radar_id BIGINT UNSIGNED NULL,
  source_id BIGINT UNSIGNED NULL,
  change_id BIGINT UNSIGNED NULL,
  signal_id BIGINT UNSIGNED NULL,
  hub_id BIGINT UNSIGNED NULL,
  post_id BIGINT UNSIGNED NULL,
  link_role VARCHAR(32) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
  weight DECIMAL(5,2) NOT NULL DEFAULT 1.00,
  include_descendants TINYINT(1) NOT NULL DEFAULT 1,
  featured TINYINT(1) NOT NULL DEFAULT 0,
  counter_value INT UNSIGNED NOT NULL DEFAULT 0,
  note VARCHAR(500) NULL,
  created_by VARCHAR(100) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(topic_link_id),
  UNIQUE KEY uk_zentide_topic_link_radar(topic_id,radar_id),
  UNIQUE KEY uk_zentide_topic_link_source(topic_id,source_id),
  UNIQUE KEY uk_zentide_topic_link_change(topic_id,change_id),
  UNIQUE KEY uk_zentide_topic_link_signal(topic_id,signal_id),
  UNIQUE KEY uk_zentide_topic_link_hub(topic_id,hub_id),
  UNIQUE KEY uk_zentide_topic_link_post(topic_id,post_id),
  KEY idx_zentide_topic_link_radar(radar_id,topic_id),
  KEY idx_zentide_topic_link_source(source_id,topic_id,status),
  KEY idx_zentide_topic_link_change(change_id,topic_id),
  KEY idx_zentide_topic_link_signal(signal_id,topic_id),
  KEY idx_zentide_topic_link_hub(hub_id,featured,counter_value),
  KEY idx_zentide_topic_link_post(post_id,topic_id),
  CONSTRAINT fk_zentide_topic_link_topic FOREIGN KEY(topic_id) REFERENCES zentide_topic(topic_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_topic_link_radar FOREIGN KEY(radar_id) REFERENCES zentide_radar(radar_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_topic_link_source FOREIGN KEY(source_id) REFERENCES zentide_source(source_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_topic_link_change FOREIGN KEY(change_id) REFERENCES zentide_change(change_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_topic_link_signal FOREIGN KEY(signal_id) REFERENCES zentide_signal(signal_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_topic_link_hub FOREIGN KEY(hub_id) REFERENCES zentide_hub(hub_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_topic_link_post FOREIGN KEY(post_id) REFERENCES zentide_interest_post(post_id) ON DELETE CASCADE,
  CONSTRAINT chk_zentide_topic_link_one_target CHECK (
    (radar_id IS NOT NULL)+(source_id IS NOT NULL)+(change_id IS NOT NULL)+
    (signal_id IS NOT NULL)+(hub_id IS NOT NULL)+(post_id IS NOT NULL)=1
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE zentide_user_action (
  user_action_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(10) NOT NULL,
  topic_id BIGINT UNSIGNED NULL,
  change_id BIGINT UNSIGNED NULL,
  insight_id BIGINT UNSIGNED NULL,
  comment_id BIGINT UNSIGNED NULL,
  post_id BIGINT UNSIGNED NULL,
  target_user_id VARCHAR(10) NULL,
  event_id BIGINT UNSIGNED NULL,
  interest_entity_id BIGINT UNSIGNED NULL,
  action_type VARCHAR(24) NOT NULL,
  action_value VARCHAR(40) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  note VARCHAR(1000) NULL,
  metadata_json JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(user_action_id),
  UNIQUE KEY uk_zentide_user_action_topic(user_id,topic_id,action_type),
  UNIQUE KEY uk_zentide_user_action_change(user_id,change_id,action_type),
  UNIQUE KEY uk_zentide_user_action_insight(user_id,insight_id,action_type),
  UNIQUE KEY uk_zentide_user_action_comment(user_id,comment_id,action_type),
  UNIQUE KEY uk_zentide_user_action_post(user_id,post_id,action_type),
  UNIQUE KEY uk_zentide_user_action_user(user_id,target_user_id,action_type),
  UNIQUE KEY uk_zentide_user_action_event(user_id,event_id,action_type),
  UNIQUE KEY uk_zentide_user_action_entity(user_id,interest_entity_id,action_type),
  KEY idx_zentide_user_action_topic(topic_id,action_type,status),
  KEY idx_zentide_user_action_change(change_id,action_type,status),
  KEY idx_zentide_user_action_insight(insight_id,action_type,status),
  KEY idx_zentide_user_action_comment(comment_id,action_type,status),
  KEY idx_zentide_user_action_post(post_id,action_type,status),
  KEY idx_zentide_user_action_target_user(target_user_id,action_type,status),
  KEY idx_zentide_user_action_event(event_id,action_type,status),
  KEY idx_zentide_user_action_entity(interest_entity_id,action_type,status),
  KEY idx_zentide_user_action_actor(user_id,action_type,status,updated_at),
  CONSTRAINT fk_zentide_user_action_actor FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_user_action_topic FOREIGN KEY(topic_id) REFERENCES zentide_topic(topic_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_user_action_change FOREIGN KEY(change_id) REFERENCES zentide_change(change_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_user_action_insight FOREIGN KEY(insight_id) REFERENCES zentide_insight(insight_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_user_action_comment FOREIGN KEY(comment_id) REFERENCES zentide_interest_comment(comment_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_user_action_post FOREIGN KEY(post_id) REFERENCES zentide_interest_post(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_user_action_target_user FOREIGN KEY(target_user_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_user_action_event FOREIGN KEY(event_id) REFERENCES zentide_interest_event(event_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_user_action_entity FOREIGN KEY(interest_entity_id) REFERENCES zentide_interest_entity(entity_id) ON DELETE CASCADE,
  CONSTRAINT chk_zentide_user_action_one_target CHECK (
    (topic_id IS NOT NULL)+(change_id IS NOT NULL)+(insight_id IS NOT NULL)+
    (comment_id IS NOT NULL)+(post_id IS NOT NULL)+(target_user_id IS NOT NULL)+
    (event_id IS NOT NULL)+(interest_entity_id IS NOT NULL)=1
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO zentide_topic_link(topic_id,radar_id,weight,include_descendants,note)
SELECT topic_id,radar_id,weight,include_descendants,note FROM zentide_radar_topic;
INSERT INTO zentide_topic_link(topic_id,source_id,link_role,status,created_by,created_at,updated_at)
SELECT topic_id,source_id,platform_key,observation_status,added_by,created_at,updated_at FROM zentide_topic_source;
INSERT INTO zentide_topic_link(topic_id,change_id,link_role,created_at)
SELECT topic_id,change_id,assigned_by,created_at FROM zentide_topic_change;
INSERT INTO zentide_topic_link(topic_id,signal_id,link_role,created_at)
SELECT topic_id,signal_id,assigned_by,created_at FROM zentide_signal_topic;
INSERT INTO zentide_topic_link(topic_id,hub_id,featured,counter_value,created_at)
SELECT topic_id,hub_id,featured,post_count,created_at FROM zentide_hub_topic;
INSERT INTO zentide_topic_link(topic_id,post_id,created_at)
SELECT topic_id,post_id,created_at FROM zentide_interest_post_topic;

INSERT INTO zentide_user_action(user_id,topic_id,action_type,action_value,created_at,updated_at)
SELECT user_id,topic_id,'FOLLOW',notification_mode,created_at,created_at FROM zentide_topic_follow;
INSERT INTO zentide_user_action(user_id,change_id,action_type,action_value,status,created_at,updated_at)
SELECT user_id,change_id,'WATCH',watch_mode,status,created_at,updated_at FROM zentide_change_watch;
INSERT INTO zentide_user_action(user_id,change_id,action_type,action_value,created_at,updated_at)
SELECT user_id,change_id,'STANCE',stance_type,created_at,updated_at FROM zentide_change_stance;
INSERT INTO zentide_user_action(user_id,insight_id,action_type,action_value,note,created_at,updated_at)
SELECT owner_id,insight_id,'FEEDBACK',feedback_type,note,created_at,updated_at FROM zentide_insight_feedback;
INSERT INTO zentide_user_action(user_id,comment_id,action_type,action_value,created_at,updated_at)
SELECT user_id,comment_id,'REACTION',reaction,created_at,created_at FROM zentide_interest_comment_reaction;
INSERT INTO zentide_user_action(user_id,post_id,action_type,action_value,created_at,updated_at)
SELECT user_id,post_id,'REACTION',reaction,created_at,updated_at FROM zentide_interest_post_engagement WHERE reaction IS NOT NULL;
INSERT INTO zentide_user_action(user_id,post_id,action_type,action_value,created_at,updated_at)
SELECT user_id,post_id,'ACTION',action,created_at,updated_at FROM zentide_interest_post_engagement WHERE action IS NOT NULL;
INSERT INTO zentide_user_action(user_id,post_id,action_type,action_value,created_at,updated_at)
SELECT user_id,post_id,'BOOKMARK','BOOKMARKED',created_at,updated_at FROM zentide_interest_post_engagement WHERE bookmarked=1;
INSERT INTO zentide_user_action(user_id,target_user_id,action_type,action_value,created_at,updated_at)
SELECT follower_id,following_id,'FOLLOW','FOLLOWING',created_at,created_at FROM zentide_user_follow;
INSERT INTO zentide_user_action(user_id,event_id,action_type,action_value,created_at,updated_at)
SELECT user_id,event_id,'ATTENDANCE',attendance_status,created_at,updated_at FROM zentide_interest_event_attendance;
INSERT INTO zentide_user_action(user_id,interest_entity_id,action_type,action_value,created_at,updated_at)
SELECT user_id,entity_id,'FOLLOW',notification_mode,created_at,updated_at FROM zentide_interest_entity_engagement WHERE followed=1;
INSERT INTO zentide_user_action(user_id,interest_entity_id,action_type,action_value,created_at,updated_at)
SELECT user_id,entity_id,'ACTION',action,created_at,updated_at FROM zentide_interest_entity_engagement WHERE action IS NOT NULL;

INSERT INTO zentide_fact_audit(actor_type,actor_id,action,target_type,target_id,detail_json,created_at)
SELECT actor_type,actor_id,action,'SOURCE_APPLICATION',CAST(application_id AS CHAR),
       JSON_OBJECT('note',note,'detail',detail_json),created_at
FROM zentide_source_application_review;

DROP TABLE zentide_radar_topic;
DROP TABLE zentide_topic_source;
DROP TABLE zentide_topic_follow;
DROP TABLE zentide_topic_change;
DROP TABLE zentide_change_watch;
DROP TABLE zentide_signal_topic;
DROP TABLE zentide_change_stance;
DROP TABLE zentide_insight_feedback;
DROP TABLE zentide_source_application_review;
DROP TABLE zentide_interest_comment_reaction;
DROP TABLE zentide_interest_post_engagement;
DROP TABLE zentide_user_follow;
DROP TABLE zentide_interest_event_attendance;
DROP TABLE zentide_interest_entity_engagement;
DROP TABLE zentide_hub_topic;
DROP TABLE zentide_interest_post_topic;
