SETUP
1. install nodes.js 18
2. firebase -> project settings -> service accounts -> generate new private key
save it this main folder as "serviceAccount.json"
3. copy .env.example to .env
4. run npm install 
npm start

FILES
src
	index.js - sets up Express, the routes and the error handler, then starts the server
	firebase.js - onnects to Firebase with the key; exports db (Firestore) and auth
	middleware/auth.js - checks the login token, loads the profile, blocks suspended users
	routes/users.js - all the /users routes
	utils/validate.js -checks what the app sends
	utils/profiles.js  - what's shown to the owner/admins vs. everyone else
	
ROUTES
All routes expect /health needs Authorization: bearer firebase token

METHOD	PATH										WHO 				WHAT IT DOES

POST 	/users										logged in once		create my profile after singing up
GET 	/users/me 									me 					my full prof
PUT  	/users/me 									me					update my profile 
DELETE 	users/me 									me 					delete my profie	
GET 	users/tutors?subject=&q=					anyone				Approved tutors, best ratedd first
GET 	/users/:uid									anyone				someones public profie (admin see everything) 					
GET 	users?role=&accountStatus=&tutorStatus=		admin				List users newest first
PUT  	/users/:uid/status							admin				{"accountStatus": "suspended"/"active" } and/or { "tutorStatus": "approved"/"rejected"}

Errors always look like this, so the app can show 'error' in a Toast and each
'fields' message under its input:


{ "error": "Some fields are missing or invalid.", "code": "VALIDATION_ERROR",
  "fields": { "yearOfStudy": "Must be a whole number from 1 to 6." } }

RULES
- Each user is a document in the 'users' collection, with the document name being their Firebase uid.
- Students and Tutors are the same account type. You are 'pending' if you list subjects you can tutor so that you only show up in tutor search once an admin approves you. Changing your subjects or credentials will bring you back to 'pending'.- Users cannot set their own email, role (other than student/tutor at sign-up), tutorStatus, accountStatus or rating.
- The role and suspension are queried from Firestore on each request, so they are instantly active.
- Your email and what you need help with is never seen by other users.
- To create an admin, create the profile, then in the Firebase console, go to Firestore → 'users' → that document and set 'role' to 'admin'.

POSTMAN TESTING
1. Get a toke