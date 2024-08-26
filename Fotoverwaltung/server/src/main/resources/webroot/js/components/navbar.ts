import { logout } from '../api.js';
import {store, User} from "../store.js";
import {eventBus} from "../utils/eventBus.js";

export async function initializeNavbar() {
  const currentUser : User = store.getCurrentUser()!;

  const username : string = currentUser.username;
  const isAdmin : boolean = currentUser.role === 'admin';

  const navbarUser = document.getElementById('username') as HTMLElement;
  navbarUser.textContent = username + " ▼";

  const userDropdownContent = document.querySelector('.user-dropdown-content') as HTMLElement;

  if (isAdmin) {
    const adminLink = document.createElement('a');
    adminLink.href = 'admin.html';
    adminLink.textContent = 'Admin Menü';
    userDropdownContent.insertBefore(adminLink, userDropdownContent.firstChild);
  }

  const logoutButton = document.getElementById('logout') as HTMLElement;
  if (logoutButton) {
    logoutButton.addEventListener('click', async () => {
      try {
        await logout();
        localStorage.removeItem('user');
        window.location.href = 'login.html';
      } catch (error) {
        console.error('Error during logout:', error);
      }
    });
  }

  // Search field
  const searchInput = document.getElementById('searchInputPhoto') as HTMLInputElement;
  searchInput.addEventListener('input', () => {
    const query = searchInput.value;
    eventBus.emit('search', query);
  });

  eventBus.on('albumSelected', () => {
    // Clear the search input field
    if (searchInput) {
      searchInput.value = '';
    }
  });

  const uploadPhotoLink = document.getElementById('uploadPhoto') as HTMLElement;
  uploadPhotoLink.addEventListener('click', () => {
    //addDropdown.style.display = 'none'; // Close the dropdown
    eventBus.emit('openUploadPhotoModal');
  });

  const createAlbumLink = document.getElementById('createAlbum') as HTMLElement;
  createAlbumLink.addEventListener('click', () => {
    //addDropdown.style.display = 'none'; // Close the dropdown
    eventBus.emit('openCreateAlbumModal');
  });

}
