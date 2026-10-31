# PlantDex backend

A small Flask API that sits between the Android app and SQL Server. The
Android app never talks to the database directly — it only calls this API
over HTTP, and this API is the only thing with SQL Server credentials.

## One-time setup

1. **Install Docker Desktop** (docker.com/products/docker-desktop) and open it
   once so its background service is running.

2. **Start the database and API:**

   ```bash
   cd backend
   docker compose up -d --build
   ```

   This starts two containers: `db` (SQL Server 2022) on port 1433, and `api`
   (this Flask app) on port 5000. First run takes a few minutes while the SQL
   Server image downloads and Docker builds the API image.

3. **Create the database schema.** Open SSMS and connect to:
   - Server name: `localhost,1433`
   - Authentication: SQL Server Authentication
   - Login: `sa`
   - Password: `PlantDex!2026` (from `docker-compose.yml` — change it there
     for anything beyond local dev, since it's checked into git)

   Open `schema.sql` from this folder in SSMS and execute it (F5). This
   creates the `PlantDexDB` database with `Users` and `AuditLog` tables.

4. **Check the API is up:**

   ```bash
   curl http://localhost:5000/api/audit-log
   # {"entries": []}
   ```

## Running the Android app against this

The Android app is already pointed at `http://10.0.2.2:5000` in
`ApiClient.java` — that's the special address the Android **emulator** uses
to reach `localhost` on this Mac, so no changes are needed if you're running
the app in the emulator while these containers are up.

If you run the app on a **physical device** instead, it can't resolve
`10.0.2.2` — change `BASE_URL` in
`app/src/main/java/com/example/plantdex/common/ApiClient.java` to this Mac's
LAN IP address (e.g. `http://192.168.1.23:5000`), and make sure the device is
on the same Wi-Fi network.

## Day-to-day use

```bash
docker compose up -d      # start both containers (after the first build)
docker compose down       # stop them
docker compose logs -f api  # tail the Flask logs
```

Data persists in a Docker volume across restarts. To wipe it and start over:
`docker compose down -v`.

## Endpoints

| Method | Path                | Body                                              |
|--------|---------------------|----------------------------------------------------|
| POST   | `/api/auth/register` | `email, fullName, username, password, dob`        |
| POST   | `/api/auth/login`    | `username, password, role`                        |
| POST   | `/api/auth/logout`   | `username, role`                                   |
| GET    | `/api/audit-log`     | —  (returns the most recent 50 entries)            |
