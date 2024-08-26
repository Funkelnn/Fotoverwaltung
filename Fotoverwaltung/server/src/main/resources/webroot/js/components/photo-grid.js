import { store } from '../store.js';
import { eventBus } from '../utils/eventBus.js';
export async function initializePhotoGrid() {
    let selectedAlbum = null;
    eventBus.on('albumSelected', async (albumId) => {
        if (albumId) {
            displayPhotos(albumId);
            selectedAlbum = albumId;
        }
        else {
            displayAllPhotos();
            selectedAlbum = null;
        }
    });
    eventBus.on('photoUpdated', (updatedPhoto) => {
        updatePhotoInGrid(updatedPhoto);
    });
    eventBus.on('photoDeleted', (deletedPhotoId) => {
        removePhotoFromGrid(deletedPhotoId);
    });
    eventBus.on('albumUpdated', () => {
        if (selectedAlbum != null) {
            displayPhotos(selectedAlbum);
        }
        else {
            displayAllPhotos();
        }
    });
    eventBus.on('newPhotosAdded', () => {
        if (selectedAlbum == null) {
            displayAllPhotos();
            selectedAlbum = null;
        }
    });
    eventBus.on('albumDeleted', (albumId) => {
        if (selectedAlbum === albumId.toString()) {
            displayAllPhotos();
        }
    });
    eventBus.on('search', (query) => {
        // Hole die aktuellen Fotos, die angezeigt werden sollen (Alle Fotos oder ein bestimmtes Album)
        let photos = store.getPhotos();
        // Falls ein Album ausgewählt ist, filtere die Fotos nach diesem Album
        if (selectedAlbum != null) {
            const currentAlbumId = Number.parseInt(selectedAlbum);
            if (currentAlbumId) {
                const currentAlbum = store.getAlbumById(currentAlbumId);
                if (currentAlbum) {
                    photos = photos.filter(photo => currentAlbum.photo_ids.includes(photo.photo_id));
                }
            }
        }
        // Filtern der Fotos nach Titel oder Tags
        const filteredPhotos = photos.filter(photo => photo.title.toLowerCase().includes(query.toLowerCase()) ||
            photo.tag_ids.some(tagId => {
                const tag = store.getTagById(tagId);
                return tag && tag.name.toLowerCase().includes(query.toLowerCase());
            }));
        // Aktualisiere das Grid mit den gefilterten Fotos
        populatePhotoGrid(filteredPhotos);
    });
    displayAllPhotos();
}
function displayAllPhotos() {
    const photos = store.getPhotos();
    populatePhotoGrid(photos);
}
function displayPhotos(albumId) {
    const album = store.getAlbums().find(album => album.album_id === parseInt(albumId));
    if (album) {
        const photos = album.photo_ids.map(photoId => store.getPhotos().find(photo => photo.photo_id === photoId));
        populatePhotoGrid(photos);
    }
}
async function populatePhotoGrid(photos) {
    const photoGrid = document.getElementById('photoGrid');
    photoGrid.innerHTML = ''; // Clear the grid before populating
    for (const photo of photos) {
        if (!photo.imageData) {
            // Falls das Bild noch nicht heruntergeladen wurde, lade es herunter
            photo.imageData = await store.downloadPhoto(photo.photo_id);
        }
        const photoElement = document.createElement('div');
        photoElement.classList.add('photo-item');
        photoElement.dataset.photoId = photo.photo_id.toString();
        photoElement.innerHTML = `
            <div class="photo-wrapper">
                <img src="${photo.imageData}" alt="${photo.title}">
                <div class="photo-title">${photo.title}</div>
            </div>
        `;
        photoElement.addEventListener('click', () => {
            eventBus.emit('photoSelected', photo);
        });
        photoGrid.appendChild(photoElement);
    }
}
function updatePhotoInGrid(updatedPhoto) {
    const photoElement = document.querySelector(`[data-photo-id="${updatedPhoto.photo_id}"]`);
    if (photoElement) {
        const titleElement = photoElement.querySelector('.photo-title');
        if (titleElement) {
            titleElement.textContent = updatedPhoto.title;
        }
    }
}
function removePhotoFromGrid(deletedPhotoId) {
    const photoElement = document.querySelector(`[data-photo-id="${deletedPhotoId}"]`);
    if (photoElement) {
        photoElement.remove();
    }
}
