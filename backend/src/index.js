require("dotenv").config();
const express = require("express");
const userRoutes = require("./routes/users");
const sessionRoutes = require("./routes/sessions");
const messageRoutes = require("./routes/messages");
const notificationRoutes = require("./routes/notifications");
const ratingRoutes = require("./routes/ratings");

const app = express();
app.use(express.json());

app.get("/health", (req, res) =>
{
    res.json(
    {
        status: "ok"
    });
});

app.use("/users", userRoutes);
app.use("/sessions", sessionRoutes);
app.use("/messages", messageRoutes);
app.use("/notifications", notificationRoutes);
app.use("/ratings", ratingRoutes);

app.use((req, res) =>
{
    res.status(404).json(
    {
        error: `No route for ${req.method} ${req.path}`,
        code: "ROUTE_NOT_FOUND"
    });
});

// Error handler: Express knows it is one, takes 4 arguments
app.use((err, req, res, next) =>
{
    if (err.type === "entity.parse.failed")
    {
        return res.status(400).json(
        {
            error: "The request body is not valid JSON.",
            code: "INVALID_JSON"
        });
    }
    console.error(err);
    res.status(500).json(
    {
        error: "Something went wrong on the server. Please try again.",
        code: "INTERNAL_ERROR"
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () =>
{
    console.log(`Peer Tutoring API running on http://localhost:${PORT}`);
});