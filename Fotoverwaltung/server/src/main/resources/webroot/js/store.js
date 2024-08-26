import { getUsers, getPhotos, getAlbums, getTags, getUserById, getPhotosByAlbum, downloadPhoto, getPhotoTags, getAlbumTags, addTagToPhoto, removeTagFromPhoto, updatePhoto, deletePhoto, createTag, deleteTag, addPhotoToAlbum, removePhotoFromAlbum, uploadPhoto, createAlbum, addTagToAlbum, removeTagFromAlbum, updateAlbum, deleteAlbum, deleteUser, createUser, updateUser } from './api.js';
import { eventBus } from "./utils/eventBus.js";
class Store {
    users = [];
    photos = [];
    albums = [];
    tags = [];
    currentUser = null;
    setUsers(users) {
        this.users = users;
    }
    getUsers() {
        return this.users;
    }
    getUserById(userId) {
        return this.users.find(user => user.user_id === userId);
    }
    setCurrentUser(user) {
        this.currentUser = user;
    }
    getCurrentUser() {
        return this.currentUser;
    }
    setPhotos(photos) {
        this.photos = photos;
    }
    getPhotos() {
        return this.photos;
    }
    setAlbums(albums) {
        this.albums = albums;
    }
    getAlbums() {
        return this.albums;
    }
    getAlbumById(albumId) {
        return this.albums.find(album => album.album_id === albumId);
    }
    setTags(tags) {
        this.tags = tags;
    }
    getTags() {
        return this.tags;
    }
    getTagById(tagId) {
        return this.tags.find(tag => tag.tag_id === tagId);
    }
    /*
      Fotos in Alben
     */
    async setPhotosByAlbum(albumId) {
        const album = this.albums.find(album => album.album_id === albumId);
        if (album) {
            const photos = await getPhotosByAlbum(albumId);
            album.photo_ids = photos.map(photo => photo.photo_id);
            for (const photo of photos) {
                if (!this.photos.find(p => p.photo_id === photo.photo_id)) {
                    this.photos.push(photo);
                }
            }
        }
    }
    async addPhotoToAlbum(photoId, albumId) {
        await addPhotoToAlbum(photoId, albumId);
        const album = this.albums.find(a => a.album_id === albumId);
        if (album && !album.photo_ids.includes(photoId)) {
            album.photo_ids.push(photoId);
        }
    }
    async removePhotoFromAlbum(photoId, albumId) {
        await removePhotoFromAlbum(photoId, albumId); // API-Aufruf zum Entfernen des Fotos aus dem Album
        const album = this.albums.find(a => a.album_id === albumId);
        if (album) {
            album.photo_ids = album.photo_ids.filter(id => id !== photoId); // Entfernen der Foto-ID aus dem Album
        }
        eventBus.emit('albumUpdated', albumId);
    }
    /*
      Tags
     */
    async createTag(name) {
        await createTag(name);
        await this.loadTags(); // Alle Tags neu laden
        const newTag = this.tags.find(tag => tag.name === name);
        if (!newTag) {
            throw new Error('Tag creation failed.');
        }
        return newTag;
    }
    async loadTags() {
        this.tags = await getTags();
    }
    async deleteTag(tagId) {
        await deleteTag(tagId);
        this.tags = this.tags.filter(tag => tag.tag_id !== tagId);
        this.photos.forEach(photo => {
            photo.tag_ids = photo.tag_ids.filter(id => id !== tagId);
        });
        this.albums.forEach(album => {
            album.tag_ids = album.tag_ids.filter(id => id !== tagId);
        });
    }
    async addTagToPhoto(photoId, tagId) {
        await addTagToPhoto(photoId, tagId);
        const photo = this.photos.find(p => p.photo_id === photoId);
        if (photo && !photo.tag_ids.includes(tagId)) {
            photo.tag_ids.push(tagId);
        }
    }
    async removeTagFromPhoto(photoId, tagId) {
        await removeTagFromPhoto(photoId, tagId);
        const photo = this.photos.find(p => p.photo_id === photoId);
        if (photo) {
            photo.tag_ids = photo.tag_ids.filter(id => id !== tagId);
        }
    }
    async addTagToAlbum(albumId, tagId) {
        await addTagToAlbum(albumId, tagId);
        const album = this.getAlbumById(albumId);
        if (album) {
            if (!album.tag_ids.includes(tagId)) {
                album.tag_ids.push(tagId);
            }
            eventBus.emit('albumUpdated', albumId); // Event auslösen, wenn das Album aktualisiert wurde
        }
    }
    async removeTagFromAlbum(albumId, tagId) {
        await removeTagFromAlbum(albumId, tagId);
        const album = this.getAlbumById(albumId);
        if (album) {
            album.tag_ids = album.tag_ids.filter(id => id !== tagId);
        }
        eventBus.emit('albumUpdated', albumId);
    }
    /*
      Foto Methoden
     */
    async updatePhoto(photoId, updates) {
        const photo = this.photos.find(p => p.photo_id === photoId);
        if (photo) {
            Object.assign(photo, updates); // Lokale Kopie aktualisieren
            await updatePhoto(photoId, updates); // API-Aufruf zum Aktualisieren des Fotos in der Datenbank
            eventBus.emit('photoUpdated', photo); // Event für die Aktualisierung des Grids
        }
    }
    async deletePhoto(photoId) {
        await deletePhoto(photoId); // API-Aufruf zum Löschen des Fotos in der Datenbank
        this.photos = this.photos.filter(photo => photo.photo_id !== photoId); // Lokale Kopie aktualisieren
        eventBus.emit('photoDeleted', photoId); // Event für die Aktualisierung des Grids
    }
    async uploadPhoto(formData) {
        await uploadPhoto(formData);
        await this.fetchAndSetPhotos();
        eventBus.emit('photoUploaded');
    }
    async fetchAndSetPhotos() {
        const newPhotos = await getPhotos();
        const existingPhotoIds = new Set(this.photos.map(photo => photo.photo_id));
        const addedPhotos = newPhotos.filter(photo => !existingPhotoIds.has(photo.photo_id));
        // Stelle sicher, dass jedes Foto ein `tag_ids`-Feld hat
        addedPhotos.forEach(photo => {
            if (!photo.tag_ids) {
                photo.tag_ids = [];
            }
        });
        this.photos.push(...addedPhotos);
        if (addedPhotos.length > 0) {
            eventBus.emit('newPhotosAdded');
        }
    }
    async downloadPhoto(photoId) {
        const photo = this.photos.find(p => p.photo_id === photoId);
        if (photo && !photo.imageData) {
            photo.imageData = await downloadPhoto(photoId);
        }
        return photo ? photo.imageData : '';
    }
    /*
      Album methoden
     */
    async createAlbum(title) {
        await createAlbum({ title });
        await this.loadAlbums();
        eventBus.emit('albumCreated');
    }
    async loadAlbums() {
        const newAlbums = await getAlbums();
        const existingAlbumIds = new Set(this.albums.map(album => album.album_id));
        const addedAlbums = newAlbums.filter(album => !existingAlbumIds.has(album.album_id));
        // Initialisiere `photo_ids` und `tag_ids` für jedes neue Album, falls sie nicht vorhanden sind
        addedAlbums.forEach(album => {
            if (!album.photo_ids) {
                album.photo_ids = [];
            }
            if (!album.tag_ids) {
                album.tag_ids = [];
            }
        });
        this.albums.push(...addedAlbums);
        if (addedAlbums.length > 0) {
            eventBus.emit('albumsUpdated', addedAlbums);
        }
    }
    async updateAlbum(albumId, title) {
        await updateAlbum(albumId, { title });
        const album = this.getAlbumById(albumId);
        if (album) {
            album.title = title;
        }
        eventBus.emit('albumUpdated', albumId);
    }
    async deleteAlbum(albumId) {
        await deleteAlbum(albumId);
        this.albums = this.albums.filter(album => album.album_id !== albumId);
        eventBus.emit('albumDeleted', albumId);
    }
    async initialize(userId) {
        try {
            const [currentUser, photos, albums, tags, photoTags, albumTags] = await Promise.all([
                getUserById(userId),
                getPhotos(),
                getAlbums(),
                getTags(),
                getPhotoTags(),
                getAlbumTags()
            ]);
            this.setCurrentUser(currentUser);
            this.setPhotos(photos);
            this.setAlbums(albums);
            this.setTags(tags);
            for (const album of this.albums) {
                await this.setPhotosByAlbum(album.album_id);
            }
            // Match tags to photos and albums
            this.photos.forEach(photo => {
                photo.tag_ids = photoTags
                    .filter(pt => pt.photo_id === photo.photo_id)
                    .map(pt => pt.tag_id);
            });
            this.albums.forEach(album => {
                album.photo_ids = album.photo_ids || []; // Initialize photo_ids if not already set
                album.tag_ids = albumTags
                    .filter(at => at.album_id === album.album_id)
                    .map(at => at.tag_id);
            });
            console.log('Store initialized');
        }
        catch (error) {
            console.error('Error initializing store:', error);
        }
    }
    /*
      User
     */
    async fetchAndSetUsers() {
        this.users = await getUsers();
    }
    async createUser(username, password) {
        const newUser = await createUser({ username, password });
        this.users.push(newUser);
    }
    async deleteUser(userId) {
        await deleteUser(userId);
        this.users = this.users.filter(user => user.user_id !== userId);
    }
    async updateUser(userId, updates) {
        await updateUser(userId, updates);
        const user = this.users.find(user => user.user_id === userId);
        if (user) {
            if (updates.username)
                user.username = updates.username;
        }
    }
    async initializeAdmin(userId) {
        try {
            const [currentUser, users] = await Promise.all([
                getUserById(userId),
                getUsers(),
            ]);
            this.setCurrentUser(currentUser);
            this.setUsers(users);
            console.log('Store (admin) initialized');
        }
        catch (error) {
            console.error('Error initializing store:', error);
        }
    }
}
export const store = new Store();
