import { eventBus } from '../utils/eventBus.js';
import { store } from '../store.js';
export function initializeCreateAlbumModal() {
    const modalContainer = document.getElementById('create-album-modal');
    const modalCloseButton = document.getElementById('modal-close-create-album');
    const modalSaveButton = document.getElementById('modal-save-create-album');
    const albumTitleInput = document.getElementById('album-title-input');
    // Event listener for opening the modal
    eventBus.on('openCreateAlbumModal', () => {
        modalContainer.classList.remove('hidden');
    });
    // Close modal event
    modalCloseButton.addEventListener('click', closeModal);
    // Close modal when clicking outside of it
    window.addEventListener('click', (event) => {
        if (event.target === modalContainer) {
            closeModal();
        }
    });
    modalSaveButton.addEventListener('click', async () => {
        const title = albumTitleInput.value.trim();
        if (!title) {
            alert('Bitte geben Sie einen Titel für das Album ein.');
            return;
        }
        try {
            await store.createAlbum(title);
            alert('Album erfolgreich erstellt!');
            closeModal();
        }
        catch (error) {
            console.error('Fehler beim Erstellen des Albums:', error);
            alert('Fehler beim Erstellen des Albums.');
        }
    });
    function closeModal() {
        modalContainer.classList.add('hidden');
        clearForm();
    }
    function clearForm() {
        albumTitleInput.value = '';
    }
}
