import {
  getUsers,
  getPhotos,
  getAlbums,
  getTags,
  getUserById,
  getPhotosByAlbum,
  downloadPhoto,
  getPhotoTags,
  getAlbumTags,
  addTagToPhoto,
  removeTagFromPhoto,
  updatePhoto,
  deletePhoto,
  createTag,
  deleteTag,
  addPhotoToAlbum,
  removePhotoFromAlbum,
  uploadPhoto,
  createAlbum,
  addTagToAlbum,
  removeTagFromAlbum,
  updateAlbum,
  deleteAlbum,
  deleteUser,
  createUser,
  updateUser
} from './api.js';
import {eventBus} from "./utils/eventBus.js";

export interface User {
  user_id: number;
  username: string;
  role: 'user' | 'admin';
  created_at: string;
}

export interface Tag {
  tag_id: number;
  user_id: number;
  name: string;
  created_at: string;
}

export interface Photo {
  photo_id: number;
  user_id: number;
  filepath: string;
  title: string;
  capture_date: string;
  capture_time: string | null;
  latitude: number | null;
  longitude: number | null;
  created_at: string;
  updated_at: string;
  tag_ids: number[];
  imageData?: string;
}

export interface Album {
  album_id: number;
  user_id: number;
  title: string;
  created_at: string;
  updated_at: string;
  photo_ids: number[];
  tag_ids: number[];
}

class Store {
  private users: User[] = [];
  private photos: Photo[] = [];
  private albums: Album[] = [];
  private tags: Tag[] = [];
  private currentUser: User | null = null;

  public setUsers(users: User[]) {
    this.users = users;
  }

  public getUsers(): User[] {
    return this.users;
  }

  public getUserById(userId: number): User | undefined {
    return this.users.find(user => user.user_id === userId);
  }

  public setCurrentUser(user: User) {
    this.currentUser = user;
  }

  public getCurrentUser(): User | null {
    return this.currentUser;
  }

  public setPhotos(photos: Photo[]) {
    this.photos = photos;
  }

  public getPhotos(): Photo[] {
    return this.photos;
  }

  public setAlbums(albums: Album[]) {
    this.albums = albums;
  }

  public getAlbums(): Album[] {
    return this.albums;
  }

  public getAlbumById(albumId: number): Album | undefined {
    return this.albums.find(album => album.album_id === albumId);
  }

  public setTags(tags: Tag[]) {
    this.tags = tags;
  }

  public getTags(): Tag[] {
    return this.tags;
  }

  public getTagById(tagId: number): Tag | undefined {
    return this.tags.find(tag => tag.tag_id === tagId);
  }

  /*
    Fotos in Alben
   */
  public async setPhotosByAlbum(albumId: number) {
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

  public async addPhotoToAlbum(photoId: number, albumId: number) {
    await addPhotoToAlbum(photoId, albumId);
    const album = this.albums.find(a => a.album_id === albumId);
    if (album && !album.photo_ids.includes(photoId)) {
      album.photo_ids.push(photoId);
    }
  }

  public async removePhotoFromAlbum(photoId: number, albumId: number): Promise<void> {
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
  public async createTag(name: string): Promise<Tag> {
    await createTag(name);
    await this.loadTags(); // Alle Tags neu laden
    const newTag = this.tags.find(tag => tag.name === name);
    if (!newTag) {
      throw new Error('Tag creation failed.');
    }
    return newTag;
  }

  private async loadTags() {
    this.tags = await getTags();
  }

  public async deleteTag(tagId: number): Promise<void> {
    await deleteTag(tagId);
    this.tags = this.tags.filter(tag => tag.tag_id !== tagId);
    this.photos.forEach(photo => {
      photo.tag_ids = photo.tag_ids.filter(id => id !== tagId);
    });
    this.albums.forEach(album => {
      album.tag_ids = album.tag_ids.filter(id => id !== tagId);
    });
  }

  public async addTagToPhoto(photoId: number, tagId: number) {
    await addTagToPhoto(photoId, tagId);
    const photo = this.photos.find(p => p.photo_id === photoId);
    if (photo && !photo.tag_ids.includes(tagId)) {
      photo.tag_ids.push(tagId);
    }
  }

  public async removeTagFromPhoto(photoId: number, tagId: number) {
    await removeTagFromPhoto(photoId, tagId);
    const photo = this.photos.find(p => p.photo_id === photoId);
    if (photo) {
      photo.tag_ids = photo.tag_ids.filter(id => id !== tagId);
    }
  }

  public async addTagToAlbum(albumId: number, tagId: number): Promise<void> {
    await addTagToAlbum(albumId, tagId);

    const album = this.getAlbumById(albumId);
    if (album) {
      if (!album.tag_ids.includes(tagId)) {
        album.tag_ids.push(tagId);
      }
      eventBus.emit('albumUpdated', albumId); // Event auslösen, wenn das Album aktualisiert wurde
    }
  }

  public async removeTagFromAlbum(albumId: number, tagId: number): Promise<void> {
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

  public async updatePhoto(photoId: number, updates: Partial<Photo>) {
    const photo = this.photos.find(p => p.photo_id === photoId);
    if (photo) {
      Object.assign(photo, updates); // Lokale Kopie aktualisieren
      await updatePhoto(photoId, updates); // API-Aufruf zum Aktualisieren des Fotos in der Datenbank
      eventBus.emit('photoUpdated', photo); // Event für die Aktualisierung des Grids
    }
  }

  public async deletePhoto(photoId: number) {
    await deletePhoto(photoId); // API-Aufruf zum Löschen des Fotos in der Datenbank
    this.photos = this.photos.filter(photo => photo.photo_id !== photoId); // Lokale Kopie aktualisieren
    eventBus.emit('photoDeleted', photoId); // Event für die Aktualisierung des Grids
  }

  public async uploadPhoto(formData: FormData): Promise<void> {
    await uploadPhoto(formData);
    await this.fetchAndSetPhotos();
    eventBus.emit('photoUploaded');
  }

  public async fetchAndSetPhotos(): Promise<void> {
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

  public async downloadPhoto(photoId: number): Promise<string> {
    const photo = this.photos.find(p => p.photo_id === photoId);
    if (photo && !photo.imageData) {
      photo.imageData = await downloadPhoto(photoId);
    }
    return photo ? photo.imageData! : '';
  }

  /*
    Album methoden
   */
  public async createAlbum(title: string): Promise<void> {
    await createAlbum({ title });
    await this.loadAlbums();
    eventBus.emit('albumCreated');
  }

  private async loadAlbums(): Promise<void> {
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

  public async updateAlbum(albumId: number, title: string): Promise<void> {
    await updateAlbum(albumId, { title });
    const album = this.getAlbumById(albumId);
    if (album) {
      album.title = title;
    }
    eventBus.emit('albumUpdated', albumId);
  }

  public async deleteAlbum(albumId: number): Promise<void> {
    await deleteAlbum(albumId);
    this.albums = this.albums.filter(album => album.album_id !== albumId);
    eventBus.emit('albumDeleted', albumId);
  }

  public async initialize(userId: number) {
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
    } catch (error) {
      console.error('Error initializing store:', error);
    }
  }


  /*
    User
   */
  public async fetchAndSetUsers(): Promise<void> {
    this.users = await getUsers();
  }

  public async createUser(username: string, password: string): Promise<void> {
    const newUser = await createUser({ username, password });
    this.users.push(newUser);
  }

  public async deleteUser(userId: number): Promise<void> {
    await deleteUser(userId);
    this.users = this.users.filter(user => user.user_id !== userId);
  }

  public async updateUser(userId: number, updates: { username?: string, password?: string }): Promise<void> {
    await updateUser(userId, updates);
    const user = this.users.find(user => user.user_id === userId);
    if (user) {
      if (updates.username) user.username = updates.username;
    }
  }

  public async initializeAdmin(userId: number) {
    try {
      const [currentUser, users] = await Promise.all([
        getUserById(userId),
        getUsers(),
      ]);
      this.setCurrentUser(currentUser);
      this.setUsers(users);

      console.log('Store (admin) initialized');
    } catch (error) {
      console.error('Error initializing store:', error);
    }
  }
}

export const store = new Store();
