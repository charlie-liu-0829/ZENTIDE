SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS ow_notification_feedback;
DROP TABLE IF EXISTS ow_notification_batch_item;
DROP TABLE IF EXISTS ow_notification_batch;
DROP TABLE IF EXISTS ow_agent_analysis_evidence;
DROP TABLE IF EXISTS ow_agent_analysis;
DROP TABLE IF EXISTS ow_application_status_history;
DROP TABLE IF EXISTS ow_application_record;
DROP TABLE IF EXISTS ow_notification;
DROP TABLE IF EXISTS ow_user_event_match;
DROP TABLE IF EXISTS ow_subscription;
DROP TABLE IF EXISTS ow_change_event;
DROP TABLE IF EXISTS ow_job_presence_record;
DROP TABLE IF EXISTS ow_job_version;
DROP TABLE IF EXISTS ow_job;
DROP TABLE IF EXISTS ow_source_health_record;
DROP TABLE IF EXISTS ow_parser_run_record;
DROP TABLE IF EXISTS ow_page_snapshot;
DROP TABLE IF EXISTS ow_crawl_attempt;
DROP TABLE IF EXISTS ow_crawl_task;
DROP TABLE IF EXISTS ow_parser_configuration;
DROP TABLE IF EXISTS ow_url_security_check;
DROP TABLE IF EXISTS ow_source_review_record;
DROP TABLE IF EXISTS ow_recruitment_source;
DROP TABLE IF EXISTS ow_source_application;
DROP TABLE IF EXISTS ow_company_alias;
DROP TABLE IF EXISTS ow_company;
DROP TABLE IF EXISTS ow_user_preference;
DROP TABLE IF EXISTS ow_audit_log;
DROP TABLE IF EXISTS ow_privacy_request;

SET FOREIGN_KEY_CHECKS = 1;

UPDATE ow_platform_metadata
SET metadata_value = 'zentide-community-only'
WHERE metadata_key = 'platform_phase';
