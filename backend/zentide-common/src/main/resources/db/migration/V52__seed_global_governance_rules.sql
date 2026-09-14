-- 全站默认治理规则。规则由 Agent 结合帖子语义判断，不依赖关键词命中。
INSERT INTO zentide_governance_rule(scene_id,violation_type,rule_description,keywords_json,severity,enabled)
SELECT 0,'广告刷屏',
  '禁止在社区内重复发布未经允许的商品推广、促销广告或引流内容。如果帖子包含商品宣传、优惠促销、购买引导、外部联系方式或外部链接，并且存在重复发布、重复营销文案、短时间连续发布等行为，应判定为广告刷屏。单纯分享个人体验、真实测评或非营销性质的公开资料，不判定为广告刷屏。',
  '[]','high',1
WHERE NOT EXISTS (SELECT 1 FROM zentide_governance_rule WHERE violation_type='广告刷屏');

INSERT INTO zentide_governance_rule(scene_id,violation_type,rule_description,keywords_json,severity,enabled)
SELECT 0,'敏感信息泄露',
  '禁止发布个人隐私、账号凭证和系统密钥，包括手机号、身份证号、邮箱、住址、密码、访问令牌、API Key、私钥以及可用于登录或控制系统的完整凭据。公开的脱敏示例、占位符和已经失效的演示值不应判定为违规；发现真实可用凭据时应判定为高风险。',
  '[]','high',1
WHERE NOT EXISTS (SELECT 1 FROM zentide_governance_rule WHERE violation_type='敏感信息泄露');

INSERT INTO zentide_governance_rule(scene_id,violation_type,rule_description,keywords_json,severity,enabled)
SELECT 0,'人身攻击与骚扰',
  '禁止针对具体用户、群体或身份特征进行侮辱、威胁、诅咒、歧视、持续骚扰或煽动围攻。对观点、作品或产品进行有事实依据的批评不属于人身攻击；语气激烈但没有指向具体对象时应结合上下文谨慎判断，必要时进入人工审核。',
  '[]','medium',1
WHERE NOT EXISTS (SELECT 1 FROM zentide_governance_rule WHERE violation_type='人身攻击与骚扰');

INSERT INTO zentide_governance_rule(scene_id,violation_type,rule_description,keywords_json,severity,enabled)
SELECT 0,'虚假信息与恶意误导',
  '禁止故意捏造事实、伪造官方公告、冒充他人或机构、断章取义制造误导，并以确定语气传播可能造成现实损害的未经证实信息。个人观点、经验分享和明确标注为推测的内容不应直接判定为违规；涉及公共安全、健康、金融或紧急事件的高影响虚假信息应提高风险等级。',
  '[]','high',1
WHERE NOT EXISTS (SELECT 1 FROM zentide_governance_rule WHERE violation_type='虚假信息与恶意误导');

INSERT INTO zentide_governance_rule(scene_id,violation_type,rule_description,keywords_json,severity,enabled)
SELECT 0,'违法交易与危险行为',
  '禁止在社区内组织或推广违法交易、诈骗、洗钱、毒品、武器、盗号、恶意软件、危险品或其他明显违法及高危行为。技术讨论、新闻报道、风险教育和合法合规的安全研究可以保留，但不得提供可直接执行的交易、实施或规避监管指引。',
  '[]','high',1
WHERE NOT EXISTS (SELECT 1 FROM zentide_governance_rule WHERE violation_type='违法交易与危险行为');

INSERT INTO zentide_governance_rule(scene_id,violation_type,rule_description,keywords_json,severity,enabled)
SELECT 0,'低质量重复灌水',
  '禁止短时间内连续发布相同或高度相似的无实质内容、无意义顶帖、重复链接和刷屏式回复。正常的补充说明、对同一问题的不同角度讨论，以及经过实质更新的版本不应判定为灌水。',
  '[]','medium',1
WHERE NOT EXISTS (SELECT 1 FROM zentide_governance_rule WHERE violation_type='低质量重复灌水');
