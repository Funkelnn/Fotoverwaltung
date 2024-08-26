import { eventBus } from '../utils/eventBus.js';
import {store} from '../store.js';

export async function initializeSidebar() {
  const toggleSidebarOnButton = document.getElementById('toggleSidebarOn') as HTMLElement;
  const toggleSidebarOffButton = document.getElementById('toggleSidebarOff') as HTMLElement;
  const sidebar = document.querySelector('.sidebar') as HTMLElement;
  const sidebarPlaceholder = document.querySelector('.sidebar-placeholder') as HTMLElement;

  toggleSidebarOnButton.addEventListener('click', () => {
    sidebar.classList.remove('minimized');
    sidebarPlaceholder.style.width = '14em';
    toggleSidebarOnButton.style.display = 'none';
  });

  toggleSidebarOffButton.addEventListener('click', () => {
    sidebar.classList.add('minimized');
    sidebarPlaceholder.style.width = '3.125em';
    toggleSidebarOnButton.style.display = 'block';
  });

  eventBus.on('albumCreated', () => {
    loadAlbums();
  });

  eventBus.on('albumUpdated', () => {
    loadAlbums();
  });

  eventBus.on('albumDeleted', (albumId: number) => {
    loadAlbums();
  });

  loadAlbums();

  const albumList = document.getElementById('albumList') as HTMLElement;
  albumList.addEventListener('click', (event: MouseEvent) => {
    const target = event.target as HTMLElement;
    if (target.tagName === 'LI') {
      const albumId = target.dataset.albumId;
      eventBus.emit('albumSelected', albumId);
      highlightSelectedAlbum(target);
    }
  });

  const searchInput = document.getElementById('searchInputAlbum') as HTMLInputElement;
  searchInput.addEventListener('input', () => {
    const query = searchInput.value.toLowerCase();
    filterAlbums(query);
  });
}


function loadAlbums() {
  const albums = store.getAlbums();

  const albumList = document.getElementById('albumList') as HTMLElement;
  albumList.innerHTML = '';

  const allPhotosItem = document.createElement('li');
  allPhotosItem.textContent = 'Alle Fotos';
  allPhotosItem.dataset.albumId = '';
  allPhotosItem.addEventListener('click', () => highlightSelectedAlbum(allPhotosItem));
  albumList.appendChild(allPhotosItem);
  highlightSelectedAlbum(allPhotosItem)

  albums.forEach(album => {
    const li = document.createElement('li');
    li.textContent = album.title;
    li.dataset.albumId = album.album_id.toString();

    const editButton = document.createElement('button');
    editButton.textContent = 'B';
    editButton.classList.add('edit-button');
    editButton.addEventListener('click', (event) => {
      event.stopPropagation();
      eventBus.emit('editAlbum', album.album_id);
    });

    li.appendChild(editButton);
    albumList.appendChild(li);
  });
}


function filterAlbums(query: string) {
  const albums = store.getAlbums();
  const tags = store.getTags();
  const albumList = document.getElementById('albumList') as HTMLElement;
  albumList.innerHTML = '';

  if (query === '') {
    const allPhotosItem = document.createElement('li');
    allPhotosItem.textContent = 'Alle Fotos';
    allPhotosItem.dataset.albumId = '';
    allPhotosItem.addEventListener('click', () => highlightSelectedAlbum(allPhotosItem));
    albumList.appendChild(allPhotosItem);
  }

  const filteredAlbums = albums.filter(album => {
    return album.title.toLowerCase().includes(query) ||
      album.tag_ids.some(tagId => {
        const tag = tags.find(tag => tag.tag_id === tagId);
        return tag?.name.toLowerCase().includes(query);
      });
  });

  filteredAlbums.forEach(album => {
    const li = document.createElement('li');
    li.textContent = album.title;
    li.dataset.albumId = album.album_id.toString();

    const editButton = document.createElement('button');
    editButton.textContent = 'B';
    editButton.classList.add('edit-button');
    editButton.addEventListener('click', (event) => {
      event.stopPropagation();
      eventBus.emit('editAlbum', album.album_id);
    });

    li.appendChild(editButton);
    albumList.appendChild(li);
  });
}

function highlightSelectedAlbum(selectedItem: HTMLElement) {
  const albumList = document.getElementById('albumList') as HTMLElement;
  const items = albumList.getElementsByTagName('li');
  for (let i = 0; i < items.length; i++) {
    items[i].style.color = '';
  }
  selectedItem.style.color = '#018be1';
}
