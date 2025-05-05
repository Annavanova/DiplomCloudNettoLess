CREATE TABLE files (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    size BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_file_user FOREIGN KEY (user_id) REFERENCES users(id)
);




