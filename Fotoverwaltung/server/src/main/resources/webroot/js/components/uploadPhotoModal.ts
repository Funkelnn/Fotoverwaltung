import { eventBus } from '../utils/eventBus.js';
import { store } from '../store.js';

export function initializeUploadPhotoModal() {
  const modalContainer = document.getElementById('upload-photo-modal') as HTMLElement;
  const modalCloseButton = document.getElementById('modal-close-upload') as HTMLElement;
  const modalSaveButton = document.getElementById('modal-save-upload') as HTMLButtonElement;
  const fileInput = document.getElementById('file-input') as HTMLInputElement;
  const titleInput = document.getElementById('title-input') as HTMLInputElement;
  const dateInput = document.getElementById('date-input') as HTMLInputElement;
  const timeInput = document.getElementById('time-input') as HTMLInputElement;
  const uploadForm = document.getElementById('upload-photo-form') as HTMLFormElement;

  // Zulässige Dateitypen
  const validFileTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/heic', 'image/webp'];

  // Event listener for opening the modal
  eventBus.on('openUploadPhotoModal', () => {
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

  // File input change event to auto-fill title, date, and time
  fileInput.addEventListener('change', () => {
    if (fileInput.files && fileInput.files[0]) {
      const file = fileInput.files[0];

      // Überprüfen des Dateityps
      if (!validFileTypes.includes(file.type)) {
        alert('Ungültiger Dateityp. Zulässige Formate sind: jpg, jpeg, png, heic, webp.');
        fileInput.value = ''; // Leert das Feld, wenn der Dateityp ungültig ist
        return;
      }

      titleInput.value = file.name.split('.').slice(0, -1).join('.'); // Setzt den Titel als Dateinamen

      // Optionally extract date and time from file metadata (needs additional library)
    }
  });

  // Save button event to upload the photo
  modalSaveButton.addEventListener('click', async () => {
    // Validierung der Pflichtfelder
    if (!fileInput.files || fileInput.files.length === 0) {
      alert('Bitte wählen Sie eine Bilddatei aus.');
      return;
    }

    if (!titleInput.value.trim()) {
      alert('Bitte geben Sie einen Titel ein.');
      return;
    }

    if (!dateInput.value.trim()) {
      alert('Bitte geben Sie ein Aufnahmedatum ein.');
      return;
    }

    const captureDate = new Date(dateInput.value);
    const minDate = new Date('1900-01-01');
    const maxDate = new Date();

    if (captureDate < minDate || captureDate > maxDate) {
      alert('Das Aufnahmedatum kann nicht in der Zukunft liegen.');
      return;
    }

    const formData = new FormData(uploadForm);
    formData.append('file', fileInput.files[0]);

    const metadata: { title: string, capture_date: string, capture_time?: string } = {
      title: titleInput.value.trim(),
      capture_date: dateInput.value.trim(),
    };

    // Optional capture_time hinzufügen, wenn vorhanden
    if (timeInput.value) {
      const timeWithMillis = `${timeInput.value}:00`;
      metadata.capture_time = timeWithMillis;
    }

    formData.append('metadata', JSON.stringify(metadata));

    try {
      await store.uploadPhoto(formData);
      alert('Bild erfolgreich hochgeladen!');
      closeModal();
    } catch (error) {
      console.error('Fehler beim Hochladen des Bildes:', error);
      alert('Fehler beim Hochladen des Bildes.');
    }
  });

  function closeModal() {
    modalContainer.classList.add('hidden');
    clearForm();
  }

  function clearForm() {
    fileInput.value = '';
    titleInput.value = '';
    dateInput.value = '';
    timeInput.value = '';
  }
}
