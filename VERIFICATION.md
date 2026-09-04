# Delivery verification — 3 September 2026

## Completed

- Maven packaged all six modules and produced five executable service JARs with Java 25.0.3.
- All five services started locally and reported healthy: Eureka 8761, users 8081, songs 8082, notifications 8083 and frontend/gateway 8090.
- Eureka registration and service-to-service HTTP requests worked.
- All **33 checks** in `scripts/Verify.ps1` passed: registration, admin login, anonymous/role rejection, wrong passwords, input validation, song create/read/search/update/delete, empty search, playlist ownership and duplicates, notification creation/ownership/read status, hidden-song restrictions and server-side logout revocation.
- The Windows PowerShell Stop launcher stopped all five recorded project processes and retained database files.
- JavaScript syntax was checked with Node. All demo assets and source files are included in the archive.
- The ZIP excludes generated credentials, local databases, test-account data, logs and compiled output.

## Limits of verification

Browser rendering, keyboard interaction, responsive appearance and audible playback have not been manually exercised in a real browser in this workspace. They should be checked after startup on your computer. Actual SMTP email delivery is not tested or enabled; in-app notification delivery is tested.

This workspace’s Windows sandbox triggered an access-denied exception in javac’s dependency-ZIP cleanup. Packaging succeeded using Maven’s forked-compiler option; the resulting JARs were used for the runtime checks above. The ordinary launcher uses the standard Maven build on your machine. No unit-test suite is claimed: the verification script exercises running APIs.

## Repeat the API checks

Start the project, then run from the extracted project folder:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Verify.ps1
```

The script creates two test accounts and their historical notifications. It removes its temporary songs and playlist after a successful run. If it fails midway, test data may remain. Use this only against your local demonstration instance.

## Browser check after startup

Register an account, play one of the six demo tracks, test seek/volume/shuffle/repeat, make a playlist, and resize to a narrow window. Sign out and sign in as admin to edit a song and toggle its visibility. Then sign back in as a user and check the library and notification panel.
