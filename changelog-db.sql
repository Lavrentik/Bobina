CREATE DATABASE IF NOT EXISTS bobina;

USE bobina;

CREATE TABLE IF NOT EXISTS users(
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(20) NOT NULL UNIQUE
);

INSERT INTO users VALUES (1, 'admin');
INSERT INTO users VALUES (100, 'user1');
INSERT INTO users (username) VALUES ('user2');

CREATE TABLE IF NOT EXISTS operations(
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    query VARCHAR(1024) NOT NULL,
    title VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(256)
);

INSERT INTO operations VALUES (100,'INSERT INTO users (username) VALUES (\'<@username@>\');', 'addUser', 'Add new user into database.');
INSERT INTO operations (query, title, description) VALUES ('DELETE FROM users WHERE username = \'<@username@>\';', 'removeUser', 'Remove existing user from database.');
INSERT INTO operations (query, title, description) VALUES ('INSERT INTO operations (query, title, description) VALUES (\'<@query@>\', \'<@title@>\', \'<@description@>\');', 'addOperation', 'Add new operation.');

CREATE TABLE IF NOT EXISTS permissions(
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    operation_id INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (operation_id) REFERENCES operations(id) ON DELETE CASCADE ON UPDATE CASCADE
);

INSERT INTO permissions (user_id, operation_id) VALUES (1, 100);
INSERT INTO permissions (user_id, operation_id) VALUES (1, 101);
INSERT INTO permissions (user_id, operation_id) VALUES (1, 102);

INSERT INTO operations (query, title, description) VALUES ('INSERT INTO permissions (user_id, operation_id) VALUES (<@user_id@>, <@operation_id@>);', 'addPermission', 'Add permission on executing operation for user.');
INSERT INTO operations (query, title, description) VALUES ('DELETE FROM permissions WHERE user_id = <@user_id@> AND operation_id = <@operation_id@>;', 'removePermission', 'Remove permission on executing operation for user.');

INSERT INTO permissions (user_id, operation_id) VALUES (1, 103);
INSERT INTO permissions (user_id, operation_id) VALUES (1, 104);
