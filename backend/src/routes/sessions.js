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
const sessionsCollection = db.collection("sessions");
const usersCollection = db.collection("users");

const SESSION_STATUSES_TUTOR_CAN_SET = ["accepted", "declined", "completed"];

function sendValidationError(res, fields)
{
    res.status(400).json(
    {
        error: "Some fields are missing or invalid.",
        code: "VALIDATION_ERROR",
        fields
    });
}

function sendNotFound(res)
{
    res.status(404).json(
    {
        error: "Session not found.",
        code: "NOT_FOUND"
    });
}

function isNonEmptyText(value, maxLength)
{
    return typeof value === "string" && value.trim() !== "" && value.trim().length <= maxLength;
}

// every route needs a valid login token and a profile
router.use(checkToken, requireProfile);

// POST /sessions - a student asks an approved tutor for a session
router.post("/", async (req, res, next) =>
{
    try
    {
        const body = isObject(req.body) ? req.body :
        {};
        const errors = {};
        if (!isNonEmptyText(body.tutorId, 128)) errors.tutorId = "This field is required.";
        if (!isNonEmptyText(body.subject, 40)) errors.subject = "Required, at most 40 characters.";
        if (!isNonEmptyText(body.topic, 200)) errors.topic = "Required, at most 200 characters.";
        if (!isNonEmptyText(body.preferredTime, 100)) errors.preferredTime = "Required, at most 100 characters.";
        if (Object.keys(errors).length > 0)
        {
            return sendValidationError(res, errors);
        }

        const tutorId = body.tutorId.trim();
        if (tutorId === req.user.uid)
        {
            return res.status(400).json(
            {
                error: "You cannot book a session with yourself.",
                code: "VALIDATION_ERROR"
            });
        }

        // tutors are users whose tutorStatus is approved
        const tutorDoc = await usersCollection.doc(tutorId).get();
        const tutor = tutorDoc.exists ? tutorDoc.data() : null;
        if (!tutor || tutor.tutorStatus !== "approved" || tutor.accountStatus !== "active")
        {
            return res.status(404).json(
            {
                error: "Tutor not available.",
                code: "TUTOR_NOT_AVAILABLE"
            });
        }

        const timestamp = new Date().toISOString();
        const session = {
            studentId: req.user.uid,
            studentName: fullName(req.user.profile),
            tutorId,
            tutorName: fullName(tutor),
            subject: body.subject.trim(),
            topic: body.topic.trim(),
            preferredTime: body.preferredTime.trim(),
            status: "pending",
            createdAt: timestamp,
            updatedAt: timestamp
        };
        const ref = await sessionsCollection.add(session);
        await notify(tutorId, "session_request", "New session request",
            `${session.studentName} asked for help with ${session.subject} (${session.preferredTime}).`);
        res.status(201).json(
        {
            id: ref.id,
            ...session
        });
    }
    catch (err)
    {
        next(err);
    }
});

// GET /sessions - my sessions, as a student or as a tutor, newest first
router.get("/", async (req, res, next) =>
{
    try
    {
        const uid = req.user.uid;
        const [asStudent, asTutor] = await Promise.all(
        [
            sessionsCollection.where("studentId", "==", uid).get(),
            sessionsCollection.where("tutorId", "==", uid).get()
        ]);
        const list = [...asStudent.docs, ...asTutor.docs].map((sessionDoc) => (
        {
            id: sessionDoc.id,
            ...sessionDoc.data()
        }));
        list.sort((first, second) => timeKey(second.createdAt).localeCompare(timeKey(first.createdAt)));
        res.json(list);
    }
    catch (err)
    {
        next(err);
    }
});

// PUT /sessions/:id - the tutor sets status and/or time, the student changes topic and/or time
router.put("/:id", async (req, res, next) =>
{
    try
    {
        const ref = sessionsCollection.doc(req.params.id);
        const sessionDoc = await ref.get();
        if (!sessionDoc.exists)
        {
            return sendNotFound(res);
        }

        const session = sessionDoc.data();
        const uid = req.user.uid;
        if (session.studentId !== uid && session.tutorId !== uid)
        {
            return res.status(403).json(
            {
                error: "This is not your session.",
                code: "FORBIDDEN"
            });
        }

        const body = isObject(req.body) ? req.body :
        {};
        const allowedFields = session.tutorId === uid ? ["status", "preferredTime"] : ["topic", "preferredTime"];
        const errors = {};
        const updates = {};

        for (const fieldName of Object.keys(body))
        {
            if (!allowedFields.includes(fieldName)) errors[fieldName] = "This field cannot be set here.";
        }
        if (body.status !== undefined && !SESSION_STATUSES_TUTOR_CAN_SET.includes(body.status))
        {
            errors.status = `Must be one of: ${SESSION_STATUSES_TUTOR_CAN_SET.join(", ")}.`;
        }
        if (body.topic !== undefined && !isNonEmptyText(body.topic, 200))
        {
            errors.topic = "Required, at most 200 characters.";
        }
        if (body.preferredTime !== undefined && !isNonEmptyText(body.preferredTime, 100))
        {
            errors.preferredTime = "Required, at most 100 characters.";
        }
        if (Object.keys(errors).length > 0)
        {
            return sendValidationError(res, errors);
        }

        for (const fieldName of allowedFields)
        {
            if (body[fieldName] !== undefined)
            {
                updates[fieldName] = typeof body[fieldName] === "string" ? body[fieldName].trim() : body[fieldName];
            }
        }
        if (Object.keys(updates).length === 0)
        {
            return res.status(400).json(
            {
                error: "Send at least one field to update.",
                code: "VALIDATION_ERROR"
            });
        }

        updates.updatedAt = new Date().toISOString();
        await ref.update(updates);

        const iAmTutor = session.tutorId === uid;
        const otherId = iAmTutor ? session.studentId : session.tutorId;
        const myName = iAmTutor ? session.tutorName : session.studentName;
        const statusMessages = {
            accepted: `${myName} accepted your ${session.subject} session.`,
            declined: `${myName} declined your ${session.subject} session.`,
            completed: `${myName} marked your ${session.subject} session as completed.`
        };
        if (updates.status)
        {
            await notify(otherId, `session_${updates.status}`, `Session ${updates.status}`, statusMessages[updates.status]);
        }
        else
        {
            await notify(otherId, "session_updated", "Session updated", `${myName} changed the details of your ${session.subject} session.`);
        }

        res.json(
        {
            id: sessionDoc.id,
            ...session,
            ...updates
        });
    }
    catch (err)
    {
        next(err);
    }
});

// DELETE /sessions/:id - the student cancels their session
router.delete("/:id", async (req, res, next) =>
{
    try
    {
        const ref = sessionsCollection.doc(req.params.id);
        const sessionDoc = await ref.get();
        if (!sessionDoc.exists)
        {
            return sendNotFound(res);
        }
        if (sessionDoc.data().studentId !== req.user.uid)
        {
            return res.status(403).json(
            {
                error: "Only the student can cancel the session.",
                code: "FORBIDDEN"
            });
        }
        const cancelled = sessionDoc.data();
        await ref.delete();
        await notify(cancelled.tutorId, "session_cancelled", "Session cancelled",
            `${cancelled.studentName} cancelled the ${cancelled.subject} session.`);
        res.status(204).send();
    }
    catch (err)
    {
        next(err);
    }
});

module.exports = router;
