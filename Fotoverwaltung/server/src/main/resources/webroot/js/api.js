export const apiBaseUrl = 'http://localhost:3000/api';
export async function login(username, password) {
    const response = await fetch(`${apiBaseUrl}/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password }),
        credentials: 'include',
    });
    if (response.ok) {
        return await response.json();
    }
    else {
        const errorData = await response.json();
        return Promise.reject(new Error(errorData.error || 'Login failed'));
    }
}
export async function logout() {
    const response = await fetch(`${apiBaseUrl}/logout`, {
        method: 'POST',
        credentials: 'include',
    });
    if (!response.ok) {
        throw new Error('Logout failed');
    }
}
/*
  Api Error
 */
class ApiError extends Error {
    status;
    constructor(message, status) {
        super(message);
        this.status = status;
    }
}
async function fetchJson(url, options = {}) {
    try {
        const response = await fetch(url, {
            ...options,
            credentials: 'include',
        });
        if (!response.ok) {
            throw new ApiError(`Failed to fetch ${url}: ${response.statusText} - ${await response.text()}`, response.status);
        }
        if (response.status === 204) {
            return null; // Leere Antwort bei Status 204
        }
        return response.json();
    }
    catch (error) {
        console.error(`Error fetching JSON from ${url}:`, error);
        throw new Error(error instanceof ApiError ? error.message : 'Unknown error');
    }
}
/*
  User
 */
export async function getUsers() {
    return fetchJson(`${apiBaseUrl}/users`);
}
export async function getUserById(userId) {
    return fetchJson(`${apiBaseUrl}/users/${userId}`);
}
export async function createUser(user) {
    return fetchJson(`${apiBaseUrl}/users`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(user),
    });
}
export async function deleteUser(userId) {
    return fetchJson(`${apiBaseUrl}/users/${userId}`, {
        method: 'DELETE',
    });
}
export async function updateUser(userId, updates) {
    return fetchJson(`${apiBaseUrl}/users/${userId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(updates),
    });
}
/*
  Photo
 */
export async function getPhotos() {
    return fetchJson(`${apiBaseUrl}/photos`);
}
export async function getPhotoById(photoId) {
    return fetchJson(`${apiBaseUrl}/photos/${photoId}`);
}
export async function getPhotosByAlbum(albumId) {
    return fetchJson(`${apiBaseUrl}/albums/${albumId}/photos`);
}
export async function deletePhoto(photoId) {
    await fetchJson(`${apiBaseUrl}/photos/${photoId}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
        },
    });
}
export async function updatePhoto(photoId, updates) {
    await fetchJson(`${apiBaseUrl}/photos/${photoId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(updates),
    });
}
export async function uploadPhoto(formData) {
    const response = await fetch(`${apiBaseUrl}/photos`, {
        method: 'POST',
        body: formData,
        credentials: 'include',
    });
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Fehler beim Hochladen des Bildes: ${response.statusText} - ${errorText}`);
    }
}
export async function downloadPhoto(photoId) {
    const response = await fetch(`${apiBaseUrl}/photos/download/${photoId}`, {
        method: 'GET',
        credentials: 'include', // Ensure cookies are included
    });
    if (!response.ok) {
        throw new ApiError(`Failed to download photo: ${response.statusText}`, response.status);
    }
    return response.blob().then(blob => URL.createObjectURL(blob));
}
/*
  Album
 */
export async function getAlbums() {
    return fetchJson(`${apiBaseUrl}/albums`);
}
export async function createAlbum(album) {
    const response = await fetch(`${apiBaseUrl}/albums`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(album),
        credentials: 'include',
    });
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Fehler beim Erstellen des Albums: ${response.statusText} - ${errorText}`);
    }
}
export async function updateAlbum(albumId, updates) {
    await fetchJson(`${apiBaseUrl}/albums/${albumId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(updates),
    });
}
export async function deleteAlbum(albumId) {
    await fetchJson(`${apiBaseUrl}/albums/${albumId}`, {
        method: 'DELETE',
        credentials: 'include',
    });
}
/*
  Tags
 */
export async function getTags() {
    return fetchJson(`${apiBaseUrl}/tags`);
}
export async function getPhotoTags() {
    return fetchJson(`${apiBaseUrl}/photo-tags`);
}
export async function getAlbumTags() {
    return fetchJson(`${apiBaseUrl}/album-tags`);
}
export async function createTag(name) {
    await fetchJson(`${apiBaseUrl}/tags`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ name }),
    });
}
export async function deleteTag(tagId) {
    await fetchJson(`${apiBaseUrl}/tags/${tagId}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
        },
    });
}
export async function addTagToPhoto(photoId, tagId) {
    await fetchJson(`${apiBaseUrl}/photos/${photoId}/tags`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ tag_id: tagId }),
    });
}
export async function removeTagFromPhoto(photoId, tagId) {
    await fetchJson(`${apiBaseUrl}/photos/${photoId}/tags/${tagId}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
        },
    });
}
export async function addTagToAlbum(albumId, tagId) {
    return await fetchJson(`${apiBaseUrl}/albums/${albumId}/tags`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ tag_id: tagId }),
    });
}
export async function removeTagFromAlbum(albumId, tagId) {
    await fetchJson(`${apiBaseUrl}/albums/${albumId}/tags/${tagId}`, {
        method: 'DELETE',
    });
}
/*
  Album Foto
 */
export async function addPhotoToAlbum(photoId, albumId) {
    await fetchJson(`${apiBaseUrl}/albums/${albumId}/photos`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ photo_id: photoId }),
    });
}
export async function removePhotoFromAlbum(photoId, albumId) {
    await fetchJson(`${apiBaseUrl}/albums/${albumId}/photos/${photoId}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
        },
    });
}
