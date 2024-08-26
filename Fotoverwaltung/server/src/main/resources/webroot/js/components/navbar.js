import { logout } from '../api.js';
import { store } from "../store.js";
import { eventBus } from "../utils/eventBus.js";
export async function initializeNavbar() {
    const currentUser = store.getCurrentUser();
    const username = currentUser.username;
    const isAdmin = currentUser.role === 'admin';
    const navbarUser = document.getElementById('username');
    navbarUser.textContent = username + " ▼";
    const userDropdownContent = document.querySelector('.user-dropdown-content');
    if (isAdmin) {
        const adminLink = document.createElement('a');
        adminLink.href = 'admin.html';
        adminLink.textContent = 'Admin Menü';
        userDropdownContent.insertBefore(adminLink, userDropdownContent.firstChild);
    }
    const logoutButton = document.getElementById('logout');
    if (logoutButton) {
        logoutButton.addEventListener('click', async () => {
            try {
                await logout();
                localStorage.removeItem('user');
                window.location.href = 'login.html';
            }
            catch (error) {
                console.error('Error during logout:', error);
            }
        });
    }
    // Search field
    const searchInput = document.getElementById('searchInputPhoto');
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
    const uploadPhotoLink = document.getElementById('uploadPhoto');
    uploadPhotoLink.addEventListener('click', () => {
        //addDropdown.style.display = 'none'; // Close the dropdown
        eventBus.emit('openUploadPhotoModal');
    });
    const createAlbumLink = document.getElementById('createAlbum');
    createAlbumLink.addEventListener('click', () => {
        //addDropdown.style.display = 'none'; // Close the dropdown
        eventBus.emit('openCreateAlbumModal');
    });
}
