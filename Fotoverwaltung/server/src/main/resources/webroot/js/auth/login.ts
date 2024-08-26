import { login } from '../api.js';

document.addEventListener('DOMContentLoaded', () => {
  const loginForm = document.getElementById('loginForm') as HTMLFormElement;
  const errorElement = document.getElementById('login-error') as HTMLElement;
  const passwordInput = document.getElementById('password') as HTMLInputElement;
  const togglePasswordButton = document.getElementById('togglePassword') as HTMLButtonElement;

  loginForm.addEventListener('submit', async (event) => {
    event.preventDefault();

    const username = (document.getElementById('username') as HTMLInputElement).value;
    const password = passwordInput.value;

    try {
      errorElement.textContent = '';
      const userData = await login(username, password);
      localStorage.setItem('userId', JSON.stringify(userData.user_id));
      window.location.href = 'index.html';
    } catch (error) {
      if (error instanceof Error) {
        if (error.message === 'Invalid username or password') {
          errorElement.textContent = 'Ungültiger Benutzername oder Passwort!';
        } else {
          errorElement.textContent = error.message;
        }
      } else {
        errorElement.textContent = 'Ein unbekannter Fehler ist aufgetreten';
      }
    }
  });

  togglePasswordButton.addEventListener('click', () => {
    const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
    passwordInput.setAttribute('type', type);
    togglePasswordButton.textContent = type === 'password' ? '👁️' : '🙈';
  });
});
