-- ZENTIDE community demo data
--
-- Safe to run repeatedly. Every record is identified by a demo_* slug or a
-- demo_* email/title and is inserted only when it does not already exist.
-- This script is for local development/demo databases only; it does not
-- delete or overwrite existing user content.

SET NAMES utf8mb4;
USE zentide;

START TRANSACTION;

-- Demo accounts use a BCrypt hash for the password "password". They are not
-- production credentials and should never be used outside a local database.
INSERT IGNORE INTO user_info
  (user_id,nick_name,handle,email,password,sex,status)
VALUES
  ('9000000001','潮汐编辑','demo_editor','demo-editor@zentide.local',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',2,1),
  ('9000000002','现场观察员','demo_music','demo-music@zentide.local',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',2,1),
  ('9000000003','产品体验者','demo_tech','demo-tech@zentide.local',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',2,1),
  ('9000000004','周末玩家','demo_games','demo-games@zentide.local',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',2,1);

INSERT IGNORE INTO zentide_hub
  (slug,owner_id,name,description,category,join_policy,status,visibility)
VALUES
  ('demo-tech','9000000003','科技新品观察室','一起讨论新品发布、真实体验和那些值得买的变化。','TECHNOLOGY','OPEN','ACTIVE','PUBLIC'),
  ('demo-live-music','9000000002','城市现场音乐','演唱会、音乐节、购票情报和现场体验都在这里发生。','MUSIC','OPEN','ACTIVE','PUBLIC'),
  ('demo-weekend-games','9000000004','周末游戏局','记录新作、版本变化、攻略和周末一起玩的快乐。','GAMES','OPEN','ACTIVE','PUBLIC');

SELECT hub_id INTO @hub_tech FROM zentide_hub WHERE slug='demo-tech' LIMIT 1;
SELECT hub_id INTO @hub_music FROM zentide_hub WHERE slug='demo-live-music' LIMIT 1;
SELECT hub_id INTO @hub_games FROM zentide_hub WHERE slug='demo-weekend-games' LIMIT 1;

-- Members: the owner plus a few active members for realistic member counts.
INSERT IGNORE INTO zentide_hub_member
  (hub_id,user_id,role,permissions_json,membership_status,notification_mode)
VALUES
  (@hub_tech,'9000000003','OWNER',JSON_ARRAY(), 'ACTIVE','HIGHLIGHTS'),
  (@hub_tech,'9000000001','MEMBER',JSON_ARRAY(), 'ACTIVE','ALL'),
  (@hub_tech,'9000000002','MEMBER',JSON_ARRAY(), 'ACTIVE','HIGHLIGHTS'),
  (@hub_music,'9000000002','OWNER',JSON_ARRAY(), 'ACTIVE','HIGHLIGHTS'),
  (@hub_music,'9000000001','MEMBER',JSON_ARRAY(), 'ACTIVE','ALL'),
  (@hub_music,'9000000004','MEMBER',JSON_ARRAY(), 'ACTIVE','HIGHLIGHTS'),
  (@hub_games,'9000000004','OWNER',JSON_ARRAY(), 'ACTIVE','HIGHLIGHTS'),
  (@hub_games,'9000000001','MEMBER',JSON_ARRAY(), 'ACTIVE','HIGHLIGHTS'),
  (@hub_games,'9000000002','MEMBER',JSON_ARRAY(), 'ACTIVE','ALL');

-- Interest entities make the relationship between a scene and its concrete
-- objects visible in the UI.
INSERT INTO zentide_interest_entity
  (hub_id,entity_type,name,subtitle,metadata_json,status)
SELECT @hub_tech,'PRODUCT','下一代轻薄本','关注发布、续航和真实办公体验。',JSON_OBJECT('demo',true,'kind','product'),'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM zentide_interest_entity WHERE hub_id=@hub_tech AND name='下一代轻薄本');
INSERT INTO zentide_interest_entity
  (hub_id,entity_type,name,subtitle,metadata_json,status)
SELECT @hub_music,'TOUR','秋日城市巡演','歌单、场馆、视野和现场记录。',JSON_OBJECT('demo',true,'kind','tour'),'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM zentide_interest_entity WHERE hub_id=@hub_music AND name='秋日城市巡演');
INSERT INTO zentide_interest_entity
  (hub_id,entity_type,name,subtitle,metadata_json,status)
SELECT @hub_games,'GAME','本季度合作新作','版本更新、组队和新手体验。',JSON_OBJECT('demo',true,'kind','game'),'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM zentide_interest_entity WHERE hub_id=@hub_games AND name='本季度合作新作');

SELECT entity_id INTO @entity_tech FROM zentide_interest_entity WHERE hub_id=@hub_tech AND name='下一代轻薄本' LIMIT 1;
SELECT entity_id INTO @entity_music FROM zentide_interest_entity WHERE hub_id=@hub_music AND name='秋日城市巡演' LIMIT 1;
SELECT entity_id INTO @entity_games FROM zentide_interest_entity WHERE hub_id=@hub_games AND name='本季度合作新作' LIMIT 1;

INSERT INTO zentide_interest_event
  (hub_id,entity_id,title,description,starts_at,ends_at,venue,status)
SELECT @hub_tech,@entity_tech,'新品发布直播讨论','边看发布会边讨论参数、价格和真实需求。','2026-09-12 19:30:00','2026-09-12 21:00:00','线上直播','UPCOMING'
WHERE NOT EXISTS (SELECT 1 FROM zentide_interest_event WHERE hub_id=@hub_tech AND title='新品发布直播讨论');
INSERT INTO zentide_interest_event
  (hub_id,entity_id,title,description,starts_at,ends_at,venue,status)
SELECT @hub_music,@entity_music,'秋日巡演观演交换','演出前交换购票信息，演出后分享现场体验。','2026-09-20 18:30:00','2026-09-20 22:00:00','城市音乐厅','UPCOMING'
WHERE NOT EXISTS (SELECT 1 FROM zentide_interest_event WHERE hub_id=@hub_music AND title='秋日巡演观演交换');
INSERT INTO zentide_interest_event
  (hub_id,entity_id,title,description,starts_at,ends_at,venue,status)
SELECT @hub_games,@entity_games,'周末合作游戏开黑','周六晚上组队，欢迎新手加入。','2026-09-13 20:00:00','2026-09-13 23:00:00','线上语音房','UPCOMING'
WHERE NOT EXISTS (SELECT 1 FROM zentide_interest_event WHERE hub_id=@hub_games AND title='周末合作游戏开黑');

SELECT event_id INTO @event_tech FROM zentide_interest_event WHERE hub_id=@hub_tech AND title='新品发布直播讨论' LIMIT 1;
SELECT event_id INTO @event_music FROM zentide_interest_event WHERE hub_id=@hub_music AND title='秋日巡演观演交换' LIMIT 1;
SELECT event_id INTO @event_games FROM zentide_interest_event WHERE hub_id=@hub_games AND title='周末合作游戏开黑' LIMIT 1;

-- Demo-specific topics are deliberately prefixed so they are easy to remove
-- from a local database later without touching real community topics.
INSERT INTO zentide_topic(canonical_name,topic_type,aliases_json,status)
SELECT 'demo·真实续航','INTEREST',JSON_ARRAY('续航','电池'),'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM zentide_topic WHERE canonical_name='demo·真实续航');
INSERT INTO zentide_topic(canonical_name,topic_type,aliases_json,status)
SELECT 'demo·现场视野','INTEREST',JSON_ARRAY('看台','视野'),'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM zentide_topic WHERE canonical_name='demo·现场视野');
INSERT INTO zentide_topic(canonical_name,topic_type,aliases_json,status)
SELECT 'demo·新手组队','INTEREST',JSON_ARRAY('组队','开黑'),'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM zentide_topic WHERE canonical_name='demo·新手组队');

SELECT topic_id INTO @topic_tech FROM zentide_topic WHERE canonical_name='demo·真实续航' LIMIT 1;
SELECT topic_id INTO @topic_music FROM zentide_topic WHERE canonical_name='demo·现场视野' LIMIT 1;
SELECT topic_id INTO @topic_games FROM zentide_topic WHERE canonical_name='demo·新手组队' LIMIT 1;

INSERT IGNORE INTO zentide_topic_link(topic_id,hub_id,link_role,featured,created_by)
VALUES
  (@topic_tech,@hub_tech,'COMMUNITY_TOPIC',1,'demo-seed'),
  (@topic_music,@hub_music,'COMMUNITY_TOPIC',1,'demo-seed'),
  (@topic_games,@hub_games,'COMMUNITY_TOPIC',1,'demo-seed');

-- Posts are keyed by their demo title within a scene, so this block is
-- idempotent and can be safely rerun after changing the seed content.
INSERT INTO zentide_interest_post
  (hub_id,author_id,entity_id,event_id,post_type,title,body,media_json,status,like_count,comment_count,bookmark_count,view_count)
SELECT @hub_tech,'9000000003',@entity_tech,@event_tech,'EXPERIENCE','这台新品最值得关注的，可能不是参数','<p>我更想知道它在真实办公场景里能不能安静、稳定地工作一整天。</p><p>如果续航和散热都过关，参数之外的体验才会真正影响选择。</p>','[]','PUBLISHED',8,0,0,42
WHERE NOT EXISTS (SELECT 1 FROM zentide_interest_post WHERE hub_id=@hub_tech AND title='这台新品最值得关注的，可能不是参数');
INSERT INTO zentide_interest_post
  (hub_id,author_id,entity_id,event_id,post_type,title,body,media_json,status,like_count,comment_count,bookmark_count,view_count)
SELECT @hub_tech,'9000000001',@entity_tech,@event_tech,'QUESTION','大家买新品时最看重哪一个指标？','<p>如果只能选一个，你会优先考虑续航、重量、屏幕还是售后？欢迎说说理由。</p>','[]','PUBLISHED',5,0,0,31
WHERE NOT EXISTS (SELECT 1 FROM zentide_interest_post WHERE hub_id=@hub_tech AND title='大家买新品时最看重哪一个指标？');
INSERT INTO zentide_interest_post
  (hub_id,author_id,entity_id,event_id,post_type,title,body,media_json,status,like_count,comment_count,bookmark_count,view_count)
SELECT @hub_music,'9000000002',@entity_music,@event_music,'REVIEW','小场馆观演，提前选位置真的很重要','<p>这次现场最惊喜的是声音，最需要做功课的是座位视野。</p><p>以后会提前看场馆图，也会把适合第一次去的区域整理出来。</p>','[]','PUBLISHED',15,0,0,76
WHERE NOT EXISTS (SELECT 1 FROM zentide_interest_post WHERE hub_id=@hub_music AND title='小场馆观演，提前选位置真的很重要');
INSERT INTO zentide_interest_post
  (hub_id,author_id,entity_id,event_id,post_type,title,body,media_json,status,like_count,comment_count,bookmark_count,view_count)
SELECT @hub_music,'9000000001',@entity_music,@event_music,'DISCUSSION','第一次去现场，大家会提前多久到？','<p>想听听不同场馆的经验：检票、周边和入场之后，哪个环节最容易排队？</p>','[]','PUBLISHED',6,0,0,28
WHERE NOT EXISTS (SELECT 1 FROM zentide_interest_post WHERE hub_id=@hub_music AND title='第一次去现场，大家会提前多久到？');
INSERT INTO zentide_interest_post
  (hub_id,author_id,entity_id,event_id,post_type,title,body,media_json,status,like_count,comment_count,bookmark_count,view_count)
SELECT @hub_games,'9000000004',@entity_games,@event_games,'DISCUSSION','周末开黑缺一位，欢迎新手','<p>周六晚上八点开始，主打轻松合作，不卷排名。第一次玩的朋友也可以来。</p>','[]','PUBLISHED',11,0,0,53
WHERE NOT EXISTS (SELECT 1 FROM zentide_interest_post WHERE hub_id=@hub_games AND title='周末开黑缺一位，欢迎新手');
INSERT INTO zentide_interest_post
  (hub_id,author_id,entity_id,event_id,post_type,title,body,media_json,status,like_count,comment_count,bookmark_count,view_count)
SELECT @hub_games,'9000000002',@entity_games,@event_games,'EXPERIENCE','这次更新后，新手教程终于有用了','<p>更新后的引导更清楚了，第一次接触这类游戏也能快速理解合作目标。</p><p>如果你最近刚开始玩，可以先从教程和低难度关卡入手。</p>','[]','PUBLISHED',9,0,0,39
WHERE NOT EXISTS (SELECT 1 FROM zentide_interest_post WHERE hub_id=@hub_games AND title='这次更新后，新手教程终于有用了');

SELECT post_id INTO @post_tech_1 FROM zentide_interest_post WHERE hub_id=@hub_tech AND title='这台新品最值得关注的，可能不是参数' LIMIT 1;
SELECT post_id INTO @post_tech_2 FROM zentide_interest_post WHERE hub_id=@hub_tech AND title='大家买新品时最看重哪一个指标？' LIMIT 1;
SELECT post_id INTO @post_music_1 FROM zentide_interest_post WHERE hub_id=@hub_music AND title='小场馆观演，提前选位置真的很重要' LIMIT 1;
SELECT post_id INTO @post_music_2 FROM zentide_interest_post WHERE hub_id=@hub_music AND title='第一次去现场，大家会提前多久到？' LIMIT 1;
SELECT post_id INTO @post_games_1 FROM zentide_interest_post WHERE hub_id=@hub_games AND title='周末开黑缺一位，欢迎新手' LIMIT 1;
SELECT post_id INTO @post_games_2 FROM zentide_interest_post WHERE hub_id=@hub_games AND title='这次更新后，新手教程终于有用了' LIMIT 1;

INSERT IGNORE INTO zentide_topic_link(topic_id,post_id,link_role,created_by)
VALUES
  (@topic_tech,@post_tech_1,'POST_TOPIC','demo-seed'),(@topic_tech,@post_tech_2,'POST_TOPIC','demo-seed'),
  (@topic_music,@post_music_1,'POST_TOPIC','demo-seed'),(@topic_music,@post_music_2,'POST_TOPIC','demo-seed'),
  (@topic_games,@post_games_1,'POST_TOPIC','demo-seed'),(@topic_games,@post_games_2,'POST_TOPIC','demo-seed');

-- Comments provide a visible threaded discussion on each scene.
INSERT INTO zentide_interest_comment(post_id,author_id,body,status)
SELECT @post_tech_1,'9000000001','我也更关心一天使用下来是否稳定。','PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM zentide_interest_comment WHERE post_id=@post_tech_1 AND body='我也更关心一天使用下来是否稳定。');
INSERT INTO zentide_interest_comment(post_id,author_id,body,status)
SELECT @post_music_1,'9000000004','小场馆的前排和中间区域体验差别确实很大。','PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM zentide_interest_comment WHERE post_id=@post_music_1 AND body='小场馆的前排和中间区域体验差别确实很大。');
INSERT INTO zentide_interest_comment(post_id,author_id,body,status)
SELECT @post_games_1,'9000000002','新手可以来，我也愿意带大家熟悉流程。','PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM zentide_interest_comment WHERE post_id=@post_games_1 AND body='新手可以来，我也愿意带大家熟悉流程。');

SELECT comment_id INTO @comment_tech FROM zentide_interest_comment WHERE post_id=@post_tech_1 AND body='我也更关心一天使用下来是否稳定。' LIMIT 1;
INSERT INTO zentide_interest_comment(post_id,author_id,parent_comment_id,body,status)
SELECT @post_tech_1,'9000000003',@comment_tech,'如果发布会现场有实测，我会第一时间整理出来。','PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM zentide_interest_comment WHERE post_id=@post_tech_1 AND body='如果发布会现场有实测，我会第一时间整理出来。');

-- A few typed actions make the counters and personal interactions visible.
INSERT INTO zentide_user_action(user_id,post_id,action_type,action_value,status)
SELECT '9000000001',@post_tech_1,'REACTION','LIKE','ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM zentide_user_action WHERE user_id='9000000001' AND post_id=@post_tech_1 AND action_type='REACTION');
INSERT INTO zentide_user_action(user_id,post_id,action_type,action_value,status)
SELECT '9000000002',@post_music_1,'BOOKMARK','BOOKMARKED','ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM zentide_user_action WHERE user_id='9000000002' AND post_id=@post_music_1 AND action_type='BOOKMARK');
INSERT INTO zentide_user_action(user_id,event_id,action_type,action_value,status)
SELECT '9000000001',@event_music,'ATTENDANCE','WANT','ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM zentide_user_action WHERE user_id='9000000001' AND event_id=@event_music AND action_type='ATTENDANCE');

-- Recalculate denormalized counters from the seeded relationships.
UPDATE zentide_hub h
SET member_count=(SELECT COUNT(*) FROM zentide_hub_member m WHERE m.hub_id=h.hub_id AND m.membership_status='ACTIVE'),
    post_count=(SELECT COUNT(*) FROM zentide_interest_post p WHERE p.hub_id=h.hub_id AND p.status='PUBLISHED')
WHERE h.slug IN ('demo-tech','demo-live-music','demo-weekend-games');
UPDATE zentide_interest_post p
SET comment_count=(SELECT COUNT(*) FROM zentide_interest_comment c WHERE c.post_id=p.post_id AND c.status='PUBLISHED'),
    like_count=GREATEST(like_count,(SELECT COUNT(*) FROM zentide_user_action a WHERE a.post_id=p.post_id AND a.action_type='REACTION' AND a.action_value='LIKE' AND a.status='ACTIVE')),
    bookmark_count=GREATEST(bookmark_count,(SELECT COUNT(*) FROM zentide_user_action a WHERE a.post_id=p.post_id AND a.action_type='BOOKMARK' AND a.status='ACTIVE'))
WHERE p.post_id IN (@post_tech_1,@post_tech_2,@post_music_1,@post_music_2,@post_games_1,@post_games_2);
-- MySQL forbids reading the target table directly from a subquery in an
-- UPDATE. Materialize the counts in a temporary table first.
DROP TEMPORARY TABLE IF EXISTS zentide_demo_topic_counts;
CREATE TEMPORARY TABLE zentide_demo_topic_counts (
  topic_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  counter_value INT UNSIGNED NOT NULL
) ENGINE=InnoDB;
INSERT INTO zentide_demo_topic_counts(topic_id,counter_value)
SELECT topic_id,COUNT(*)
FROM zentide_topic_link
WHERE post_id IS NOT NULL AND status='ACTIVE'
GROUP BY topic_id;
UPDATE zentide_topic_link l
JOIN zentide_topic t ON t.topic_id=l.topic_id
JOIN zentide_demo_topic_counts c ON c.topic_id=l.topic_id
SET l.counter_value=c.counter_value
WHERE l.hub_id IS NOT NULL AND t.canonical_name LIKE 'demo·%';
DROP TEMPORARY TABLE zentide_demo_topic_counts;

COMMIT;

SELECT 'demo seed complete' AS result;
SELECT h.slug,h.name,h.member_count,h.post_count
FROM zentide_hub h
WHERE h.slug IN ('demo-tech','demo-live-music','demo-weekend-games')
ORDER BY h.hub_id;
