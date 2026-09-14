INSERT INTO ow_zentide_interest_entity(hub_id, entity_type, name, subtitle, metadata_json)
SELECT h.hub_id, 'PRODUCT', '下一代旗舰手机', '关注发布会、真实体验与长期使用反馈。', JSON_OBJECT('kind','technology')
FROM ow_zentide_hub h WHERE h.slug='tech-new'
  AND NOT EXISTS (SELECT 1 FROM ow_zentide_interest_entity e WHERE e.hub_id=h.hub_id AND e.name='下一代旗舰手机');

INSERT INTO ow_zentide_interest_entity(hub_id, entity_type, name, subtitle, metadata_json)
SELECT h.hub_id, 'TOUR', '城市现场音乐周末', '演出信息、购票提醒、歌单和现场体验。', JSON_OBJECT('kind','music')
FROM ow_zentide_hub h WHERE h.slug='live-music'
  AND NOT EXISTS (SELECT 1 FROM ow_zentide_interest_entity e WHERE e.hub_id=h.hub_id AND e.name='城市现场音乐周末');

INSERT INTO ow_zentide_interest_entity(hub_id, entity_type, name, subtitle, metadata_json)
SELECT h.hub_id, 'GAME', '年度游戏新作', '版本消息、攻略、组队与真实游玩反馈。', JSON_OBJECT('kind','game')
FROM ow_zentide_hub h WHERE h.slug='games'
  AND NOT EXISTS (SELECT 1 FROM ow_zentide_interest_entity e WHERE e.hub_id=h.hub_id AND e.name='年度游戏新作');

INSERT INTO ow_zentide_interest_entity(hub_id, entity_type, name, subtitle, metadata_json)
SELECT h.hub_id, 'MODEL', '新一代通用模型', '能力变化、工作流和真实使用体验。', JSON_OBJECT('kind','ai')
FROM ow_zentide_hub h WHERE h.slug='ai-lab'
  AND NOT EXISTS (SELECT 1 FROM ow_zentide_interest_entity e WHERE e.hub_id=h.hub_id AND e.name='新一代通用模型');

UPDATE ow_zentide_interest_event event_item
JOIN ow_zentide_hub h ON h.hub_id=event_item.hub_id
JOIN ow_zentide_interest_entity entity_item ON entity_item.hub_id=h.hub_id
SET event_item.entity_id=entity_item.entity_id
WHERE event_item.entity_id IS NULL
  AND ((h.slug='tech-new' AND entity_item.name='下一代旗舰手机')
    OR (h.slug='live-music' AND entity_item.name='城市现场音乐周末'));
