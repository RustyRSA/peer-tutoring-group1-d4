// All user data, sent to the user and to admin
function privateView(profile)
{
    return {
        uid: profile.uid,
        email: profile.email,
        firstName: profile.firstName,
        lastName: profile.lastName,
        role: profile.role,
        university: profile.university,
        yearOfStudy: profile.yearOfStudy,
        programme: profile.programme,
        subjectsNeeded: profile.subjectsNeeded,
        subjectsOffered: profile.subjectsOffered,
        credentials: profile.credentials,
        tutorStatus: profile.tutorStatus,
        accountStatus: profile.accountStatus,
        rating: profile.rating,
        createdAt: profile.createdAt,
        updatedAt: profile.updatedAt,
    };
}

// what other users see: no email, no subjects they want help with, no account status
function publicView(profile)
{
    const verifiedTutor = profile.tutorStatus === "approved";
    return {
        uid: profile.uid,
        firstName: profile.firstName,
        lastName: profile.lastName,
        role: profile.role,
        university: profile.university,
        yearOfStudy: profile.yearOfStudy,
        programme: profile.programme,
        subjectsOffered: verifiedTutor ? profile.subjectsOffered || [] : [],
        credentials: verifiedTutor ? profile.credentials : null,
        verifiedTutor,
        rating: profile.rating ||
        {
            average: 0,
            count: 0
        },
    };
}

function fullName(profile)
{
    return [profile.firstName, profile.lastName].filter(Boolean).join(" ");
}

module.exports = {
    privateView,
    publicView,
    fullName
};