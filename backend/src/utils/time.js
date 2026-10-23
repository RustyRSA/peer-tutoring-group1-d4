// Turns a stored timestamp into text that sorts correctly.
// Documents made by hand in the Firestore console can hold a Timestamp object, or no value at all,
// instead of the ISO text the API writes, so sorting must not assume a string.
function timeKey(value)
{
    if (typeof value === "string") return value;
    if (value && typeof value.toDate === "function") return value.toDate().toISOString();
    return "";
}

module.exports = {
    timeKey
};
