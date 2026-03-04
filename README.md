# Time to Go 🚌

**Automated Bus Route Notification Android App**

Never miss your bus home again. Time to Go automatically determines your current location, finds the best bus route to your home address, and sends you a notification at your configured time each day.

## Features

- 🔐 **Google Sign-In** — Secure authentication via Credential Manager
- 📍 **Home Address Autocomplete** — Powered by Google Places SDK
- ⏰ **Configurable Daily Alarm** — Set your preferred notification time
- 🚌 **Transit Route Notifications** — Bus numbers, departure times, walking directions
- 🗺️ **Google Maps Integration** — Tap notification to open transit directions
- 🔄 **Recurring or One-shot Mode** — Daily notifications or just once
- 📝 **Brief or Detailed Display** — Choose your notification format

## Setup

### Prerequisites

- Android Studio (latest stable version)
- JDK 17
- A Google Cloud Platform project with billing enabled

### 1. API Keys

This app requires three Google APIs enabled in your Google Cloud project:

1. **Maps SDK for Android**
2. **Routes API** (NOT the legacy Directions API)
3. **Places API (New)**

#### Steps:
1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the three APIs listed above
4. Go to **APIs & Services → Credentials**
5. Create an **API Key** (restrict it to the three APIs above)
6. Copy the API key

### 2. Google Sign-In (OAuth)

1. In Google Cloud Console, go to **APIs & Services → OAuth consent screen**
2. Configure the consent screen (External or Internal based on your needs)
3. Go to **APIs & Services → Credentials**
4. Create an **OAuth 2.0 Client ID**:
   - Application type: **Android**
   - Package name: `com.timetogo.app`
   - SHA-1 certificate fingerprint: Get this by running:
     ```powershell
     # Debug key (Windows PowerShell):
     keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
     ```
5. Create another **OAuth 2.0 Client ID**:
   - Application type: **Web application**
   - Note the **Web Client ID** — this is used in `local.properties`

### 3. Configure local.properties

Edit `local.properties` in the project root (create it if it doesn't exist):

```properties
MAPS_API_KEY=your_google_maps_api_key_here
GOOGLE_WEB_CLIENT_ID=your_web_client_id_here
```

> ⚠️ **NEVER** commit `local.properties` to version control.

### 4. Build & Run

```bash
# Open in Android Studio and sync Gradle, or:
./gradlew assembleDebug

# Install on connected device:
./gradlew installDebug
```

The app requires **Android 14 (API 34)** or higher.

## Architecture

- **MVVM** with ViewModels and StateFlow
- **Single Activity** with Navigation Component (3 fragments)
- **Hilt** for dependency injection
- **DataStore** for preferences persistence
- **WorkManager** for background route fetching
- **AlarmManager** for precise scheduling

## API Cost Awareness

This app is designed for the **Google Maps Platform free tier**:

- **One API call per alarm trigger** — no polling, no retries (except one manual retry)
- **Route caching** — fetched routes are cached locally with timestamps
- **No API calls on app launch** — only when the alarm fires
- **Minimal response fields** — field mask limits response to transit-relevant data only

## Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | API calls |
| `ACCESS_FINE_LOCATION` | Determine current position for route calculation |
| `ACCESS_COARSE_LOCATION` | Fallback location |
| `POST_NOTIFICATIONS` | Display route notifications |
| `SCHEDULE_EXACT_ALARM` | Schedule daily alarm at exact time |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule alarm after device reboot |
| `FOREGROUND_SERVICE` | Background route fetching |
| `FOREGROUND_SERVICE_LOCATION` | Location access during background work |

## Known Limitations

1. The Routes API transit mode may not be available in all regions
2. Bus route preferences are best-effort — the API may return mixed transit modes
3. The app requires Google Maps to be installed for the notification tap action
4. `SCHEDULE_EXACT_ALARM` must be manually granted by the user on Android 14+
5. Route cache is stored as JSON in DataStore (not a full database)

## Disclaimer

> This project was entirely designed, developed, and documented with the assistance of artificial intelligence. All source code, architecture decisions, UI design, and documentation — including this README — were generated through AI-driven development. Human oversight was provided for review, guidance, and final approval.

## License

This project is for educational purposes.
