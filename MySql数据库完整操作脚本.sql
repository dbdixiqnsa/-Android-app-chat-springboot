CREATE DATABASE chat_app;

USE chat_app;

CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password VARCHAR(100) NOT NULL,
                       email VARCHAR(100) NOT NULL UNIQUE
);
ALTER TABLE users ADD COLUMN user_photo VARCHAR(255);
ALTER TABLE users ADD COLUMN nickname VARCHAR(100) DEFAULT '默认名字';

CREATE TABLE security_questions (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    user_id BIGINT NOT NULL,
                                    question VARCHAR(255) NOT NULL,
                                    answer VARCHAR(255) NOT NULL,
                                    UNIQUE(user_id, question),
                                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE friends (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         user_id BIGINT NOT NULL,
                         friend_id BIGINT NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         UNIQUE(user_id, friend_id),
                         FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                         FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE friend_requests (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 from_user_id BIGINT NOT NULL,
                                 to_user_id BIGINT NOT NULL,
                                 status ENUM('PENDING', 'ACCEPTED', 'DECLINED') DEFAULT 'PENDING',
                                 request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE CASCADE,
                                 FOREIGN KEY (to_user_id) REFERENCES users(id) ON DELETE CASCADE,
                                 UNIQUE (from_user_id, to_user_id)
);

CREATE TABLE messages (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          sender_id BIGINT NOT NULL,
                          receiver_id BIGINT NOT NULL,
                          content TEXT NOT NULL,
                          timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
                          FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE messages ADD COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE friend_requests ADD COLUMN is_viewed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE friend_requests ADD COLUMN viewed_by_to_user BOOLEAN DEFAULT FALSE;
ALTER TABLE friend_requests ADD COLUMN viewed_by_from_user BOOLEAN DEFAULT TRUE;


CREATE TABLE posts (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       user_id BIGINT NOT NULL,
                       title VARCHAR(255) NOT NULL,
                       content TEXT NOT NULL,
                       timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       like_count INT NOT NULL DEFAULT 0,
                       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE post_images (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             post_id BIGINT NOT NULL,
                             image_url VARCHAR(255) NOT NULL,
                             FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);
CREATE TABLE post_likes (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            post_id BIGINT NOT NULL,
                            user_id BIGINT NOT NULL,
                            timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE(post_id, user_id),
                            FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE friend_requests
    ADD COLUMN viewed_by_to_user BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN viewed_by_from_user  BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE friend_requests
    ADD COLUMN is_viewed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN viewed_by_to_user BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN viewed_by_from_user BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE friend_remarks (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                user_id BIGINT NOT NULL,
                                friend_id BIGINT NOT NULL,
                                remark VARCHAR(100),
                                UNIQUE(user_id, friend_id),
                                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE
);


