const express = require("express");
const
{
    db
} = require("../firebase");
const
{
    checkToken,
    requireProfile
} = require("../middleware/auth");
const
{
    timeKey
} = require("../utils/time");

const router = express.Router();
const notificationsCollection = db.collection("notifications");

const MAX_LIST_LENGTH = 100;

// every route needs a valid login token and a profile
router.use(checkToken, requireProfile);

// GET /notifications - my notifications, newest first
router.get("/", async (req, res, next) =>
{
    try
    {
        const results = await notificationsCollection.where("userId", "==", req.user.uid).get();
        const list = results.docs
            .map((notificationDoc) => (
            {
                id: notificationDoc.id,
                ...notificationDoc.data()
            }))
            .sort((first, second) => timeKey(second.createdAt).localeCompare(timeKey(first.createdAt)))
            .slice(0, MAX_LIST_LENGTH);
        res.json(list);
    }
    catch (err)
    {
        next(err);
    }
});

// PUT /notifications/read-all - mark all my unread notifications as read
router.put("/read-all", async (req, res, next) =>
{
    try
    {
        const results = await notificationsCollection.where("userId", "==", req.user.uid).get();
        const unread = results.docs.filter((notificationDoc) => notificationDoc.data().read !== true);
        await Promise.all(unread.map((notificationDoc) => notificationsCollection.doc(notificationDoc.id).update(
        {
            read: true
        })));
        res.status(204).send();
    }
    catch (err)
    {
        next(err);
    }
});

// PUT /notifications/:id/read - mark one of my notifications as read
router.put("/:id/read", async (req, res, next) =>
{
    try
    {
        const ref = notificationsCollection.doc(req.params.id);
        const notificationDoc = await ref.get();
        // someone else's notification looks the same as one that does not exist
        if (!notificationDoc.exists || notificationDoc.data().userId !== req.user.uid)
        {
            return res.status(404).json(
            {
                error: "Notification not found.",
                code: "NOT_FOUND"
            });
        }
        await ref.update(
        {
            read: true
        });
        res.status(204).send();
    }
    catch (err)
    {
        next(err);
    }
});

module.exports = router;
