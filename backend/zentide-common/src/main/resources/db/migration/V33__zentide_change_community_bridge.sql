-- A community post can be grounded in one verified change. Topic mappings keep
-- the reference within the matching interest hub rather than creating a second
-- parallel discussion system.
ALTER TABLE ow_zentide_interest_post
  ADD COLUMN change_id BIGINT UNSIGNED NULL AFTER event_id,
  ADD KEY idx_zentide_post_change (change_id, created_at),
  ADD CONSTRAINT fk_zentide_post_change
    FOREIGN KEY (change_id) REFERENCES ow_zentide_change(change_id) ON DELETE SET NULL;
