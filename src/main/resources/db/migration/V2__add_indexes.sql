CREATE INDEX idx_tasks_deleted_at ON tasks (deleted_at);

CREATE INDEX idx_tasks_status_deleted_at ON tasks (status, deleted_at);

CREATE INDEX idx_tasks_due_date_deleted_at ON tasks (due_date, deleted_at);

CREATE INDEX idx_tasks_created_at ON tasks (created_at);

CREATE INDEX idx_task_tags_tag_id ON task_tags (tag_id);

CREATE INDEX idx_task_tags_task_id ON task_tags (task_id);

CREATE INDEX idx_tags_name ON tags (name);
