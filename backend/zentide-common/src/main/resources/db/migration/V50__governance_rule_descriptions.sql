ALTER TABLE zentide_governance_rule
  ADD COLUMN rule_description VARCHAR(2000) NULL AFTER violation_type;
