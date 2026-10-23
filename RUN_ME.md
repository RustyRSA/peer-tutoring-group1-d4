# Run PeerTutoring (login via Node + Firebase)

## One-time Firebase console setup (project peertutoringplatform-92ae9)
1. Build > Authentication > Get started > Sign-in method > enable **Email/Password**.
2. Build > Firestore Database > **Create database** (test mode is fine for now).
3. Project settings > General > Your apps > **Add app > Android**, package name `com.example.peertutoring`.
   Download **google-services.json** and put it in `app/` (next to `app/build.gradle.kts`).

## Backend
```
cd backend
npm install
npm start        # should print: Peer Tutoring API running on http://localhost:3000
```
Check http://localhost:3000/health returns {"status":"ok"}.

## Android
Open this folder in Android Studio, let Gradle sync, pick an emulator, press Run.
The app reaches the backend at http://10.0.2.2:3000/.

## Logging in
.env sets UNIVERSITY_DOMAIN=@vossie.net, so register with an email like `test@vossie.net`.
(To allow any email, clear UNIVERSITY_DOMAIN in backend/.env and UNIVERSITY_EMAIL_DOMAIN in ApiConfig.java.)
