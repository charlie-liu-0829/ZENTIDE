-- Platform post types are administrator-managed; hub owners may add scoped types.

CREATE TABLE zentide_post_type (
  post_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  hub_id BIGINT UNSIGNED NULL,
  type_code VARCHAR(24) NOT NULL,
  display_name VARCHAR(20) NOT NULL,
  description VARCHAR(160) NULL,
  system_fixed TINYINT(1) NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL DEFAULT 0,
  created_by VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(post_type_id),
  UNIQUE KEY uk_zentide_post_type_code(type_code),
  KEY idx_zentide_post_type_scope(hub_id,system_fixed,status,sort_order),
  CONSTRAINT fk_zentide_post_type_hub FOREIGN KEY(hub_id) REFERENCES zentide_hub(hub_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO zentide_post_type(hub_id,type_code,display_name,description,system_fixed,status,sort_order,created_by) VALUES
  (NULL,'DISCUSSION','讨论','交换观点、信息和开放式想法',1,'ACTIVE',10,'system'),
  (NULL,'QUESTION','提问','提出明确问题并向社区寻求回答',1,'ACTIVE',20,'system'),
  (NULL,'EXPERIENCE','经验','分享亲身经历、方法和实践过程',1,'ACTIVE',30,'system'),
  (NULL,'REVIEW','评测','围绕作品、产品或活动给出完整评价',1,'ACTIVE',40,'system'),
  (NULL,'EVENT','活动','发起或分享与兴趣相关的线下线上活动',1,'ACTIVE',50,'system'),
  (NULL,'POLL','投票','通过选项快速收集社区成员的看法',1,'ACTIVE',60,'system');
