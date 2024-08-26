-- Initialer Admin-Nutzer
-- admin:adminpass
-- Passwort wurde mit bcrypt gehast
INSERT INTO users (username, password_hash, role) VALUES ('admin', '$2a$10$pRQu4uo9Vvi72EbNW/VKJehcNAJcHA/aTDIq/QKmMKIJtB8QDQSZq', 'admin');