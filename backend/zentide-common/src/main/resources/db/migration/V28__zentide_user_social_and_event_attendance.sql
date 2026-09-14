CREATE TABLE IF NOT EXISTS ow_zentide_user_follow (
  follower_id VARCHAR(10) NOT NULL,
  following_id VARCHAR(10) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (follower_id, following_id),
  KEY idx_zentide_follow_following (following_id, created_at),
  CONSTRAINT fk_zentide_follow_follower FOREIGN KEY (follower_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_follow_following FOREIGN KEY (following_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_interest_event_attendance (
  event_id BIGINT UNSIGNED NOT NULL,
  user_id VARCHAR(10) NOT NULL,
  attendance_status VARCHAR(20) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (event_id, user_id),
  KEY idx_zentide_event_attendance_user (user_id, updated_at),
  CONSTRAINT fk_zentide_event_attendance_event FOREIGN KEY (event_id) REFERENCES ow_zentide_interest_event(event_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_event_attendance_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
