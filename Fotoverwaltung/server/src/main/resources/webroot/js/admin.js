import { store } from './store.js';
import { logout } from "./api.js";
document.addEventListener('DOMContentLoaded', async () => {
    // Weiterleitung zu /login, falls Nutzer nicht angemeldet ist.
    const userId = localStorage.getItem('userId');
    if (!userId) {
        window.location.href = 'login.html';
        return;
    }
    try {
        await store.initializeAdmin(Number.parseInt(userId));
        const currentUser = store.getCurrentUser();
        if (!currentUser) {
            window.location.href = 'login.html';
            return;
        }
        // Navbar
        const username = currentUser.username;
        const navbarUser = document.getElementById('username');
        navbarUser.textContent = `${username} ▼`;
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
    }
    catch (error) {
        console.error('Error during initialization:', error);
        window.location.href = 'login.html';
    }
    displayUsers(store.getUsers());
    const searchUserInput = document.getElementById('searchUserInput');
    const addUserButton = document.getElementById('addUserButton');
    const closeUserModal = document.getElementById('closeUserModal');
    const saveUserButton = document.getElementById('saveUserButton');
    searchUserInput.addEventListener('input', () => {
        const query = searchUserInput.value.toLowerCase();
        const filteredUsers = store.getUsers().filter(user => user.username.toLowerCase().includes(query));
        displayUsers(filteredUsers);
    });
    addUserButton.addEventListener('click', () => {
        openUserModal(); // Öffnet das Modal für einen neuen Benutzer
    });
    closeUserModal.addEventListener('click', closeUserModalHandler);
    saveUserButton.addEventListener('click', async () => {
        const userId = document.getElementById('userIdInput').value;
        const username = document.getElementById('usernameInput').value;
        const password = document.getElementById('passwordInput').value;
        if (username || password) {
            if (userId) {
                await store.updateUser(parseInt(userId), { username, password });
                // Aktualisiere den Benutzernamen in der Navbar, wenn der eigene Name geändert wurde
                const currentUser = store.getCurrentUser();
                if (currentUser && currentUser.user_id === parseInt(userId)) {
                    const navbarUser = document.getElementById('username');
                    navbarUser.textContent = `${username} ▼`;
                }
            }
            else {
                await store.createUser(username, password);
            }
            closeUserModalHandler();
            await store.fetchAndSetUsers();
            displayUsers(store.getUsers());
        }
        else {
            alert('Bitte geben Sie einen Nutzernamen oder ein Passwort ein.');
        }
    });
    document.getElementById('userList')?.addEventListener('click', async (event) => {
        const target = event.target;
        if (target.classList.contains('delete-user')) {
            const userId = parseInt(target.dataset.userId);
            if (confirm('Möchten Sie diesen Nutzer wirklich löschen?')) {
                await store.deleteUser(userId);
                displayUsers(store.getUsers());
            }
        }
        if (target.classList.contains('reset-password')) {
            const userId = parseInt(target.dataset.userId);
            const user = store.getUserById(userId);
            openUserModal(user); // Modal öffnen und mit Benutzerdaten füllen
        }
    });
});
function displayUsers(users) {
    const userList = document.getElementById('userList');
    userList.innerHTML = '';
    users.forEach(user => {
        const li = document.createElement('li');
        li.innerHTML = `
      <span>
        <span class="username">${user.username}</span>
        <span class="role">${user.role}</span>
      </span>
      <div>
        <button class="reset-password" data-user-id="${user.user_id}">Bearbeiten</button>
        <button class="delete-user" data-user-id="${user.user_id}">Löschen</button>
      </div>
    `;
        if (user.role === 'admin') {
            li.querySelector('.delete-user')?.remove(); // Admin kann nicht gelöscht werden
        }
        userList.appendChild(li);
    });
}
function openUserModal(user = null) {
    const userModal = document.getElementById('userModal');
    const userModalTitle = document.getElementById('userModalTitle');
    const userIdInput = document.getElementById('userIdInput');
    const usernameInput = document.getElementById('usernameInput');
    const passwordInput = document.getElementById('passwordInput');
    const roleSelect = document.getElementById('roleSelect')?.parentElement;
    if (user) {
        userModalTitle.textContent = 'Nutzer bearbeiten';
        userIdInput.value = user.user_id.toString();
        usernameInput.value = user.username;
        passwordInput.value = ''; // Passwort leer lassen für die Eingabe eines neuen Passworts
        roleSelect?.classList.add('hidden'); // Rolle ausblenden, da nur ein Admin existiert
    }
    else {
        userModalTitle.textContent = 'Nutzer erstellen';
        userIdInput.value = '';
        usernameInput.value = '';
        passwordInput.value = '';
        roleSelect?.classList.add('hidden');
    }
    userModal.classList.remove('hidden');
}
function closeUserModalHandler() {
    const userModal = document.getElementById('userModal');
    userModal.classList.add('hidden');
}
