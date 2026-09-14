CREATE TABLE IF NOT EXISTS ow_zentide_user_profile (
  user_id VARCHAR(10) NOT NULL,
  handle VARCHAR(30) NULL,
  bio VARCHAR(500) NULL,
  cover_url VARCHAR(2048) NULL,
  location VARCHAR(80) NULL,
  website_url VARCHAR(2048) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_zentide_user_profile_handle (handle),
  CONSTRAINT fk_zentide_user_profile_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO ow_zentide_user_profile(user_id, handle)
SELECT user_id, CONCAT('user', user_id) FROM user_info;
