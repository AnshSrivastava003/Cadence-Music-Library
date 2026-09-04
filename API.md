# API guide

Use the gateway at `http://localhost:8090`. Except register/login, API calls require `Authorization: Bearer <token>` returned by login. Admin calls require role ADMIN. JSON request bodies use `Content-Type: application/json`. Logout invalidates the current session across services.

| Method | Path | Purpose |
|---|---|---|
| POST | /api/auth/register | name, email, password (10–64 characters), emailOptIn |
| POST | /api/auth/login | email, password; returns token, expiresAt, user |
| GET | /api/auth/me | Current user's safe profile |
| POST | /api/auth/logout | Revoke current token; 204 |
| GET | /api/songs?keyword= | Visible songs, optional case-insensitive substring search |
| GET | /api/songs/search?keyword= | Alias for search |
| GET | /api/songs/{id} | Visible song or 404 |
| GET | /api/admin/songs | All songs, including hidden (ADMIN) |
| POST | /api/admin/songs | Add song (ADMIN), 201 |
| PUT | /api/admin/songs/{id} | Replace editable metadata (ADMIN) |
| DELETE | /api/admin/songs/{id} | Delete song (ADMIN), 204 |
| GET | /api/playlists | Current user's playlists |
| POST | /api/playlists | Create with {"name":"..."} |
| PUT | /api/playlists/{id} | Rename with {"name":"..."} |
| DELETE | /api/playlists/{id} | Delete own playlist |
| POST | /api/playlists/{id}/songs/{songId} | Add visible song, no duplicates |
| DELETE | /api/playlists/{id}/songs/{songId} | Remove song from own playlist |
| GET | /api/notifications | Current user's latest 100 notices |
| PUT | /api/notifications/{id}/read | Mark own notice read |

Song POST/PUT body:

```json
{"name":"Example","singer":"Artist","musicDirector":"Composer","releaseDate":"2026-01-01","albumName":"Album","coverImageUrl":"/assets/cover-0.svg","audioUrl":"/assets/demo-0.wav","visible":true,"genre":"Ambient","durationSeconds":24}
```

The server assigns IDs. Required text cannot be blank; names and URLs have length limits; duration is 0–86400 seconds. Missing/hidden songs and playlists not owned by the caller return 404. Invalid input returns 400. Wrong role returns 403. Unavailable downstream services return 503 from the gateway.

Internal endpoints under `/internal/` require the generated `X-Service-Key`. They are not exposed by the gateway. Eureka/health are local development endpoints. The database is private to its owning service.
