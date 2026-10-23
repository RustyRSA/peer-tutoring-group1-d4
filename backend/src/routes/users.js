const express = require("express");
const
{
    db,
    auth
} = require("../firebase");
const
{
    checkToken,
    requireProfile,
    requireAdmin
} = require("../middleware/auth");
const
{
    validateProfile,
    isObject,
    ROLES,
    ACCOUNT_STATUSES,
    TUTOR_STATUSES
} = require("../utils/validate");
const
{
    privateView,
    publicView,
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
const usersCollection = db.collection("users");

function sendValidationError(res, fields)
{
    res.status(400).json(
    {
        error: "Some fields are missing or invalid.",
        code: "VALIDATION_ERROR",
        fields
    });
}

function sameSubjects(firstList = [], secondList = [])
{
    const normalise = (subjects) => subjects.map((subjectName) => subjectName.toLowerCase()).sort().join("|");
    return normalise(firstList) === normalise(secondList);
}

//all route needs valid login token
router.use(checkToken);

// POST /users - create my profile straight after sign up
router.post("/", async (req, res, next) =>
{
    try
    {
        if (req.user.profile)
        {
            return res.status(409).json(
            {
                error: "A profile already exists for this account.",
                code: "PROFILE_EXISTS"
            });
        }
        if (!isObject(req.body))
        {
            return res.status(400).json(
            {
                error: "The request body must be a JSON object.",
                code: "VALIDATION_ERROR"
            });
        }

        // Optional: only university emails may sign up. Set UNIVERSITY_DOMAIN in .env (e.g. @vossie.net); leave it out to allow any email.
        const universityDomain = (process.env.UNIVERSITY_DOMAIN || "").trim().toLowerCase();
        if (universityDomain && !(req.user.email || "").toLowerCase().endsWith(universityDomain))
        {
            return res.status(400).json(
            {
                error: "Please use your university email address.",
                code: "INVALID_EMAIL_DOMAIN"
            });
        }

        const
        {
            cleaned,
            errors
        } = validateProfile(req.body, true);
        if (Object.keys(errors).length > 0)
        {
            return sendValidationError(res, errors);
        }

        const timestamp = new Date().toISOString();
        const subjectsOffered = cleaned.subjectsOffered || [];
        const profile = {
            email: req.user.email, // from the token, never from the app
            firstName: cleaned.firstName ?? null,
            lastName: cleaned.lastName ?? null,
            role: cleaned.role || "student",
            university: cleaned.university ?? null,
            yearOfStudy: cleaned.yearOfStudy ?? null,
            programme: cleaned.programme ?? null,
            subjectsNeeded: cleaned.subjectsNeeded || [],
            subjectsOffered,
            credentials: cleaned.credentials ?? null,
            // Offering subjects means an admin has to check the tutor first
            tutorStatus: subjectsOffered.length > 0 ? "pending" : "none",
            accountStatus: "active",
            rating:
            {
                average: 0,
                count: 0
            },
            createdAt: timestamp,
            updatedAt: timestamp,
        };

        // create() fails if the document already exists, unlike set()
        try
        {
            await usersCollection.doc(req.user.uid).create(profile);
        }
        catch (err)
        {
            if (err.code === 6)
            {
                return res.status(409).json(
                {
                    error: "A profile already exists for this account.",
                    code: "PROFILE_EXISTS"
                });
            }
            throw err;
        }

        res.status(201).json(privateView(
        {
            uid: req.user.uid,
            ...profile
        }));
    }
    catch (err)
    {
        next(err);
    }
});

// GET /users/me - my full profile
router.get("/me", requireProfile, (req, res) =>
{
    res.json(privateView(req.user.profile));
});

// PUT /users/me - update my profile only the fields that were sent
router.put("/me", requireProfile, async (req, res, next) =>
{
    try
    {
        if (!isObject(req.body) || Object.keys(req.body).length === 0)
        {
            return res.status(400).json(
            {
                error: "Send at least one field to update.",
                code: "VALIDATION_ERROR"
            });
        }

        const
        {
            cleaned,
            errors
        } = validateProfile(req.body, false);
        if (Object.keys(errors).length > 0)
        {
            return sendValidationError(res, errors);
        }

        const currentProfile = req.user.profile;
        const changes = {
            ...cleaned,
            updatedAt: new Date().toISOString()
        };

        //change what tutor means as admin has to check you agian
        const subjectsChanged = cleaned.subjectsOffered !== undefined && !sameSubjects(cleaned.subjectsOffered, currentProfile.subjectsOffered);
        const credentialsChanged = cleaned.credentials !== undefined && cleaned.credentials !== currentProfile.credentials;
        if (subjectsChanged || credentialsChanged)
        {
            const offeredSubjects = cleaned.subjectsOffered ?? currentProfile.subjectsOffered ?? [];
            changes.tutorStatus = offeredSubjects.length > 0 ? "pending" : "none";
        }

        await usersCollection.doc(currentProfile.uid).update(changes);
        res.json(privateView(
        {
            ...currentProfile,
            ...changes
        }));
    }
    catch (err)
    {
        next(err);
    }
});

// DELETE /users/me - delete my profile and my login
router.delete("/me", requireProfile, async (req, res, next) =>
{
    try
    {
        await usersCollection.doc(req.user.uid).delete();
        try
        {
            await auth.deleteUser(req.user.uid);
        }
        catch (err)
        {
            if (err.code !== "auth/user-not-found") throw err;
        }
        res.json(
        {
            message: "Your account has been deleted."
        });
    }
    catch (err)
    {
        next(err);
    }
});

// GET /users/tutors?subject=&q= approved tutors, best rated first
router.get("/tutors", requireProfile, async (req, res, next) =>
{
    try
    {
        const subjectFilter = typeof req.query.subject === "string" ? req.query.subject.trim().toLowerCase() : "";
        const searchText = typeof req.query.q === "string" ? req.query.q.trim().toLowerCase() : "";

        const results = await usersCollection
            .where("tutorStatus", "==", "approved")
            .where("accountStatus", "==", "active")
            .get();

        // firestore cant search contains, filters here
        const tutors = results.docs
            .map((userDoc) => (
            {
                uid: userDoc.id,
                ...userDoc.data()
            }))
            .filter((tutor) => tutor.uid !== req.user.uid)
            .filter((tutor) => !subjectFilter || (tutor.subjectsOffered || []).some((subjectName) => subjectName.toLowerCase() === subjectFilter))
            .filter((tutor) => !searchText ||
                fullName(tutor).toLowerCase().includes(searchText) ||
                (tutor.subjectsOffered || []).some((subjectName) => subjectName.toLowerCase().includes(searchText)));

        tutors.sort((firstTutor, secondTutor) =>
        {
            const firstRating = firstTutor.rating ||
            {
                average: 0,
                count: 0
            };
            const secondRating = secondTutor.rating ||
            {
                average: 0,
                count: 0
            };
            return secondRating.average - firstRating.average || secondRating.count - firstRating.count || fullName(firstTutor).localeCompare(fullName(secondTutor));
        });

        res.json(tutors.map(publicView));
    }
    catch (err)
    {
        next(err);
    }
});

// GET /users?role=&accountStatus=&tutorStatus= admin: list users, newest first
router.get("/", requireProfile, requireAdmin, async (req, res, next) =>
{
    try
    {
        const allowedFilters = {
            role: ROLES,
            accountStatus: ACCOUNT_STATUSES,
            tutorStatus: TUTOR_STATUSES
        };
        const errors = {};
        let usersQuery = usersCollection;

        for (const [fieldName, allowedValues] of Object.entries(allowedFilters))
        {
            const value = req.query[fieldName];
            if (value === undefined || value === "") continue;
            if (!allowedValues.includes(String(value).toLowerCase()))
            {
                errors[fieldName] = `Must be one of: ${allowedValues.join(", ")}.`;
            }
            else
            {
                usersQuery = usersQuery.where(fieldName, "==", String(value).toLowerCase());
            }
        }
        if (Object.keys(errors).length > 0)
        {
            return sendValidationError(res, errors);
        }

        const results = await usersQuery.get();
        const userList = results.docs.map((userDoc) => (
        {
            uid: userDoc.id,
            ...userDoc.data()
        }));
        userList.sort((firstUser, secondUser) => timeKey(secondUser.createdAt).localeCompare(timeKey(firstUser.createdAt)));
        res.json(userList.map(privateView));
    }
    catch (err)
    {
        next(err);
    }
});

// GET /users/:uid someones profile: public fields only, unless you are admin
router.get("/:uid", requireProfile, async (req, res, next) =>
{
    try
    {
        const myProfile = req.user.profile;
        if (req.params.uid === myProfile.uid)
        {
            return res.json(privateView(myProfile));
        }

        const userDoc = await usersCollection.doc(req.params.uid).get();
        const isAdmin = myProfile.role === "admin";
        if (!userDoc.exists || (!isAdmin && userDoc.data().accountStatus !== "active"))
        {
            return res.status(404).json(
            {
                error: "User not found.",
                code: "NOT_FOUND"
            });
        }

        const profile = {
            uid: userDoc.id,
            ...userDoc.data()
        };
        res.json(isAdmin ? privateView(profile) : publicView(profile));
    }
    catch (err)
    {
        next(err);
    }
});

// PUT /users/:uid/status admin: suspend or reactivate an account, approve or reject a tutor
router.put("/:uid/status", requireProfile, requireAdmin, async (req, res, next) =>
{
    try
    {
        const body = isObject(req.body) ? req.body :
        {};
        const errors = {};

        for (const fieldName of Object.keys(body))
        {
            if (fieldName !== "accountStatus" && fieldName !== "tutorStatus") errors[fieldName] = "This field cannot be set here.";
        }
        if (body.accountStatus !== undefined && !["active", "suspended"].includes(body.accountStatus))
        {
            errors.accountStatus = "Must be one of: active, suspended.";
        }
        if (body.tutorStatus !== undefined && !["approved", "rejected"].includes(body.tutorStatus))
        {
            errors.tutorStatus = "Must be one of: approved, rejected.";
        }
        if (Object.keys(errors).length > 0)
        {
            return sendValidationError(res, errors);
        }
        if (!body.accountStatus && !body.tutorStatus)
        {
            return res.status(400).json(
            {
                error: "Send accountStatus and/or tutorStatus.",
                code: "VALIDATION_ERROR"
            });
        }
        if (req.params.uid === req.user.uid && body.accountStatus)
        {
            return res.status(400).json(
            {
                error: "You cannot change the status of your own account.",
                code: "VALIDATION_ERROR"
            });
        }

        const userDoc = await usersCollection.doc(req.params.uid).get();
        if (!userDoc.exists)
        {
            return res.status(404).json(
            {
                error: "User not found.",
                code: "NOT_FOUND"
            });
        }
        const targetUser = {
            uid: userDoc.id,
            ...userDoc.data()
        };

        const timestamp = new Date().toISOString();
        const changes = {
            updatedAt: timestamp,
            reviewedBy: req.user.uid,
            reviewedAt: timestamp
        };
        if (body.accountStatus)
        {
            changes.accountStatus = body.accountStatus;
        }
        if (body.tutorStatus)
        {
            if ((targetUser.subjectsOffered || []).length === 0)
            {
                return res.status(409).json(
                {
                    error: "This user has not applied to tutor any subjects.",
                    code: "NO_TUTOR_APPLICATION"
                });
            }
            changes.tutorStatus = body.tutorStatus;
            if (body.tutorStatus === "approved" && targetUser.role === "student")
            {
                changes.role = "tutor";
            }
        }

        await usersCollection.doc(targetUser.uid).update(changes);

        // let the user know about the decision (a suspended user cannot log in, so only the other cases)
        if (body.tutorStatus === "approved")
        {
            await notify(targetUser.uid, "tutor_approved", "Tutor application approved", "You can now be found by students and receive session requests.");
        }
        else if (body.tutorStatus === "rejected")
        {
            await notify(targetUser.uid, "tutor_rejected", "Tutor application rejected", "An administrator did not approve your tutor application.");
        }
        if (body.accountStatus === "active")
        {
            await notify(targetUser.uid, "account_reactivated", "Account reactivated", "Your account is active again.");
        }

        res.json(privateView(
        {
            ...targetUser,
            ...changes
        }));
    }
    catch (err)
    {
        next(err);
    }
});

module.exports = router;