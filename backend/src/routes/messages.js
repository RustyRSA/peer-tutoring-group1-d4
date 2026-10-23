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
    isObject
} = require("../utils/validate");
const
{
    fullName
} = require("../utils/profiles");

const router = express.Router();
const messagesCollection = db.collection("messages");
const usersCollection = db.collection("users");

const MAX_TEXT_LENGTH = 1000;
const MAX_THREAD_LENGTH = 200;

// the same id for both people, no matter who is the sender
function conversationKey(firstUid, secondUid)
{
    return [firstUid, secondUid].sort().join("_");
}

// every route needs a valid login token and a profile
router.use(checkToken, requireProfile);

// POST /messages - send a message to another user
router.post("/", async (req, res, next) =>
{
    try
    {
        const body = isObject(req.body) ? req.body :
        {};
        const errors = {};
        if (typeof body.recipientId !== "string" || body.recipientId.trim() === "") errors.recipientId = "This field is required.";
        if (typeof body.text !== "string" || body.text.trim() === "" || body.text.trim().length > MAX_TEXT_LENGTH)
        {
            errors.text = `Required, at most ${MAX_TEXT_LENGTH} characters.`;
        }
        if (Object.keys(errors).length > 0)
        {
            return res.status(400).json(
            {
                error: "Some fields are missing or invalid.",
                code: "VALIDATION_ERROR",
                fields: errors
            });
        }

        const recipientId = body.recipientId.trim();
        if (recipientId === req.user.uid)
        {
            return res.status(400).json(
            {
                error: "You cannot message yourself.",
                code: "VALIDATION_ERROR"
            });
        }

        const recipientDoc = await usersCollection.doc(recipientId).get();
        const recipient = recipientDoc.exists ? recipientDoc.data() : null;
        if (!recipient || recipient.accountStatus !== "active")
        {
            return res.status(404).json(
            {
                error: "That user is not available.",
                code: "RECIPIENT_NOT_FOUND"
            });
        }

        const message = {
            conversationId: conversationKey(req.user.uid, recipientId),
            participants: [req.user.uid, recipientId],
            senderId: req.user.uid,
            senderName: fullName(req.user.profile),
            recipientId,
            recipientName: fullName(recipient),
            text: body.text.trim(),
            createdAt: new Date().toISOString()
        };
        const ref = await messagesCollection.add(message);
        res.status(201).json(
        {
            id: ref.id,
            ...message
        });
    }
    catch (err)
    {
        next(err);
    }
});

// GET /messages - my conversations, latest first (one row per person I have talked to)
router.get("/", async (req, res, next) =>
{
    try
    {
        const uid = req.user.uid;
        const results = await messagesCollection.where("participants", "array-contains", uid).get();

        const latestByConversation = new Map();
        for (const messageDoc of results.docs)
        {
            const message = messageDoc.data();
            const current = latestByConversation.get(message.conversationId);
            if (!current || (message.createdAt || "") > (current.createdAt || ""))
            {
                latestByConversation.set(message.conversationId, message);
            }
        }

        const conversations = [...latestByConversation.values()].map((message) =>
        {
            const iAmSender = message.senderId === uid;
            return {
                conversationId: message.conversationId,
                otherUid: iAmSender ? message.recipientId : message.senderId,
                otherName: iAmSender ? message.recipientName : message.senderName,
                lastMessage: message.text,
                lastSenderId: message.senderId,
                lastAt: message.createdAt
            };
        });
        conversations.sort((first, second) => (second.lastAt || "").localeCompare(first.lastAt || ""));
        res.json(conversations);
    }
    catch (err)
    {
        next(err);
    }
});

// GET /messages/:otherUid - my conversation with one person, oldest first
router.get("/:otherUid", async (req, res, next) =>
{
    try
    {
        const key = conversationKey(req.user.uid, req.params.otherUid);
        const results = await messagesCollection.where("conversationId", "==", key).get();

        const thread = results.docs
            .map((messageDoc) => (
            {
                id: messageDoc.id,
                ...messageDoc.data()
            }))
            .sort((first, second) => (first.createdAt || "").localeCompare(second.createdAt || ""))
            .slice(-MAX_THREAD_LENGTH);
        res.json(thread);
    }
    catch (err)
    {
        next(err);
    }
});

module.exports = router;
