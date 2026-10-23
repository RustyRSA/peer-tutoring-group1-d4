const PROFILE_FIELDS = ["firstName", "lastName", "university", "yearOfStudy", "programme", "subjectsNeeded", "subjectsOffered", "credentials"];
const ROLES = ["student", "tutor", "admin"];
const ACCOUNT_STATUSES = ["active", "suspended"];
const TUTOR_STATUSES = ["none", "pending", "approved", "rejected"];

// validate - letters , spaces, hyphens, apostrophes and full stops
const NAME_PATTERN = /^\p{L}[\p{L}'’ .-]*$/u;

function isBlank(value)
{
    return value === null || (typeof value === "string" && value.trim() === "");
}

// Turns "ITMDA, Programming" or ["ITMDA", "Programming"] into a clean list without duplicates
function toSubjectList(value)
{
    const pieces = typeof value === "string" ? value.split(",") : value;
    if (!Array.isArray(pieces) || pieces.some((piece) => typeof piece !== "string"))
    {
        return null;
    }

    const subjects = [];
    for (const piece of pieces)
    {
        const subject = piece.trim();
        if (subject && !subjects.some((existingSubject) => existingSubject.toLowerCase() === subject.toLowerCase()))
        {
            subjects.push(subject);
        }
    }
    return subjects;
}

function checkName(value, errors, fieldName, cleaned)
{
    if (isBlank(value))
    {
        errors[fieldName] = "This field is required.";
    }
    else if (typeof value !== "string")
    {
        errors[fieldName] = "Must be text.";
    }
    else if (value.trim().length > 50)
    {
        errors[fieldName] = "Must be at most 50 characters.";
    }
    else if (!NAME_PATTERN.test(value.trim()))
    {
        errors[fieldName] = "Can only contain letters, spaces, hyphens and apostrophes.";
    }
    else
    {
        cleaned[fieldName] = value.trim();
    }
}

function checkText(value, maxLength, errors, fieldName, cleaned)
{
    if (isBlank(value))
    {
        cleaned[fieldName] = null;
    }
    else if (typeof value !== "string")
    {
        errors[fieldName] = "Must be text.";
    }
    else if (value.trim().length > maxLength)
    {
        errors[fieldName] = `Must be at most ${maxLength} characters.`;
    }
    else
    {
        cleaned[fieldName] = value.trim();
    }
}

function checkYear(value, errors, cleaned)
{
    if (isBlank(value))
    {
        cleaned.yearOfStudy = null;
        return;
    }
    // andriod text box send 3, thus accept that 3
    const year = typeof value === "string" && /^\d+$/.test(value.trim()) ? Number(value) : value;
    if (!Number.isInteger(year) || year < 1 || year > 6)
    {
        errors.yearOfStudy = "Must be a whole number from 1 to 6.";
    }
    else
    {
        cleaned.yearOfStudy = year;
    }
}

function checkSubjects(value, errors, fieldName, cleaned)
{
    if (isBlank(value))
    {
        cleaned[fieldName] = [];
        return;
    }
    const subjects = toSubjectList(value);
    if (subjects === null)
    {
        errors[fieldName] = "Must be a list of subject names.";
    }
    else if (subjects.some((subjectName) => subjectName.length > 40))
    {
        errors[fieldName] = "Each subject must be at most 40 characters.";
    }
    else if (subjects.length > 10)
    {
        errors[fieldName] = "List at most 10 subjects.";
    }
    else
    {
        cleaned[fieldName] = subjects;
    }
}


// Only fields that were sent are checked, so the same function works for create and update.
//only fields check that were sent, thus function works for create and update
function validateProfile(body, allowRole)
{
    const cleaned = {};
    const errors = {};

    for (const fieldName of Object.keys(body))
    {
        if (!PROFILE_FIELDS.includes(fieldName) && !(allowRole && fieldName === "role"))
        {
            errors[fieldName] = "This field cannot be set here.";
        }
    }

    if (body.firstName !== undefined) checkName(body.firstName, errors, "firstName", cleaned);
    if (body.lastName !== undefined) checkName(body.lastName, errors, "lastName", cleaned);
    if (body.university !== undefined) checkText(body.university, 100, errors, "university", cleaned);
    if (body.programme !== undefined) checkText(body.programme, 100, errors, "programme", cleaned);
    if (body.credentials !== undefined) checkText(body.credentials, 500, errors, "credentials", cleaned);
    if (body.yearOfStudy !== undefined) checkYear(body.yearOfStudy, errors, cleaned);
    if (body.subjectsNeeded !== undefined) checkSubjects(body.subjectsNeeded, errors, "subjectsNeeded", cleaned);
    if (body.subjectsOffered !== undefined) checkSubjects(body.subjectsOffered, errors, "subjectsOffered", cleaned);

    // Users can only pick student or tutor; admin is set by hand in the Firebase console
    if (allowRole && body.role !== undefined && !isBlank(body.role))
    {
        const role = String(body.role).trim().toLowerCase();
        if (role === "student" || role === "tutor")
        {
            cleaned.role = role;
        }
        else
        {
            errors.role = "Must be one of: student, tutor.";
        }
    }

    return {
        cleaned,
        errors
    };
}

function isObject(value)
{
    return value !== null && typeof value === "object" && !Array.isArray(value);
}

module.exports = {
    validateProfile,
    isObject,
    ROLES,
    ACCOUNT_STATUSES,
    TUTOR_STATUSES
};