-- 治理 Agent 使用的站内信系统账号。仅用于发送审核结果，不允许登录。
INSERT INTO user_info(user_id,nick_name,avatar,email,password,sex,status)
SELECT 'SYSTEM','社区治理助手',NULL,'system-governance@zentide.local','!SYSTEM_ACCOUNT_NO_LOGIN!',2,1
WHERE NOT EXISTS (SELECT 1 FROM user_info WHERE user_id='SYSTEM');
