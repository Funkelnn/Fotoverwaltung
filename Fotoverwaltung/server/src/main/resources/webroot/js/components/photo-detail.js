import { eventBus } from '../utils/eventBus.js';
import { store } from '../store.js';
export async function initializePhotoDetail() {
    eventBus.on('photoSelected', (photo) => {
        displayPhotoDetail(photo);
    });
}
async function displayPhotoDetail(photo) {
    const photoDetail = document.getElementById('photoDetail');
    const albumListItems = store.getAlbums()
        .filter(album => album.photo_ids.includes(photo.photo_id))
        .map(album => {
        return `<li class="album-item">${album.title} <button class="remove-album" data-album-id="${album.album_id}">×</button></li>`;
    }).join('');
    const tagListItems = photo.tag_ids
        .map(tagId => {
        const tag = store.getTags().find(tag => tag.tag_id === tagId);
        return `<li>${tag?.name} <button class="remove-tag" data-tag-id="${tagId}">×</button></li>`;
    }).join('');
    const availableAlbums = store.getAlbums().filter(album => !album.photo_ids.includes(photo.photo_id));
    photoDetail.innerHTML = `
        <div class="photo-detail-content">
            <button class="close-button" id="closeDetail">×</button>
            <div class="image-container">
                <img src="${photo.imageData}" alt="${photo.title}">
            </div>
            <div class="photo-info">
                <div class="info" id="details">
                  <h2>${photo.title}</h2>
                  <p>Datum: ${photo.capture_date}</p>
                  <p>Zeit: ${photo.capture_time ? photo.capture_time : '-'}</p>
                </div>

                <div id="detailsEdit" class="hidden">
                    <input type="text" id="newTitle" value="${photo.title}">
                    <input type="date" id="newDate" value="${photo.capture_date}">
                    <input type="time" id="newTime" value="${photo.capture_time ? photo.capture_time : ''}">
                    <button id="deletePhoto">Bild löschen</button>
                    <button id="saveDetails">Änderungen Speichern</button>
                </div>

                <button class="edit-button" id="editDetails">Bearbeiten</button>

                <div id="deleteConfirmation" class="delete-confirmation hidden">
                    <p>Möchten Sie das Bild wirklich löschen?</p>
                    <button id="confirmDelete">Ja</button>
                    <button id="cancelDelete">Nein</button>
                </div>

                <div id="detailsTags">
                    <h3>Tags</h3>
                    <ul id="tagList">${tagListItems}</ul>
                    <div class="tag-input-container">
                        <input type="text" id="newTagInput" placeholder="Neues Tag">
                        <button id="addTag">Tag hinzufügen</button>
                    </div>
                    <div id="tagSuggestions" class="hidden"></div>
                </div>
                <div id="detailsAlbums">
                    <h3>Foto in Alben</h3>
                    <ul id="albumList">${albumListItems}</ul>
                    <div class="album-select-container">
                      <select id="albumDropdown">
                          <option value="" disabled selected>Zum Album hinzufügen</option>
                          ${availableAlbums.map(album => `<option value="${album.album_id}">${album.title}</option>`).join('')}
                      </select>
                      <button id="addAlbum">Zum Album hinzufügen</button>
                    </div>
                </div>
            </div>
        </div>
    `;
    photoDetail.classList.remove('hidden');
    // Close window
    const closeButton = document.getElementById('closeDetail');
    closeButton.addEventListener('click', () => {
        photoDetail.classList.add('hidden');
    });
    photoDetail.addEventListener('click', (event) => {
        if (event.target === photoDetail) {
            photoDetail.classList.add('hidden');
        }
    });
    // Edit Details
    const editDetailsButton = document.getElementById('editDetails');
    const detailsEditDiv = document.getElementById('detailsEdit');
    const deleteConfirmationDiv = document.getElementById('deleteConfirmation');
    const detailsDiv = document.getElementById('details');
    let toggle = true;
    editDetailsButton.addEventListener('click', () => {
        detailsEditDiv.classList.toggle('hidden');
        detailsDiv.classList.toggle('hidden');
        if (toggle) {
            editDetailsButton.textContent = 'Abbrechen';
        }
        else {
            editDetailsButton.textContent = 'Bearbeiten';
        }
        toggle = !toggle;
    });
    const saveDetailsButton = document.getElementById('saveDetails');
    saveDetailsButton.addEventListener('click', async () => {
        const newTitle = document.getElementById('newTitle').value;
        const newDate = document.getElementById('newDate').value;
        const newTime = document.getElementById('newTime').value;
        const updates = {};
        if (newTitle !== photo.title)
            updates.title = newTitle;
        if (newDate !== photo.capture_date)
            updates.capture_date = newDate;
        if (newTime !== photo.capture_time)
            updates.capture_time = newTime;
        if (Object.keys(updates).length > 0) {
            await store.updatePhoto(photo.photo_id, updates); // Update in the store and database
            const updatedPhoto = { ...photo, ...updates }; // Aktualisiertes Foto
            displayPhotoDetail(updatedPhoto); // Neu anzeigen
            detailsEditDiv.classList.add('hidden'); // Bearbeiten menü schließen
        }
    });
    // Bild löschen
    const deletePhotoButton = document.getElementById('deletePhoto');
    deletePhotoButton.addEventListener('click', () => {
        deleteConfirmationDiv.classList.remove('hidden');
    });
    const confirmDeleteButton = document.getElementById('confirmDelete');
    confirmDeleteButton.addEventListener('click', async () => {
        await store.deletePhoto(photo.photo_id);
        photoDetail.classList.add('hidden');
    });
    const cancelDeleteButton = document.getElementById('cancelDelete');
    cancelDeleteButton.addEventListener('click', () => {
        deleteConfirmationDiv.classList.add('hidden');
    });
    // Tags
    const addTagButton = document.getElementById('addTag');
    const newTagInput = document.getElementById('newTagInput');
    const tagSuggestions = document.getElementById('tagSuggestions');
    newTagInput.addEventListener('input', () => {
        const query = newTagInput.value.toLowerCase();
        const suggestions = store.getTags().filter(tag => tag.name.toLowerCase().includes(query) && !photo.tag_ids.includes(tag.tag_id));
        if (suggestions.length > 0 && query !== '') {
            tagSuggestions.innerHTML = suggestions.map(tag => `
                <div class="suggestion-item" data-tag-id="${tag.tag_id}">
                    ${tag.name}
                    <button class="delete-tag" data-tag-id="${tag.tag_id}">×</button>
                </div>
            `).join('');
            tagSuggestions.classList.remove('hidden');
        }
        else {
            tagSuggestions.classList.add('hidden');
        }
    });
    tagSuggestions.addEventListener('click', async (event) => {
        const target = event.target;
        if (target.classList.contains('suggestion-item')) {
            newTagInput.value = target.childNodes[0].textContent.trim();
            tagSuggestions.classList.add('hidden');
        }
        else if (target.classList.contains('delete-tag')) {
            const tagId = parseInt(target.dataset.tagId);
            await store.deleteTag(tagId);
            displayPhotoDetail(photo); // Refresh the detail view
        }
    });
    addTagButton.addEventListener('click', async () => {
        const newTagName = newTagInput.value.trim();
        if (newTagName) {
            let tag = store.getTags().find(tag => tag.name === newTagName);
            if (!tag) {
                tag = await store.createTag(newTagName);
            }
            await store.addTagToPhoto(photo.photo_id, tag.tag_id);
            displayPhotoDetail(photo); // Refresh the detail view
        }
    });
    const removeTagButtons = photoDetail.querySelectorAll('.remove-tag');
    removeTagButtons.forEach(button => {
        button.addEventListener('click', async () => {
            const tagId = parseInt(button.dataset.tagId);
            await store.removeTagFromPhoto(photo.photo_id, tagId);
            displayPhotoDetail(photo); // Refresh the detail view
        });
    });
    // Album Management
    const addAlbumButton = document.getElementById('addAlbum');
    const albumDropdown = document.getElementById('albumDropdown');
    addAlbumButton.addEventListener('click', async () => {
        const albumId = parseInt(albumDropdown.value);
        if (albumId) {
            await store.addPhotoToAlbum(photo.photo_id, albumId);
            displayPhotoDetail(photo); // Refresh the detail view
        }
    });
    const removeAlbumButtons = photoDetail.querySelectorAll('.remove-album');
    removeAlbumButtons.forEach(button => {
        button.addEventListener('click', async () => {
            const albumId = parseInt(button.dataset.albumId);
            await store.removePhotoFromAlbum(photo.photo_id, albumId);
            displayPhotoDetail(photo); // Refresh the detail view
        });
    });
}
