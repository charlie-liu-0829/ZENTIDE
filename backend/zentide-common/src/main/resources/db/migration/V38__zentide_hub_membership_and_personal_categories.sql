-- Interest-hub admission, invitations and per-user hub organization.
-- V37 is the last schema without these columns/tables, so this migration uses
-- deterministic DDL and leaves the result identical to the V40 snapshot.

ALTER TABLE zentide_hub
  ADD COLUMN join_policy VARCHAR(20) NOT NULL DEFAULT 'OPEN' AFTER cover_url;

CREATE TABLE zentide_hub_category (
  category_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(10) NOT NULL,
  name VARCHAR(40) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(category_id),
  UNIQUE KEY uk_zentide_hub_category_user_name(user_id,name),
  KEY idx_zentide_hub_category_user_sort(user_id,sort_order,category_id),
  CONSTRAINT fk_zentide_hub_category_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE zentide_hub_member
  ADD COLUMN membership_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER role,
  ADD COLUMN category_id BIGINT UNSIGNED NULL AFTER membership_status,
  ADD COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) AFTER created_at,
  DROP KEY idx_zentide_hub_member_user,
  ADD KEY idx_zentide_hub_member_user(user_id,membership_status,created_at),
  ADD KEY idx_zentide_hub_member_category(user_id,category_id),
  ADD CONSTRAINT fk_zentide_hub_member_category FOREIGN KEY(category_id) REFERENCES zentide_hub_category(category_id) ON DELETE SET NULL;

CREATE TABLE zentide_hub_invitation (
  invitation_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  hub_id BIGINT UNSIGNED NOT NULL,
  created_by VARCHAR(10) NOT NULL,
  invite_token VARCHAR(64) NOT NULL,
  expires_at DATETIME(3) NULL,
  max_uses INT UNSIGNED NULL,
  use_count INT UNSIGNED NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(invitation_id),
  UNIQUE KEY uk_zentide_hub_invitation_token(invite_token),
  KEY idx_zentide_hub_invitation_hub(hub_id,status,created_at),
  CONSTRAINT fk_zentide_hub_invitation_hub FOREIGN KEY(hub_id) REFERENCES zentide_hub(hub_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_hub_invitation_creator FOREIGN KEY(created_by) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
