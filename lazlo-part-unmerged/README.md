# Lazlo's part: how each piece was handled

Source: PeerTutoring-Lazlo-Part.zip (an older, static-mockup version of the app).
Where main already had an equivalent, the better one was kept. The originals are stored here
as .txt files (outside src/) for reference only.

| Lazlo's piece | Main's equivalent | Decision |
|---|---|---|
| Per-screen bottom-nav, back-button and notification wiring | `utils/NavHelper.setup()` (one call, role-aware, login check) | Main kept |
| Login/Register buttons that just open the next screen | Real Firebase login/register via `AuthRepository` | Main kept |
| Tutor/Session lists with hardcoded dummy cards | `TutorAdapter`, `SessionAdapter` fed by the backend | Main kept |
| `RecyclerViewAdapter.java` (stub, no imports) | `adapters/TutorAdapter.java` | Main kept |
| `Session.java`, `Tutor.java` models | `TutorSession.java`, `UserProfile.java` | Main kept (Lazlo's removed) |
| `Firestore.java` (JavaScript inside Java, hard-coded API key) | `google-services.json` + backend Firebase Admin SDK | Main kept |
| Booking saved straight to Firestore (snippet sat outside any method) | `BookingRequestActivity` -> backend `POST /sessions` with validation and notifications | Main kept |
| Admin analytics bar chart (fake fixed-height bars) | Main only had number tiles | **Ported**, now driven by real counts |
| `.gitignore` Android entries | Main's `.gitignore` had paths that did not match this layout | **Merged** (union) |
| "Availability" filter chip (no logic behind it) | none; the data has no availability field | Not ported (needs backend + profile fields) |
| Gradle: AGP 9.4.1, compileSdk 37, Firebase commented out | AGP 8.7.3, compileSdk 35, Firebase enabled | Main kept (Firebase is needed) |
