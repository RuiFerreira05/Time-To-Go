# Assignment TP1 — Time to Go
Course: Desenvolvimento de Aplicações Móveis (DAM)
Student(s): Rui Ferreira (a51597)
Date: March 6th, 2026
Repository URL: https://github.com/RuiFerreira05/Time-To-Go.git
---
## 1. Introduction
The objective of this assignment, fulfilling the "Assisted code generation - MIP" module, was to develop a full-fledged native Android practical application utilizing AI tools. "Time to Go" is an automated bus route notification application that ensures the user never misses their bus home again. It automatically determines the user's current location, computes the best bus route to their saved home address, and sends an actionable notification at a configured time each day.

## 2. System Overview
"Time to Go" is a reliable background-oriented utility app featuring:
- **Google Sign-In**: Secure authentication using the Credential Manager.
- **Home Address Autocomplete**: Interactive searching powered by the Google Places SDK.
- **Configurable Daily Alarms**: User-defined notification times to trigger the route checks.
- **Transit Route Notifications**: Delivers bus numbers, departure time estimates, and walking directions directly into the notification drawer.
- **Google Maps Integration**: Tapping notifications launches transit directions via intents.

## 3. Architecture and Design
The application targets Android 14 (API 34) and adopts modern Android development patterns:
- **MVVM Architecture**: Utilizing ViewModels and StateFlow to maintain UI states separated from logic.
- **Single Activity**: Employs the Navigation Component for routing between three core fragments.
- **Dependency Injection**: Outlined fully with Hilt to decouple components.
- **Data Persistence**: Uses DataStore to securely and asynchronously store local configurations and route caches.
- **Background Processes**: WorkManager handles the route fetching in the background, reliably scheduled via AlarmManager.

## 4. Implementation
Crucial technical details include:
- **Routes & Places API**: Interaction with Google Maps Platform services natively. To minimize API costs, it implements route caching with timestamp invalidation.
- **Permissions**: Fine-grained handling for `ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, and `FOREGROUND_SERVICE_LOCATION` to guarantee smooth background processing.

## 5. Testing and Validation
- **Unit Tests**: Mockito was used to implement extensive JVM unit tests covering `AlarmScheduler`, `AlarmReceiver`, `NotificationHelper`, `RouteFetchWorker`, `BootReceiver`, `LocationHelper`, and `PermissionHelper`.
- **Manual UI Validation**: Verified screen jumping and insets rendering on the Map Selection screen during keyboard appearance. 

## 6. Usage Instructions
1. Clone the repository: `git clone https://github.com/RuiFerreira05/Time-To-Go.git`
2. Create a `local.properties` file in the root directory.
3. Configure `local.properties` with properties:
    ```properties
    MAPS_API_KEY=your_google_maps_api_key_here
    GOOGLE_WEB_CLIENT_ID=your_web_client_id_here
    ```
4. Build the app targeting API level 34 or higher (`./gradlew assembleDebug`).

---
# Autonomous Software Engineering Sections
## 7. Prompting Strategy
The project was driven iteratively via Google Antigravity using dynamic prompts combining Context, Goal, Constraints, and Verification criteria via the Agent Manager.
- **Initial Generation**: Provided comprehensive requirements detailing the APIs (Places SDK, Routes API), the MVVM constraints, and background scheduling preferences.
- **Specialized Conversations**: Focused sessions were generated for isolated bug fixes and enhancements:
	- *UI Redesign: Orange/Black*: Directed the agent to apply a sleek color/theme overhaul.
    - *Fixing Background Notifications*: Provided logs and constraints strictly about background location issues and `ACCESS_BACKGROUND_LOCATION` manifest requirements.
    - *Refining Map Selection UI*: Detailed the UI occlusion and "jumping" problems with precise instructions to adjust the status bar insets.

## 8. Autonomous Agent Workflow
The Google Antigravity agent was heavily involved in every major step:
1. **Planning**: Created a structured task list outlining UI elements, authentication, and routing integration.
2. **Coding**: Generated the foundational implementation, refactored standard configurations (Settings, Maps selection) directly adjusting XML layout parameters.
3. **Debugging**: When Material Dialog bugs occurred, the agent investigated cancel button state issues and autonomously resolved the double-click dismissal bug.
4. **Testing**: Upon analyzing coverage, the agent modified classes (e.g., `DirectionsRepository`, `LocationHelper`) to be `open` as required by Mockito testing and produced complete test suites.

## 9. Verification of AI-Generated Artifacts
Verification involved rigorous manual code reviews and IDE inspections.
- **Debugging Feedback**: When the agent initially skipped tests or failed background executions, manual executions were run on logcat and test runners. The error traces were fed back to the agent to enforce exact solutions.

## 10. Human vs AI Contribution
- **AI-Assisted**: Approximately 90-95% of the codebase, including scaffolding, business logic, Hilt infrastructure, Maps integration, unit test algorithms, layout designs, UI/UX aesthetics, and documentation formatting.
- **Human-Developed**: Architecture concept modeling, API Cloud Console preparation/key provisioning, prompt engineering, project oversight, manual runtime testing, issue diagnosis, and overall validation direction.

## 11. Ethical and Responsible Use
Risks involved API key handling and privacy with location data.
- The agent was deliberately instructed to isolate API key loading into `local.properties` (hidden from git).
- Background location extraction policies were constrained and ensured standard OS fallback requests, maintaining strict privacy considerations.

---
# Development Process
## 12. Version Control and Commit History
The commit history is robust and highly descriptive, largely driven by automated prefix standards (e.g., `(AGENT) feat: ...`). It showcases incremental progression:
- Establishing initial architecture.
- Modular commits for fixing the dialog components, redesigning the app icon, optimizing map selection, fixing the notification behaviors, and writing unit test coverage.

## 13. Difficulties and Lessons Learned
- **Background Location Policies**: Understanding the complexities behind Android 14's strict background execution and location tracking policies, requiring the exact choreography of `Foreground Services` and Alarm managers.
- **Agent Orchestration**: Learning how to efficiently partition large problems (e.g., whole app features) into granular, testable prompts (e.g., exclusively focusing on Map Selection bugs in one conversation window).

## 14. Future Improvements
- Implement offline fallback routing if the Google API limit is breached or the network is unavailable.
- Introduce advanced caching strategies utilizing a robust Room Database instead of relying purely on DataStore for complex route objects.
---
## 15. AI Usage Disclosure (Mandatory)
- **Google Antigravity**: Used exclusively as the primary autonomous agent throughout the software development lifecycle to write code, diagnose emulator errors, design layouts, format icons, generate unit tests, and formulate this very documentation. The user remained responsible for verifying functional requirements, ensuring security token safety, and compiling the outputs successfully.
