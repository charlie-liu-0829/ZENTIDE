-- 仅用于隔离环境或完成切换回滚前的人工确认。
-- 不要在仍有治理请求写入时执行；该脚本会永久删除 V47 表中的数据。
DROP TABLE IF EXISTS zentide_moderation_feedback;
DROP TABLE IF EXISTS zentide_governance_result;
DROP TABLE IF EXISTS zentide_governance_rule;
