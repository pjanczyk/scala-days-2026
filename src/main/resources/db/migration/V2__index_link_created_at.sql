-- For listing the newest links first
CREATE INDEX link_created_at_idx ON link (created_at);
