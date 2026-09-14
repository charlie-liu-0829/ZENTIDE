-- Platform-managed directions for interest hubs.
CREATE TABLE IF NOT EXISTS zentide_hub_direction (
  direction_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code VARCHAR(40) NOT NULL,
  display_name VARCHAR(40) NOT NULL,
  description VARCHAR(160) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL DEFAULT 0,
  created_by VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(direction_id),
  UNIQUE KEY uk_zentide_hub_direction_code(code),
  KEY idx_zentide_hub_direction_status_sort(status,sort_order,direction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO zentide_hub_direction(code,display_name,description,status,sort_order,created_by) VALUES
 ('GENERAL','综合兴趣','跨领域讨论与未归类的兴趣现场','ACTIVE',10,'system'),
 ('TECHNOLOGY','科技','产品、工具、模型与技术趋势','ACTIVE',20,'system'),
 ('AI','AI','人工智能模型、工具与工作流','ACTIVE',25,'system'),
 ('MUSIC','音乐','演出、专辑、乐队与现场体验','ACTIVE',30,'system'),
 ('GAMES','游戏','游戏作品、版本、攻略与组队','ACTIVE',40,'system'),
 ('LIFESTYLE','生活方式','城市生活、消费与日常兴趣','ACTIVE',50,'system'),
 ('CULTURE','文化','影视、阅读、艺术与文化观察','ACTIVE',60,'system'),
 ('SPORTS','运动','赛事、训练与运动装备','ACTIVE',70,'system');
