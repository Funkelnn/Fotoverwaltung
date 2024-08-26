import {store} from "./store.js";
import { initializeSidebar } from './components/sidebar.js';
import { initializeNavbar } from './components/navbar.js';
import { initializePhotoGrid } from './components/photo-grid.js';
import { initializePhotoDetail } from "./components/photo-detail.js";
import {initializeUploadPhotoModal} from "./components/uploadPhotoModal.js";
import {initializeCreateAlbumModal} from "./components/createAlbumModal.js";
import {initializeEditAlbumModal} from "./components/editAlbumModal.js";

document.addEventListener('DOMContentLoaded', async () => {
  // Weiterleitung zu /login, falls Nutzer nicht angemeldet ist.
  const userId = localStorage.getItem('userId');
  if (!userId) {
    window.location.href = 'login.html';
    return;
  }

  try {
    await store.initialize(Number.parseInt(userId));
    await initializeNavbar();
    await initializeSidebar();
    await initializePhotoGrid();
    await initializePhotoDetail();
    initializeUploadPhotoModal();
    initializeCreateAlbumModal();
    initializeEditAlbumModal();

    const user = store.getCurrentUser();
    if (!user) {
      window.location.href = 'login.html';
      return;
    }
  } catch (error) {
    console.error('Error during initialization:', error);
    window.location.href = 'login.html';
  }
});
