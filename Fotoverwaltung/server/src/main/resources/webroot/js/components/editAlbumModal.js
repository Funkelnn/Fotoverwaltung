import { eventBus } from '../utils/eventBus.js';
import { store } from '../store.js';
export function initializeEditAlbumModal() {
    const modalContainer = document.getElementById('edit-album-modal');
    const modalCloseButton = document.getElementById('modal-close-edit-album');
    const modalSaveButton = document.getElementById('modal-save-edit-album');
    const albumTitleInput = document.getElementById('album-title-edit-input');
    const albumTagsList = document.getElementById('album-tags-list');
    const albumTagsInput = document.getElementById('album-tags-input');
    const addAlbumTagButton = document.getElementById('add-album-tag-button');
    const deleteAlbumButton = document.getElementById('delete-album-button');
    const tagSuggestionsContainer = document.getElementById('tag-suggestions');
    let currentAlbum;
    eventBus.on('editAlbum', async (albumId) => {
        currentAlbum = store.getAlbumById(albumId);
        if (!currentAlbum)
            return;
        // Fülle die Felder mit den aktuellen Werten
        albumTitleInput.value = currentAlbum.title;
        displayTags(currentAlbum.tag_ids);
        modalContainer.classList.remove('hidden');
    });
    // Close
    modalCloseButton.addEventListener('click', closeModal);
    window.addEventListener('click', (event) => {
        if (event.target === modalContainer) {
            closeModal();
        }
    });
    // Speichern event
    modalSaveButton.addEventListener('click', async () => {
        const updatedTitle = albumTitleInput.value.trim();
        if (!updatedTitle) {
            alert('Bitte geben Sie einen Titel für das Album ein.');
            return;
        }
        try {
            await store.updateAlbum(currentAlbum.album_id, updatedTitle);
            alert('Album erfolgreich aktualisiert!');
            closeModal();
        }
        catch (error) {
            console.error('Fehler beim Aktualisieren des Albums:', error);
            alert('Fehler beim Aktualisieren des Albums.');
        }
    });
    // Add Tag
    addAlbumTagButton.addEventListener('click', async () => {
        const tagName = albumTagsInput.value.trim();
        if (!tagName)
            return;
        let tag = store.getTags().find(tag => tag.name.toLowerCase() === tagName.toLowerCase());
        if (!tag) {
            tag = await store.createTag(tagName);
        }
        try {
            // Verhindere doppeltes Hinzufügen
            if (!currentAlbum.tag_ids.includes(tag.tag_id)) {
                await store.addTagToAlbum(currentAlbum.album_id, tag.tag_id);
                displayTags(currentAlbum.tag_ids);
                albumTagsInput.value = '';
                displayTagSuggestions('');
            }
        }
        catch (error) {
            console.error('Fehler beim Hinzufügen des Tags:', error);
        }
    });
    albumTagsInput.addEventListener('input', () => {
        const query = albumTagsInput.value.trim().toLowerCase();
        displayTagSuggestions(query);
    });
    tagSuggestionsContainer.addEventListener('click', async (event) => {
        const target = event.target;
        if (target.classList.contains('suggestion-item')) {
            albumTagsInput.value = target.childNodes[0].textContent.trim();
            tagSuggestionsContainer.classList.add('hidden');
        }
        else if (target.classList.contains('delete-tag')) {
            const tagId = parseInt(target.dataset.tagId);
            if (confirm('Möchten Sie diesen Tag wirklich löschen?')) {
                try {
                    await store.deleteTag(tagId);
                    displayTags(currentAlbum.tag_ids); // Aktualisiert die Tag-Liste
                    albumTagsInput.value = '';
                    displayTagSuggestions('');
                }
                catch (error) {
                    console.error('Fehler beim Löschen des Tags:', error);
                }
            }
        }
    });
    deleteAlbumButton.addEventListener('click', async () => {
        if (confirm('Möchten Sie dieses Album wirklich löschen?')) {
            try {
                await store.deleteAlbum(currentAlbum.album_id);
                alert('Album erfolgreich gelöscht!');
                closeModal();
            }
            catch (error) {
                console.error('Fehler beim Löschen des Albums:', error);
                alert('Fehler beim Löschen des Albums.');
            }
        }
    });
    function displayTags(tagIds) {
        albumTagsList.innerHTML = '';
        tagIds.forEach(tagId => {
            const tag = store.getTagById(tagId);
            if (tag) {
                const tagElement = document.createElement('span');
                tagElement.classList.add('tag');
                tagElement.textContent = tag.name;
                const removeButton = document.createElement('button');
                removeButton.textContent = 'x';
                removeButton.classList.add('remove-tag-button');
                removeButton.addEventListener('click', async () => {
                    try {
                        await store.removeTagFromAlbum(currentAlbum.album_id, tagId);
                        currentAlbum.tag_ids = currentAlbum.tag_ids.filter(id => id !== tagId);
                        displayTags(currentAlbum.tag_ids);
                    }
                    catch (error) {
                        console.error('Fehler beim Entfernen des Tags:', error);
                    }
                });
                tagElement.appendChild(removeButton);
                albumTagsList.appendChild(tagElement);
            }
        });
    }
    function displayTagSuggestions(query) {
        tagSuggestionsContainer.innerHTML = '';
        if (query) {
            // Filtere Tags, die bereits zum Album gehören, aus
            const suggestions = store.getTags().filter(tag => tag.name.toLowerCase().includes(query) && !currentAlbum.tag_ids.includes(tag.tag_id));
            suggestions.forEach(tag => {
                const suggestionElement = document.createElement('div');
                suggestionElement.textContent = `${tag.name}`;
                suggestionElement.classList.add('suggestion-item');
                suggestionElement.setAttribute('data-tag-id', tag.tag_id.toString());
                // Löschen Button für den Tag
                const deleteButton = document.createElement('button');
                deleteButton.textContent = 'x';
                deleteButton.classList.add('delete-tag');
                deleteButton.setAttribute('data-tag-id', tag.tag_id.toString());
                suggestionElement.appendChild(deleteButton);
                tagSuggestionsContainer.appendChild(suggestionElement);
            });
            tagSuggestionsContainer.classList.remove('hidden');
        }
        else {
            tagSuggestionsContainer.classList.add('hidden');
        }
    }
    function closeModal() {
        modalContainer.classList.add('hidden');
        clearForm();
    }
    function clearForm() {
        albumTitleInput.value = '';
        albumTagsList.innerHTML = '';
        albumTagsInput.value = '';
        tagSuggestionsContainer.innerHTML = '';
    }
}
