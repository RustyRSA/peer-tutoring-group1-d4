const
{
    db,
    auth
} = require("../firebase");


//check login token, load user profile, null if havent made
//comment

async function checkToken(request, response, next)
{
    const authHeader = request.headers.authorization || "";
    if (!authHeader.startsWith("Bearer "))
    {
        return response.status(401).json(
        {
            error: "Missing or bad Auth header. Send 'Bearer <token>'.",
            code: "NO AUTHORISTAION"
        });
    }
 

    let tokenInfo;
    try
    {
        tokenInfo = await auth.verifyIdToken(authHeader.slice(7));
    }
    
    catch (err)
    {
        return response.status(401).json
        (
        {
            error: "The session is invalid or  expired. Log in again.",
            code: "INVALID_TOKEN"
        });
    }


    try
    {
        const userDoc = await db.collection("users").doc(tokenInfo.uid).get();
        const profile = userDoc.exists ?
        {
            uid: userDoc.id,
            ...userDoc.data()
        } : null;


        // Read from the database every time, so a suspension works straight away
        if (profile && profile.accountStatus === "suspended")
        {
            return response.status(403).json(
            {
                error: "Account Suspended. Contact an administrator.",
                code: "ACCOUNT_SUSPENDED"
            });
        }

        request.user = 
        {
            uid: tokenInfo.uid,
            email: tokenInfo.email || null,
            profile
        };
        next();
    }
    catch (err)
    {
        next(err);
    }
}

function requireAdmin(request, response, next)
{
    if (request.user.profile.role !== "admin")
    {
        return response.status(403).json(
        {
            error: "Only administrators can do this.",
            code: "FORBIDDEN"
        });
    }
    next();
}


function requireProfile(request, response, next)
{
    if (!request.user.profile)
    {
        return response.status(404).json(
        {
            error: "No profile exists for this account yet. Create one with POST /users.",
            code: "PROFILE_NOT_FOUND"
        });
    }

    next();
} 



module.exports = 
{
    checkToken,
    requireProfile,
    requireAdmin
};