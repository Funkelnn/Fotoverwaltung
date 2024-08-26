-- Benutzer Tabelle
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('user', 'admin') NOT NULL DEFAULT 'user',
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Foto Tabelle
CREATE TABLE photos (
    photo_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
	filepath VARCHAR(255) NOT NULL,
    title VARCHAR(100) NOT NULL,
	capture_date DATE NOT NULL,
    capture_time TIME,
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6),
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Fotoalbum Tabelle
CREATE TABLE albums (
    album_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Alben-Foto-Zuordnung Tabelle
CREATE TABLE album_photo (
    album_id INT NOT NULL,
    photo_id INT NOT NULL,
    PRIMARY KEY (album_id, photo_id),
    FOREIGN KEY (album_id) REFERENCES albums(album_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (photo_id) REFERENCES photos(photo_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Schlagwort Tabelle
CREATE TABLE tags (
    tag_id INT AUTO_INCREMENT PRIMARY KEY,
	user_id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Foto-Schlagwort Tabelle
CREATE TABLE photo_tags (
    photo_id INT NOT NULL,
    tag_id INT NOT NULL,
    PRIMARY KEY (photo_id, tag_id),
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (photo_id) REFERENCES photos(photo_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Album-Schlagwort Tabelle
CREATE TABLE album_tags (
    album_id INT NOT NULL,
    tag_id INT NOT NULL,
    PRIMARY KEY (album_id, tag_id),
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (album_id) REFERENCES albums(album_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE ON UPDATE CASCADE
);