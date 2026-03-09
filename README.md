# SkillHub LMS – Backend

Spring Boot 3 REST API. Requires Java 17+ and MySQL.

## Environment variables

| Variable | Description |
|----------|-------------|
| SPRING_DATASOURCE_URL | JDBC URL (e.g. `jdbc:mysql://host:port/defaultdb?ssl-mode=REQUIRED`) |
| SPRING_DATASOURCE_USERNAME | DB user |
| SPRING_DATASOURCE_PASSWORD | DB password |
| JWT_SECRET | Secret for JWT signing (min 32 chars in production) |
| CORS_ORIGINS | Allowed origins, comma-separated (e.g. `http://localhost:5173`) |
| STRIPE_SECRET_KEY | Stripe secret key (paid courses) |
| STRIPE_WEBHOOK_SECRET | Stripe webhook signing secret |
| YOUTUBE_API_KEY | YouTube Data API v3 key (for seed script only) |

## Run

**Using Aiven MySQL (default URL is preconfigured):** you must provide the database password. Use either option below.

### Option 1: Local config file (recommended — password always loaded)

1. Copy the example file:
   ```powershell
   cd src\main\resources
   copy application-local.yml.example application-local.yml
   ```
2. Open `application-local.yml` and replace `REPLACE_WITH_YOUR_AIVEN_PASSWORD` with your actual Aiven password.
3. From the backend folder run:
   ```powershell
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```
   The app listens on **port 8082** by default with the local profile (to avoid conflicts with 8080/8081). The frontend dev server is configured to proxy `/api` to `http://localhost:8082`.

`application-local.yml` is in `.gitignore` — do not commit it.

**If you see "port 8082 already in use":** stop the existing backend (close the terminal where it’s running, or run `netstat -ano | findstr :8082` then `taskkill /PID <pid> /F`). Or set a different port in `application-local.yml` (e.g. `server.port: 8083`) and update the frontend proxy in `frontend/vite.config.js` to match.

### Option 2: Environment variable

Set the password in the **same** terminal session, then run Maven:

```powershell
# PowerShell (Windows) — run both lines in the same window
$env:SPRING_DATASOURCE_PASSWORD="your_actual_aiven_password"
mvn spring-boot:run
```

If you see `Access denied ... (using password: NO)`, the JVM did not receive the password (e.g. different terminal or IDE). Use **Option 1** instead.

## Populating courses from YouTube playlists

The app can create courses from YouTube playlists (Java DSA, DevOps, Python, C++, Computer Graphics, ANN, HTML/CSS/JS, etc.). Each playlist becomes one course; each video in the playlist becomes a lesson.

### Option A: Automatic on startup (recommended)

1. Get a [YouTube Data API v3](https://console.cloud.google.com/apis/credentials) key: Create credentials → API key. In [APIs & Services](https://console.cloud.google.com/apis/library) enable **YouTube Data API v3** for your project.
2. Set the key in `application-local.yml` (copy from `application-local.yml.example`). Do not commit the key; use `application-local.yml` (in `.gitignore`) or the `YOUTUBE_API_KEY` environment variable. See [Google API key best practices](https://docs.cloud.google.com/docs/authentication/api-keys-best-practices).
   ```yaml
   app:
     youtube:
       api-key: YOUR_YOUTUBE_DATA_API_KEY
   ```
   Or set the environment variable `YOUTUBE_API_KEY`.
3. Run the app with the `local` profile (as usual):
   ```powershell
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```
   On startup, if the key is set, the app will fetch all configured playlists and create one course per playlist (with all videos as lessons). If no key is set, only two sample courses are created.

Playlist IDs are in `application.yml` under `app.seed.playlistIds`. You can add or remove IDs there.

### Option B: Seed profile only

To run the YouTube import without starting the full app for normal use (e.g. one-off seed):

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=seed,local
```

**Note:** A one-time fix on startup replaces any known broken video URLs in lessons (and course thumbnails) with working ones.
