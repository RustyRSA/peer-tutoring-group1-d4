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
const
{
    notify
} = require("../utils/notifications");
const
{
    timeKey
} = require("../utils/time");

const router = express.Router();
const ratingsCollection = db.collection("ratings");
const sessionsCollection = db.collection("sessions");
const usersCollection = db.collection("users");

const MAX_COMMENT_LENGTH = 500;
const MAX_REVIEWS = 20;

// every route needs a valid login token and a profile
router.use(checkToken, requireProfile);

// POST /ratings - a student rates the tutor of one completed session (once per session)
router.post("/", async (req, res, next) =>
{
    try
    {
        const body = isObject(req.body) ? req.body :
        {};
        const errors = {};
        if (typeof body.sessionId !== "string" || body.sessionId.trim() === "") errors.sessionId = "This field is required.";
        if (!Number.isInteger(body.stars) || body.stars < 1 || body.stars > 5) errors.stars = "Must be a whole number from 1 to 5.";
        if (body.comment !== undefined && body.comment !== null &&
            (typeof body.comment !== "string" || body.comment.trim().length > MAX_COMMENT_LENGTH))
        {
            errors.comment = `Must be text of at most ${MAX_COMMENT_LENGTH} characters.`;
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

        const sessionId = body.sessionId.trim();
        const sessionRef = sessionsCollection.doc(sessionId);
        const sessionDoc = await sessionRef.get();
        // someone else's session looks the same as one that does not exist
        if (!sessionDoc.exists || sessionDoc.data().studentId !== req.user.uid)
        {
            return res.status(404).json(
            {
                error: "Session not found.",
                code: "NOT_FOUND"
            });
        }
        const session = sessionDoc.data();
        if (session.status !== "completed")
        {
            return res.status(409).json(
            {
                error: "You can only rate a session after it is completed.",
                code: "SESSION_NOT_COMPLETED"
            });
        }

        const rating = {
            sessionId,
            tutorId: session.tutorId,
            studentId: req.user.uid,
            studentName: fullName(req.user.profile),
            stars: body.stars,
            comment: typeof body.comment === "string" && body.comment.trim() !== "" ? body.comment.trim() : null,
            createdAt: new Date().toISOString()
        };

        // one rating per session: the rating document id is the session id
        const ratingRef = ratingsCollection.doc(sessionId);
        const tutorRef = usersCollection.doc(session.tutorId);
        const saved = await db.runTransaction(async (transaction) =>
        {
            const existing = await transaction.get(ratingRef);
            if (existing.exists) return false;

            const tutorDoc = await transaction.get(tutorRef);
            transaction.set(ratingRef, rating);
            transaction.update(sessionRef,
            {
                rated: true
            });
            if (tutorDoc.exists)
            {
                const current = tutorDoc.data().rating ||
                {
                    average: 0,
                    count: 0
                };
                const count = current.count + 1;
                const average = Math.round(((current.average * current.count) + body.stars) / count * 100) / 100;
                transaction.update(tutorRef,
                {
                    rating:
                    {
                        average,
                        count
                    }
                });
            }
            return true;
        });

        if (!saved)
        {
            return res.status(409).json(
            {
                error: "You have already rated this session.",
                code: "ALREADY_RATED"
            });
        }

        await notify(session.tutorId, "new_rating", "New rating received",
            `${rating.studentName} rated your ${session.subject} session ${rating.stars} out of 5.`);
        res.status(201).json(
        {
            id: sessionId,
            ...rating
        });
    }
    catch (err)
    {
        next(err);
    }
});

// GET /ratings/tutor/:tutorId - the latest reviews of one tutor, newest first
router.get("/tutor/:tutorId", async (req, res, next) =>
{
    try
    {
        const results = await ratingsCollection.where("tutorId", "==", req.params.tutorId).get();
        const reviews = results.docs
            .map((ratingDoc) => ratingDoc.data())
            .sort((first, second) => timeKey(second.createdAt).localeCompare(timeKey(first.createdAt)))
            .slice(0, MAX_REVIEWS)
            .map((rating) => (
            {
                stars: rating.stars,
                comment: rating.comment,
                studentName: rating.studentName,
                createdAt: rating.createdAt
            }));
        res.json(reviews);
    }
    catch (err)
    {
        next(err);
    }
});

module.exports = router;
