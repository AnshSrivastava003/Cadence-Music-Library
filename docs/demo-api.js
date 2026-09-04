'use strict';
(() => {
  const KEY = 'cadence-demo-v1';
  const now = () => new Date().toISOString();
  const seeds = [
    ['After Hours','Night Assembly','Mira Vale','City Signals','2025-01-17','Electronic',0],
    ['Golden Hour','Lena Coast','Theo Arden','Slow Sundays','2024-08-09','Chill',1],
    ['Blue Orbit','Parallel Lines','Iris North','Somewhere Else','2025-03-21','Ambient',2],
    ['Sunday Morning','The Quiet Club','Noah Reed','Window Seat','2024-05-12','Lo-fi',3],
    ['Neon Avenue','Night Assembly','Mira Vale','Electric Weather','2025-02-28','Electronic',4],
    ['Open Water','Lena Coast','Theo Arden','Tidal Rooms','2024-11-08','Ambient',5]
  ].map((s, i) => ({
    id: i + 1, name: s[0], singer: s[1], musicDirector: s[2], albumName: s[3],
    releaseDate: s[4], genre: s[5], coverImageUrl: `assets/cover-${s[6]}.svg`,
    audioUrl: `assets/demo-${s[6]}.wav`, durationSeconds: 24, visible: true
  }));

  function initial() {
    return {
      nextSong: 7, nextPlaylist: 2, nextNotice: 2,
      users: [{email:'admin@cadence.demo', password:'cadence-demo', name:'Library Admin', role:'ADMIN'}],
      songs: seeds,
      playlists: [{id:1,email:'admin@cadence.demo',name:'My Favorites',songIds:[6]}],
      notices: [{id:1,email:'admin@cadence.demo',message:'Welcome to the Cadence portfolio demo.',createdAt:now(),read:false}]
    };
  }
  function load() {
    try { return {...initial(), ...JSON.parse(localStorage.getItem(KEY) || '{}')}; }
    catch { return initial(); }
  }
  const save = db => localStorage.setItem(KEY, JSON.stringify(db));
  const fail = (message, status = 400) => { const e = Error(message); e.status = status; throw e; };
  const emailFrom = token => token?.startsWith('demo:') ? token.slice(5) : null;
  const publicUser = u => ({name:u.name,email:u.email,role:u.role});
  const bodyOf = options => typeof options.body === 'string' ? JSON.parse(options.body) : (options.body || {});

  window.cadenceDemoApi = async (rawPath, options = {}, token) => {
    await new Promise(resolve => setTimeout(resolve, 80));
    const db = load();
    const method = (options.method || 'GET').toUpperCase();
    const url = new URL(rawPath, location.origin);
    const path = url.pathname;
    const email = emailFrom(token);
    const user = db.users.find(u => u.email === email);
    const requireUser = () => user || fail('Please sign in to continue.', 401);
    const requireAdmin = () => requireUser().role === 'ADMIN' || fail('Administrator access is required.', 403);

    if (path === '/auth/register' && method === 'POST') {
      const b = bodyOf(options), normalized = String(b.email || '').trim().toLowerCase();
      if (!b.name || !normalized || String(b.password || '').length < 10) fail('Enter a name, valid email and a password of at least 10 characters.');
      if (db.users.some(u => u.email === normalized)) fail('An account with that email already exists.');
      const created = {email:normalized,password:b.password,name:String(b.name).trim(),role:'USER'};
      db.users.push(created);
      db.playlists.push({id:db.nextPlaylist++,email:normalized,name:'My Favorites',songIds:[]});
      db.notices.push({id:db.nextNotice++,email:normalized,message:'Welcome to Cadence. Your browser-local demo space is ready.',createdAt:now(),read:false});
      save(db); return {token:'demo:' + normalized,user:publicUser(created)};
    }
    if (path === '/auth/login' && method === 'POST') {
      const b = bodyOf(options), found = db.users.find(u => u.email === String(b.email || '').trim().toLowerCase() && u.password === b.password);
      if (!found) fail('Incorrect email or password.', 401);
      return {token:'demo:' + found.email,user:publicUser(found)};
    }
    if (path === '/auth/me') return publicUser(requireUser());
    if (path === '/auth/logout') return null;

    if (path === '/songs' && method === 'GET') {
      requireUser(); const q = (url.searchParams.get('keyword') || '').toLowerCase();
      return db.songs.filter(s => s.visible && [s.name,s.singer,s.albumName,s.musicDirector,s.genre].join(' ').toLowerCase().includes(q));
    }
    const songMatch = path.match(/^\/songs\/(\d+)$/);
    if (songMatch) {
      requireUser(); const song = db.songs.find(s => s.id === Number(songMatch[1]) && s.visible);
      return song || fail('This track is unavailable.', 404);
    }

    if (path === '/playlists' && method === 'GET') { requireUser(); return db.playlists.filter(p => p.email === email); }
    if (path === '/playlists' && method === 'POST') {
      requireUser(); const name = String(bodyOf(options).name || '').trim(); if (!name) fail('Enter a playlist name.');
      const p = {id:db.nextPlaylist++,email,name,songIds:[]}; db.playlists.push(p); save(db); return p;
    }
    const playlistMatch = path.match(/^\/playlists\/(\d+)$/);
    if (playlistMatch) {
      requireUser(); const id = Number(playlistMatch[1]), p = db.playlists.find(x => x.id === id && x.email === email); if (!p) fail('Playlist not found.',404);
      if (method === 'PUT') { const name = String(bodyOf(options).name || '').trim(); if (!name) fail('Enter a playlist name.'); p.name = name; save(db); return p; }
      if (method === 'DELETE') { db.playlists = db.playlists.filter(x => x !== p); save(db); return null; }
    }
    const playlistSong = path.match(/^\/playlists\/(\d+)\/songs\/(\d+)$/);
    if (playlistSong) {
      requireUser(); const p = db.playlists.find(x => x.id === Number(playlistSong[1]) && x.email === email); if (!p) fail('Playlist not found.',404);
      const songId = Number(playlistSong[2]);
      if (method === 'POST' && !p.songIds.includes(songId)) p.songIds.push(songId);
      if (method === 'DELETE') p.songIds = p.songIds.filter(id => id !== songId);
      save(db); return p;
    }

    if (path === '/notifications' && method === 'GET') { requireUser(); return db.notices.filter(n => n.email === email).sort((a,b) => b.createdAt.localeCompare(a.createdAt)); }
    const noticeMatch = path.match(/^\/notifications\/(\d+)\/read$/);
    if (noticeMatch && method === 'PUT') { requireUser(); const n = db.notices.find(x => x.id === Number(noticeMatch[1]) && x.email === email); if (n) n.read = true; save(db); return n; }

    if (path === '/admin/songs' && method === 'GET') { requireAdmin(); return db.songs; }
    if (path === '/admin/songs' && method === 'POST') {
      requireAdmin(); const s = {...bodyOf(options),id:db.nextSong++}; db.songs.push(s);
      db.users.forEach(u => db.notices.push({id:db.nextNotice++,email:u.email,message:`New release: ${s.name} by ${s.singer}.`,createdAt:now(),read:false}));
      save(db); return s;
    }
    const adminSong = path.match(/^\/admin\/songs\/(\d+)$/);
    if (adminSong) {
      requireAdmin(); const id = Number(adminSong[1]), index = db.songs.findIndex(s => s.id === id); if (index < 0) fail('Song not found.',404);
      if (method === 'PUT') { db.songs[index] = {...bodyOf(options),id}; save(db); return db.songs[index]; }
      if (method === 'DELETE') { db.songs.splice(index,1); save(db); return null; }
    }
    fail(`Demo route is unavailable: ${method} ${path}`, 404);
  };
})();
