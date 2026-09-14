CREATE TABLE IF NOT EXISTS zentide_direct_message (
  message_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sender_id VARCHAR(10) NOT NULL,
  recipient_id VARCHAR(10) NOT NULL,
  body VARCHAR(2000) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'SENT',
  read_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(message_id),
  KEY idx_zentide_dm_sender_peer(sender_id,recipient_id,created_at,message_id),
  KEY idx_zentide_dm_recipient_peer(recipient_id,sender_id,created_at,message_id),
  KEY idx_zentide_dm_unread(recipient_id,status,read_at,created_at),
  CONSTRAINT fk_zentide_dm_sender FOREIGN KEY(sender_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_dm_recipient FOREIGN KEY(recipient_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
