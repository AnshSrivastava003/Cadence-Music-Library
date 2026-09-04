# File structure and source guide

Extract the ZIP as a whole. Every code file is already at its required location; you do not need to create or copy files one at a time. All paths below are relative to the extracted `music-library-complete` folder.

Do not paste Markdown fences into Java files. If editing manually, replace only the contents of the named file and keep its package declaration.

## Complete tree

```text
music-library-complete/
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── admin-service/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── ansh/
│   │       │           └── musiclibrary/
│   │       │               └── adminservice/
│   │       │                   ├── Application.java
│   │       │                   └── Gateway.java
│   │       └── resources/
│   │           ├── static/
│   │           │   ├── assets/
│   │           │   │   ├── cover-0.svg
│   │           │   │   ├── cover-1.svg
│   │           │   │   ├── cover-2.svg
│   │           │   │   ├── cover-3.svg
│   │           │   │   ├── cover-4.svg
│   │           │   │   ├── cover-5.svg
│   │           │   │   ├── demo-0.wav
│   │           │   │   ├── demo-1.wav
│   │           │   │   ├── demo-2.wav
│   │           │   │   ├── demo-3.wav
│   │           │   │   ├── demo-4.wav
│   │           │   │   └── demo-5.wav
│   │           │   ├── app.js
│   │           │   ├── index.html
│   │           │   └── styles.css
│   │           └── application.properties
│   └── pom.xml
├── common/
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/
│   │               └── ansh/
│   │                   └── musiclibrary/
│   │                       └── common/
│   │                           ├── Errors.java
│   │                           ├── SecurityConfig.java
│   │                           ├── SessionVerifier.java
│   │                           └── TomcatConfig.java
│   └── pom.xml
├── discovery-server/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── ansh/
│   │       │           └── musiclibrary/
│   │       │               └── discoveryserver/
│   │       │                   ├── Application.java
│   │       │                   └── TomcatConfig.java
│   │       └── resources/
│   │           └── application.properties
│   └── pom.xml
├── notification-service/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── ansh/
│   │       │           └── musiclibrary/
│   │       │               └── notificationservice/
│   │       │                   ├── Application.java
│   │       │                   ├── EmailWorker.java
│   │       │                   ├── Notice.java
│   │       │                   ├── NoticeController.java
│   │       │                   └── NoticeRepository.java
│   │       └── resources/
│   │           └── application.properties
│   └── pom.xml
├── scripts/
│   ├── Start.ps1
│   ├── Stop.ps1
│   └── Verify.ps1
├── song-service/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── ansh/
│   │       │           └── musiclibrary/
│   │       │               └── songservice/
│   │       │                   ├── Application.java
│   │       │                   ├── DemoLibrary.java
│   │       │                   ├── EventRepository.java
│   │       │                   ├── Outbox.java
│   │       │                   ├── Song.java
│   │       │                   ├── SongController.java
│   │       │                   ├── SongEvent.java
│   │       │                   └── SongRepository.java
│   │       └── resources/
│   │           └── application.properties
│   └── pom.xml
├── user-service/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── ansh/
│   │       │           └── musiclibrary/
│   │       │               └── userservice/
│   │       │                   ├── Account.java
│   │       │                   ├── AccountRepository.java
│   │       │                   ├── Application.java
│   │       │                   ├── AuthController.java
│   │       │                   ├── LoginSession.java
│   │       │                   ├── Playlist.java
│   │       │                   ├── PlaylistController.java
│   │       │                   ├── PlaylistRepository.java
│   │       │                   └── SessionRepository.java
│   │       └── resources/
│   │           └── application.properties
│   └── pom.xml
├── .gitignore
├── API.md
├── ASSETS.md
├── FILE-GUIDE.md
├── README.md
├── Start.cmd
├── Stop.cmd
├── VERIFICATION.md
├── mvnw
├── mvnw.cmd
└── pom.xml
```

## File purposes

| File path | Purpose |
|---|---|
| `.gitignore` | Keeps generated credentials, databases, logs and build outputs out of Git. |
| `.mvn/wrapper/maven-wrapper.properties` | Maven distribution URL used by the wrapper. |
| `admin-service/pom.xml` | Maven dependencies/build configuration; the root POM also lists all modules. |
| `admin-service/src/main/java/com/ansh/musiclibrary/adminservice/Application.java` | Spring Boot entry point; enables this service and its component scanning. |
| `admin-service/src/main/java/com/ansh/musiclibrary/adminservice/Gateway.java` | Forwards frontend API requests to the appropriate discovered service. |
| `admin-service/src/main/resources/application.properties` | Service port, database, discovery and environment-based settings. |
| `admin-service/src/main/resources/static/app.js` | Frontend state, API calls, login, search, playlists, player and admin interactions. |
| `admin-service/src/main/resources/static/assets/cover-0.svg` | Original SVG cover asset. |
| `admin-service/src/main/resources/static/assets/cover-1.svg` | Original SVG cover asset. |
| `admin-service/src/main/resources/static/assets/cover-2.svg` | Original SVG cover asset. |
| `admin-service/src/main/resources/static/assets/cover-3.svg` | Original SVG cover asset. |
| `admin-service/src/main/resources/static/assets/cover-4.svg` | Original SVG cover asset. |
| `admin-service/src/main/resources/static/assets/cover-5.svg` | Original SVG cover asset. |
| `admin-service/src/main/resources/static/assets/demo-0.wav` | Original synthesized WAV demo track. |
| `admin-service/src/main/resources/static/assets/demo-1.wav` | Original synthesized WAV demo track. |
| `admin-service/src/main/resources/static/assets/demo-2.wav` | Original synthesized WAV demo track. |
| `admin-service/src/main/resources/static/assets/demo-3.wav` | Original synthesized WAV demo track. |
| `admin-service/src/main/resources/static/assets/demo-4.wav` | Original synthesized WAV demo track. |
| `admin-service/src/main/resources/static/assets/demo-5.wav` | Original synthesized WAV demo track. |
| `admin-service/src/main/resources/static/index.html` | Frontend structure, dialogs, forms and music player. |
| `admin-service/src/main/resources/static/styles.css` | Responsive dark/orange visual design, layouts and interaction styles. |
| `API.md` | Endpoint and request-body reference. |
| `ASSETS.md` | Description and provenance of the included demo media. |
| `common/pom.xml` | Maven dependencies/build configuration; the root POM also lists all modules. |
| `common/src/main/java/com/ansh/musiclibrary/common/Errors.java` | Maps validation and application failures to readable API responses. |
| `common/src/main/java/com/ansh/musiclibrary/common/SecurityConfig.java` | JWT validation, role rules, internal service authentication and HTTP clients. |
| `common/src/main/java/com/ansh/musiclibrary/common/SessionVerifier.java` | Contract for checking whether a login session is still active. |
| `common/src/main/java/com/ansh/musiclibrary/common/TomcatConfig.java` | Configures Tomcat’s supported NIO2 connector for local Windows compatibility. |
| `discovery-server/pom.xml` | Maven dependencies/build configuration; the root POM also lists all modules. |
| `discovery-server/src/main/java/com/ansh/musiclibrary/discoveryserver/Application.java` | Spring Boot entry point; enables this service and its component scanning. |
| `discovery-server/src/main/java/com/ansh/musiclibrary/discoveryserver/TomcatConfig.java` | Configures Tomcat’s supported NIO2 connector for local Windows compatibility. |
| `discovery-server/src/main/resources/application.properties` | Service port, database, discovery and environment-based settings. |
| `FILE-GUIDE.md` | This source-file map. |
| `mvnw` | Maven Wrapper for Unix shells; supplied Windows launchers are the supported quick start. |
| `mvnw.cmd` | Maven Wrapper for Windows; downloads the pinned Maven version. |
| `notification-service/pom.xml` | Maven dependencies/build configuration; the root POM also lists all modules. |
| `notification-service/src/main/java/com/ansh/musiclibrary/notificationservice/Application.java` | Spring Boot entry point; enables this service and its component scanning. |
| `notification-service/src/main/java/com/ansh/musiclibrary/notificationservice/EmailWorker.java` | Optional SMTP delivery with bounded retries. |
| `notification-service/src/main/java/com/ansh/musiclibrary/notificationservice/Notice.java` | Per-user in-app notification and email delivery state. |
| `notification-service/src/main/java/com/ansh/musiclibrary/notificationservice/NoticeController.java` | Idempotent event intake, recipient lookup, own notifications and read status. |
| `notification-service/src/main/java/com/ansh/musiclibrary/notificationservice/NoticeRepository.java` | Database queries for notifications. |
| `notification-service/src/main/resources/application.properties` | Service port, database, discovery and environment-based settings. |
| `pom.xml` | Maven dependencies/build configuration; the root POM also lists all modules. |
| `README.md` | Setup, architecture, features, email configuration and limitations. |
| `scripts/Start.ps1` | Creates private local settings, builds, starts and health-checks the services. |
| `scripts/Stop.ps1` | Stops recorded Java processes only when PID and start timestamp match. |
| `scripts/Verify.ps1` | 33 API integration checks; creates two test accounts and removes its test songs/playlists. |
| `song-service/pom.xml` | Maven dependencies/build configuration; the root POM also lists all modules. |
| `song-service/src/main/java/com/ansh/musiclibrary/songservice/Application.java` | Spring Boot entry point; enables this service and its component scanning. |
| `song-service/src/main/java/com/ansh/musiclibrary/songservice/DemoLibrary.java` | Seeds six original demonstration tracks into an empty song database. |
| `song-service/src/main/java/com/ansh/musiclibrary/songservice/EventRepository.java` | Database queries for pending song events. |
| `song-service/src/main/java/com/ansh/musiclibrary/songservice/Outbox.java` | Retries delivery of song events to the notification service. |
| `song-service/src/main/java/com/ansh/musiclibrary/songservice/Song.java` | Song metadata entity including visibility, genre and duration. |
| `song-service/src/main/java/com/ansh/musiclibrary/songservice/SongController.java` | Search, public-to-authenticated-user reads and admin-only song management. |
| `song-service/src/main/java/com/ansh/musiclibrary/songservice/SongEvent.java` | Durable pending notification event stored with a song change. |
| `song-service/src/main/java/com/ansh/musiclibrary/songservice/SongRepository.java` | Database queries for song metadata. |
| `song-service/src/main/resources/application.properties` | Service port, database, discovery and environment-based settings. |
| `Start.cmd` | Double-click entry point for starting the project on Windows. |
| `Stop.cmd` | Double-click entry point for stopping this project’s recorded processes. |
| `user-service/pom.xml` | Maven dependencies/build configuration; the root POM also lists all modules. |
| `user-service/src/main/java/com/ansh/musiclibrary/userservice/Account.java` | Database entity for a user account, hashed password and notification preference. |
| `user-service/src/main/java/com/ansh/musiclibrary/userservice/AccountRepository.java` | Database queries for accounts. |
| `user-service/src/main/java/com/ansh/musiclibrary/userservice/Application.java` | Spring Boot entry point; enables this service and its component scanning. |
| `user-service/src/main/java/com/ansh/musiclibrary/userservice/AuthController.java` | Registration, login, logout, current profile, session checks and admin bootstrap. |
| `user-service/src/main/java/com/ansh/musiclibrary/userservice/LoginSession.java` | Persistent login session used for expiration and logout revocation. |
| `user-service/src/main/java/com/ansh/musiclibrary/userservice/Playlist.java` | User-owned playlist and its ordered song IDs. |
| `user-service/src/main/java/com/ansh/musiclibrary/userservice/PlaylistController.java` | Playlist operations with ownership and song-visibility checks. |
| `user-service/src/main/java/com/ansh/musiclibrary/userservice/PlaylistRepository.java` | Database queries for playlists. |
| `user-service/src/main/java/com/ansh/musiclibrary/userservice/SessionRepository.java` | Database queries for login sessions. |
| `user-service/src/main/resources/application.properties` | Service port, database, discovery and environment-based settings. |
| `VERIFICATION.md` | Actual delivery checks and remaining manual checks. |

## Where to start reading

1. Root README and API reference.
2. Song entity, repository and controller — these extend what you already built.
3. User authentication and playlist controllers.
4. Shared security configuration and gateway.
5. Frontend HTML, CSS and JavaScript.
6. Notification outbox and email worker.

Generated after startup: `.local/` contains private settings and admin credentials, `data/` contains persistent databases, `logs/` contains service logs, and each module’s `target/` contains build outputs. These folders are intentionally absent from the ZIP.
