'use strict';
const $ = s => document.querySelector(s),
    $$ = s => [...document.querySelectorAll(s)];
const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
} [c]));
const safeMedia = (v, fallback = '') => typeof v === 'string' && (v.startsWith('assets/') || /^https:\/\//.test(v)) ? v : fallback;
const state = {
    token: sessionStorage.getItem('cadence-token'),
    user: null,
    songs: [],
    catalog: [],
    adminSongs: [],
    playlists: [],
    notices: [],
    genre: 'All',
    view: 'discover',
    selectedPlaylist: null,
    current: null,
    queue: [],
    shuffle: false,
    repeat: false
};
let authMode = 'login',
    editingSong = null,
    editingPlaylist = null,
    searchTimer, toastTimer, searchVersion = 0,
    confirmAction = null;
const audio = $('#audio');
audio.volume = .7;

function toast(text) {
    $('#toast').textContent = text;
    $('#toast').classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => $('#toast').classList.remove('show'), 4200);
}
async function api(path, options = {}) {
    const headers = {
        ...options.headers
    };
    if (state.token) headers.Authorization = 'Bearer ' + state.token;
    if (options.body !== undefined) {
        headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(options.body);
    }
    if (window.cadenceDemoApi) return window.cadenceDemoApi(path, options, state.token);
    let r;
    try {
        r = await fetch('/api' + path, {
            ...options,
            headers
        });
    } catch {
        throw Error('Cannot reach the library. Check that the services are running.');
    }
    if (r.status === 204) return null;
    const data = await r.json().catch(() => ({}));
    if (!r.ok) {
        if (r.status === 401 && !path.startsWith('/auth/login') && !path.startsWith('/auth/register')) clearSession();
        throw Error(data.message || data.detail || `Request failed (${r.status}). Please try again.`);
    }
    return data;
}

function clearSession() {
    state.token = null;
    state.user = null;
    state.songs = [];
    state.catalog = [];
    state.playlists = [];
    state.notices = [];
    state.adminSongs = [];
    sessionStorage.removeItem('cadence-token');
    stopPlayback();
    state.current = null;
    state.queue = [];
    $('#player-title').textContent = 'Make yourself at home';
    $('#player-artist').textContent = 'Choose a track to get started';
    renderAccount();
    renderSongs();
    renderPlaylists();
    renderNotices();
    $('#admin-songs').replaceChildren();
    changeView('discover');
}

function renderAccount() {
    $('#identity').textContent = state.user ? state.user.name : '';
    $('#auth-button').textContent = state.user ? 'Sign out' : 'Sign in ↗';
    $('#admin-nav').classList.toggle('hidden', state.user?.role !== 'ADMIN');
}

function openAuth() {
    const register = authMode === 'register';
    $('#auth-title').textContent = register ? 'Find your rhythm.' : 'Welcome back.';
    $('#name-label').classList.toggle('hidden', !register);
    $('#optin-label').classList.toggle('hidden', !register);
    $('#auth-form [name=name]').required = register;
    $('#auth-form [name=password]').autocomplete = register ? 'new-password' : 'current-password';
    $('#auth-form button[type=submit]').textContent = register ? 'Create account' : 'Sign in';
    $('#auth-prompt').textContent = register ? 'Already a member?' : 'New here?';
    $('#switch-auth').textContent = register ? 'Sign in' : 'Create an account';
    $('#auth-error').textContent = '';
    if (!$('#auth-dialog').open) $('#auth-dialog').showModal();
}
$('#switch-auth').onclick = () => {
    authMode = authMode === 'login' ? 'register' : 'login';
    openAuth();
};
$('#auth-form').onsubmit = async e => {
    e.preventDefault();
    const f = e.currentTarget,
        b = f.querySelector('button[type=submit]');
    b.disabled = true;
    $('#auth-error').textContent = '';
    try {
        const d = new FormData(f),
            body = {
                email: d.get('email'),
                password: d.get('password')
            };
        if (authMode === 'register') {
            body.name = d.get('name');
            body.emailOptIn = d.get('emailOptIn') === 'on';
        }
        const r = await api('/auth/' + authMode, {
            method: 'POST',
            body
        });
        state.token = r.token;
        state.user = r.user;
        sessionStorage.setItem('cadence-token', r.token);
        $('#auth-dialog').close();
        f.reset();
        renderAccount();
        await loadLibrary();
        toast('Welcome, ' + state.user.name + '.');
    } catch (err) {
        $('#auth-error').textContent = err.message;
    } finally {
        b.disabled = false;
    }
};
$('#auth-button').onclick = async () => {
    if (!state.user) {
        openAuth();
        return;
    }
    try {
        await api('/auth/logout', {
            method: 'POST'
        });
        clearSession();
        toast('Signed out.');
    } catch (e) {
        toast(e.message);
    }
};
$('#welcome-signin').onclick = openAuth;
$$('.close-dialog').forEach(b => b.onclick = () => b.closest('dialog').close());

function requireUser() {
    if (state.user) return true;
    openAuth();
    return false;
}

function changeView(view) {
    if (view !== 'discover' && !requireUser()) return;
    if (view === 'admin' && state.user?.role !== 'ADMIN') return;
    state.view = view;
    $$('.view').forEach(v => v.classList.add('hidden'));
    $('#' + view + '-view').classList.remove('hidden');
    $$('[data-view]').forEach(b => b.classList.toggle('active', b.dataset.view === view));
    $('#breadcrumb').textContent = {
        discover: 'Discover',
        playlists: 'Your playlists',
        notifications: 'Notifications',
        admin: 'Manage library'
    } [view];
    if (view === 'admin') loadAdmin();
    if (view === 'notifications') loadNotices();
    if (view === 'playlists') loadPlaylists();
}
$$('[data-view]').forEach(b => b.onclick = () => changeView(b.dataset.view));
$('.brand').onclick = e => {
    e.preventDefault();
    changeView('discover');
};
async function loadLibrary() {
    await Promise.all([loadSongs(), loadPlaylists(), loadNotices()]);
}
async function loadSongs() {
    const version = ++searchVersion;
    $('#song-count').textContent = 'Loading…';
    try {
        const songs = await api('/songs?keyword=' + encodeURIComponent($('#search').value));
        if (version !== searchVersion) return;
        state.songs = songs;
        renderSongs();
    } catch (e) {
        if (version !== searchVersion) return;
        $('#library-state').classList.remove('hidden');
        $('#library-state').innerHTML = `<h3>Couldn’t load music</h3><p>${esc(e.message)}</p><button class="button subtle" id="retry-library">Try again</button>`;
        $('#retry-library').onclick = loadSongs;
        $('#song-count').textContent = '';
    }
}

function shownSongs() {
    return state.songs.filter(s => state.genre === 'All' || s.genre === state.genre);
}

function renderSongs() {
    const songs = shownSongs();
    $('#song-grid').innerHTML = songs.map(s => `<article class="song-card"><div class="cover-wrap"><img loading="lazy" src="${esc(safeMedia(s.coverImageUrl,'assets/cover-0.svg'))}" alt="${esc(s.albumName)} artwork"><span class="card-tag">${esc(s.genre)}</span><button class="card-play" data-play="${s.id}" aria-label="Play ${esc(s.name)}">▶</button></div><div class="song-card-info"><div><button class="song-title" data-detail="${s.id}">${esc(s.name)}</button><p>${esc(s.singer)} · ${formatTime(s.durationSeconds)}</p></div><button class="icon-button" data-add="${s.id}" aria-label="Add ${esc(s.name)} to playlist">+</button></div></article>`).join('');
    $('#song-count').textContent = songs.length + ' tracks';
    $('#library-state').classList.toggle('hidden', songs.length > 0);
    if (!songs.length) {
        $('#library-state').innerHTML = state.user ? '<h3>No tracks here yet.</h3><p>Try a different search or genre.</p>' : '<h3>Your next favorite starts here.</h3><p>Sign in or create an account to explore the library.</p><button class="button primary" id="welcome-signin">Explore with an account ↗</button>';
        const b = $('#welcome-signin');
        if (b) b.onclick = openAuth;
    }
    imageFallbacks();
}

function imageFallbacks() {
    $$('img').forEach(i => i.onerror = () => {
        i.onerror = null;
        i.src = 'assets/cover-0.svg';
    });
}
$('#search').oninput = () => {
    clearTimeout(searchTimer);
    if (state.user) searchTimer = setTimeout(loadSongs, 260);
};
$('#genres').onclick = e => {
    const b = e.target.closest('[data-genre]');
    if (!b) return;
    state.genre = b.dataset.genre;
    $$('[data-genre]').forEach(x => x.classList.toggle('selected', x === b));
    renderSongs();
};
document.addEventListener('click', async e => {
    const play = e.target.closest('[data-play]'),
        detail = e.target.closest('[data-detail]'),
        add = e.target.closest('[data-add]');
    if (play) await playSong(Number(play.dataset.play));
    if (detail) await detailSong(Number(detail.dataset.detail));
    if (add) await pickPlaylist(Number(add.dataset.add));
});
async function detailSong(id) {
    try {
        const s = await api('/songs/' + id);
        $('#song-detail').innerHTML = `<img class="detail-cover" src="${esc(safeMedia(s.coverImageUrl,'assets/cover-0.svg'))}" alt="Album artwork"><p class="eyebrow">${esc(s.genre)}</p><h2>${esc(s.name)}</h2><dl class="details">${[['Artist',s.singer],['Album',s.albumName],['Music director',s.musicDirector],['Release date',s.releaseDate]].map(([k,v])=>`<div><dt>${k}</dt><dd>${esc(v)}</dd></div>`).join('')}</dl><button class="button primary" data-play="${s.id}">▶ Play song</button> <button class="button subtle" data-add="${s.id}">+ Playlist</button>`;
        $('#detail-dialog').showModal();
        imageFallbacks();
    } catch (e) {
        toast(e.message);
    }
}

function formatTime(v) {
    const n = Math.max(0, Math.floor(Number(v) || 0));
    return Math.floor(n / 60) + ':' + String(n % 60).padStart(2, '0');
}
async function playSong(id, queue) {
    if (!requireUser()) return;
    try {
        const s = await api('/songs/' + id);
        const url = safeMedia(s.audioUrl);
        if (!url) throw Error('This song has no playable audio yet.');
        state.queue = queue || currentQueue();
        if (!state.queue.includes(id)) state.queue.push(id);
        state.current = s;
        audio.src = url;
        $('#player-cover').src = safeMedia(s.coverImageUrl, 'assets/cover-0.svg');
        $('#player-title').textContent = s.name;
        $('#player-artist').textContent = s.singer;
        await audio.play();
    } catch (e) {
        toast(e.message || 'Playback could not start.');
    }
}

function currentQueue() {
    if (state.view === 'playlists' && state.selectedPlaylist) {
        const p = state.playlists.find(x => x.id === state.selectedPlaylist);
        return p ? p.songIds.filter(id => state.catalog.some(s => s.id === id)) : [];
    }
    return shownSongs().map(s => s.id);
}
$('#hero-play').onclick = () => {
    if (!requireUser()) return;
    const s = shownSongs()[0];
    if (s) playSong(s.id);
    else toast('No songs match this selection.');
};
$('#play-toggle').onclick = async () => {
    if (!requireUser()) return;
    if (!state.current) {
        $('#hero-play').click();
        return;
    }
    if (audio.paused) {
        try {
            await api('/songs/' + state.current.id);
            await audio.play();
        } catch (e) {
            stopPlayback();
            toast(e.message);
        }
    } else audio.pause();
};
audio.onplay = () => {
    $('#play-toggle').textContent = 'Ⅱ';
    $('#play-toggle').setAttribute('aria-label', 'Pause');
};
audio.onpause = () => {
    $('#play-toggle').textContent = '▶';
    $('#play-toggle').setAttribute('aria-label', 'Play');
};
audio.ontimeupdate = () => {
    $('#elapsed').textContent = formatTime(audio.currentTime);
    $('#seek').value = audio.duration ? audio.currentTime / audio.duration * 100 : 0;
};
audio.onloadedmetadata = () => $('#duration').textContent = formatTime(audio.duration);
audio.onerror = () => toast('Audio could not load. The source may be unavailable or blocked.');
$('#seek').oninput = e => {
    if (Number.isFinite(audio.duration)) audio.currentTime = audio.duration * Number(e.target.value) / 100;
};
$('#volume').oninput = e => audio.volume = Number(e.target.value);

function stopPlayback() {
    audio.pause();
    audio.currentTime = 0;
    $('#seek').value = 0;
}
$('#stop').onclick = stopPlayback;
async function advance(direction = 1) {
    if (!state.queue.length) return;
    const index = state.queue.indexOf(state.current?.id);
    let next = (index + direction + state.queue.length) % state.queue.length;
    if (state.shuffle && state.queue.length > 1) {
        const choices = state.queue.filter(id => id !== state.current?.id);
        await playSong(choices[Math.floor(Math.random() * choices.length)], [...state.queue]);
    } else await playSong(state.queue[next], [...state.queue]);
}
$('#previous').onclick = () => {
    if (audio.currentTime > 3) audio.currentTime = 0;
    else advance(-1);
};
$('#next').onclick = () => advance();
$('#shuffle').onclick = () => {
    state.shuffle = !state.shuffle;
    $('#shuffle').setAttribute('aria-pressed', state.shuffle);
};
$('#repeat').onclick = () => {
    state.repeat = !state.repeat;
    $('#repeat').setAttribute('aria-pressed', state.repeat);
};
audio.onended = () => state.repeat ? playSong(state.current.id, [...state.queue]) : advance();
async function loadPlaylists() {
    if (!state.user) return;
    try {
        [state.playlists, state.catalog] = await Promise.all([api('/playlists'), api('/songs')]);
        renderPlaylists();
    } catch (e) {
        toast(e.message);
    }
}

function renderPlaylists() {
    $('#sidebar-playlists').innerHTML = state.playlists.map(p => `<button class="sidebar-playlist" data-playlist="${p.id}">♫ &nbsp; ${esc(p.name)}</button>`).join('');
    $('#playlist-grid').innerHTML = state.playlists.length ? state.playlists.map(p => `<button class="playlist-card" data-playlist="${p.id}"><span>♫</span><strong>${esc(p.name)}</strong><small>${p.songIds.length} saved tracks</small></button>`).join('') : '<p class="muted">Your playlists will live here. Create one and add music from Discover.</p>';
    $$('[data-playlist]').forEach(b => b.onclick = () => {
        state.selectedPlaylist = Number(b.dataset.playlist);
        changeView('playlists');
        renderPlaylistDetail();
    });
    renderPlaylistDetail();
}

function renderPlaylistDetail() {
    const p = state.playlists.find(p => p.id === state.selectedPlaylist);
    $('#playlist-detail').classList.toggle('hidden', !p);
    if (!p) return;
    $('#playlist-title').textContent = p.name;
    const q = $('#playlist-search').value.toLowerCase();
    const songs = p.songIds.map(id => state.catalog.find(s => s.id === id) || {
        id,
        name: 'Unavailable track',
        singer: 'Hidden or removed from the library'
    }).filter(s => (s.name + ' ' + s.singer + ' ' + (s.albumName || '')).toLowerCase().includes(q));
    $('#playlist-songs').innerHTML = songs.map(s => `<div class="track-row"><img src="${esc(safeMedia(s.coverImageUrl,'assets/cover-0.svg'))}" alt=""><div class="track-copy"><strong>${esc(s.name)}</strong><small>${esc(s.singer)}</small></div>${s.audioUrl?`<button class="icon-button" data-play="${s.id}" aria-label="Play ${esc(s.name)}">▶</button>`:''}<button class="icon-button" data-remove="${s.id}" aria-label="Remove ${esc(s.name)} from playlist">×</button></div>`).join('') || '<p class="muted">No tracks to show. Add songs using the + button in Discover.</p>';
    $$('[data-remove]').forEach(b => b.onclick = async () => {
        try {
            await api(`/playlists/${p.id}/songs/${b.dataset.remove}`, {
                method: 'DELETE'
            });
            await loadPlaylists();
            toast('Removed from playlist.');
        } catch (e) {
            toast(e.message);
        }
    });
    imageFallbacks();
}
$('#playlist-search').oninput = renderPlaylistDetail;

function playlistForm(rename = false) {
    if (!requireUser()) return;
    editingPlaylist = rename ? state.selectedPlaylist : null;
    $('#playlist-dialog-title').textContent = rename ? 'Rename playlist' : 'New playlist';
    $('#playlist-form [name=name]').value = rename ? state.playlists.find(p => p.id === editingPlaylist).name : '';
    $('#playlist-error').textContent = '';
    $('#playlist-dialog').showModal();
}
$('#sidebar-new').onclick = () => playlistForm();
$('#new-playlist').onclick = () => playlistForm();
$('#rename-playlist').onclick = () => playlistForm(true);
$('#playlist-form').onsubmit = async e => {
    e.preventDefault();
    const b = e.currentTarget.querySelector('button');
    b.disabled = true;
    try {
        const p = await api('/playlists' + (editingPlaylist ? '/' + editingPlaylist : ''), {
            method: editingPlaylist ? 'PUT' : 'POST',
            body: {
                name: new FormData(e.currentTarget).get('name')
            }
        });
        state.selectedPlaylist = p.id;
        $('#playlist-dialog').close();
        await loadPlaylists();
        changeView('playlists');
        toast('Playlist saved.');
    } catch (e) {
        $('#playlist-error').textContent = e.message;
    } finally {
        b.disabled = false;
    }
};

function confirmDelete(title, message, action) {
    $('#confirm-title').textContent = title;
    $('#confirm-message').textContent = message;
    confirmAction = action;
    $('#confirm-dialog').showModal();
}
$('#confirm-cancel').onclick = () => $('#confirm-dialog').close();
$('#confirm-yes').onclick = async () => {
    const b = $('#confirm-yes');
    b.disabled = true;
    try {
        await confirmAction();
        $('#confirm-dialog').close();
    } catch (e) {
        toast(e.message);
    } finally {
        b.disabled = false;
    }
};
$('#delete-playlist').onclick = () => {
    const p = state.playlists.find(p => p.id === state.selectedPlaylist);
    if (p) confirmDelete('Delete playlist?', `“${p.name}” will be removed. The songs remain in the library.`, async () => {
        await api('/playlists/' + p.id, {
            method: 'DELETE'
        });
        state.selectedPlaylist = null;
        await loadPlaylists();
        toast('Playlist deleted.');
    });
};
async function pickPlaylist(id) {
    if (!requireUser()) return;
    await loadPlaylists();
    if (!state.playlists.length) {
        toast('Create a playlist first, then add your song.');
        playlistForm();
        return;
    }
    $('#pick-list').innerHTML = state.playlists.map(p => `<button class="button subtle full" data-pick="${p.id}">${esc(p.name)} ${p.songIds.includes(id)?'✓':''}</button>`).join('');
    $$('[data-pick]').forEach(b => b.onclick = async () => {
        b.disabled = true;
        try {
            await api(`/playlists/${b.dataset.pick}/songs/${id}`, {
                method: 'POST'
            });
            $('#pick-dialog').close();
            await loadPlaylists();
            toast('Added to playlist.');
        } catch (e) {
            toast(e.message);
        } finally {
            b.disabled = false;
        }
    });
    $('#pick-dialog').showModal();
}
async function loadNotices() {
    if (!state.user) return;
    try {
        state.notices = await api('/notifications');
        renderNotices();
    } catch (e) {
        if (state.view === 'notifications') toast(e.message);
    }
}

function renderNotices() {
    const unread = state.notices.filter(n => !n.read).length;
    $('#notice-count').textContent = unread || '';
    $('#notification-list').innerHTML = state.notices.map(n => `<article class="notice"><span>${n.read?'○':'●'}</span><p>${esc(n.message)}<small>${esc(new Date(n.createdAt).toLocaleString())}</small></p><button class="button subtle" data-notice="${esc(n.id)}">${n.read?'Read':'Mark read'}</button></article>`).join('') || '<div class="empty-state"><h3>You’re all caught up.</h3><p>New releases will appear here when the admin adds music.</p></div>';
    $$('[data-notice]').forEach(b => b.onclick = async () => {
        try {
            await api('/notifications/' + b.dataset.notice + '/read', {
                method: 'PUT'
            });
            await loadNotices();
        } catch (e) {
            toast(e.message);
        }
    });
}
async function loadAdmin() {
    try {
        state.adminSongs = await api('/admin/songs');
        $('#admin-songs').innerHTML = state.adminSongs.map(s => `<tr><td><strong>${esc(s.name)}</strong><small>${esc(s.singer)}</small></td><td>${esc(s.albumName)}</td><td><span class="status ${s.visible?'':'off'}">${s.visible?'Visible':'Hidden'}</span></td><td><div class="actions"><button class="button subtle" data-edit="${s.id}">Edit</button><button class="button danger" data-delete="${s.id}">Delete</button></div></td></tr>`).join('');
        $$('[data-edit]').forEach(b => b.onclick = () => songForm(Number(b.dataset.edit)));
        $$('[data-delete]').forEach(b => b.onclick = () => {
            const s = state.adminSongs.find(s => s.id === Number(b.dataset.delete));
            confirmDelete('Delete song?', `“${s.name}” will be removed from the library.`, async () => {
                await api('/admin/songs/' + s.id, {
                    method: 'DELETE'
                });
                if (state.current?.id === s.id) stopPlayback();
                await Promise.all([loadAdmin(), loadSongs()]);
                toast('Song deleted.');
            });
        });
    } catch (e) {
        toast(e.message);
    }
}

function songForm(id = null) {
    editingSong = id;
    const form = $('#song-form');
    form.reset();
    $('#song-error').textContent = '';
    $('#song-form-title').textContent = id ? 'Edit song' : 'Add a song';
    if (id) {
        const s = state.adminSongs.find(s => s.id === id);
        for (const [k, v] of Object.entries(s)) {
            const el = form.elements.namedItem(k);
            if (el) {
                if (el.type === 'checkbox') el.checked = v;
                else el.value = v ?? '';
            }
        }
    }
    $('#song-dialog').showModal();
}
$('#add-song').onclick = () => songForm();
$('#song-form').onsubmit = async e => {
    e.preventDefault();
    const form = e.currentTarget,
        b = form.querySelector('button');
    b.disabled = true;
    try {
        const body = Object.fromEntries(new FormData(form));
        body.visible = form.elements.visible.checked;
        body.durationSeconds = Number(body.durationSeconds);
        await api('/admin/songs' + (editingSong ? '/' + editingSong : ''), {
            method: editingSong ? 'PUT' : 'POST',
            body
        });
        $('#song-dialog').close();
        await Promise.all([loadAdmin(), loadSongs()]);
        toast('Song saved. New-release notifications may take a few seconds.');
    } catch (e) {
        $('#song-error').textContent = e.message;
    } finally {
        b.disabled = false;
    }
};
setInterval(() => {
    if (state.user) loadNotices();
}, 30000);
(async () => {
    renderAccount();
    if (state.token) {
        try {
            state.user = await api('/auth/me');
            renderAccount();
            await loadLibrary();
        } catch {
            clearSession();
        }
    }
})();

