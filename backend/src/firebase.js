const path = require("path");
const
{
    initializeApp,
    cert
} = require("firebase-admin/app");
const
{
    getFirestore
} = require("firebase-admin/firestore");
const
{
    getAuth
} = require("firebase-admin/auth");

// Path to the service account key (see .env.example)
const keyPath = path.resolve(process.env.FIREBASE_KEY_PATH || "./serviceAccountKey.json");
const serviceAccountKey = require(keyPath);

initializeApp(
{
    credential: cert(serviceAccountKey),
});

const db = getFirestore();
const auth = getAuth();

module.exports = {
    db,
    auth
};
