const
{
    db
} = require("../firebase");

const notificationsCollection = db.collection("notifications");

// Leaves a notification for one user. A failure here is logged but never breaks the request that caused it.
async function notify(userId, type, title, body)
{
    try
    {
        await notificationsCollection.add(
        {
            userId,
            type,
            title,
            body,
            read: false,
            createdAt: new Date().toISOString()
        });
    }
    catch (err)
    {
        console.error("Could not create notification:", err);
    }
}

module.exports = {
    notify
};
