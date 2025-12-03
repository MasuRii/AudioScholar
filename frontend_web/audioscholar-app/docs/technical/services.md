# API Services

This document details the service layer used to communicate with the backend API. Services are located in `src/services/`.

## Base Configuration

The API base URL is determined dynamically in `src/services/authService.js`.

```javascript
const getApiBaseUrl = () => {
    const hostname = window.location.hostname;
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
        return 'http://localhost:8080/';
    } else {
        return 'https://it342-g3-audioscholar-onrender-com.onrender.com/';
    }
};
```

All services use `fetch` for HTTP requests and handle authentication by including the `Authorization` header with a Bearer token stored in `localStorage` (`AuthToken`).

## Authentication Service

**File:** `src/services/authService.js`

Handles user authentication and registration.

- `verifyFirebaseTokenWithBackend(idToken)`: Sends the Firebase ID token to the backend for verification.
- `verifyGoogleTokenWithBackend(googleIdToken)`: Sends the Google ID token to the backend for verification.
- `signUp(userData)`: Registers a new user.

## Note Service

**File:** `src/services/noteService.js`

Manages operations related to notes attached to recordings.

- `createNote(recordingId, content, tags)`: Creates a new note.
- `getNotes(recordingId)`: Fetches all notes for a specific recording.
- `getNote(noteId)`: Fetches a single note by ID.
- `updateNote(noteId, content, tags)`: Updates an existing note.
- `deleteNote(noteId)`: Deletes a note.

## Admin Service

**File:** `src/services/adminService.js`

Provides functionality for administrative tasks.

### User Management
- `getUsers(limit, startAfter)`: Fetches a paginated list of users.
- `updateUserStatus(uid, disabled)`: Enables or disables a user account.
- `updateUserRoles(uid, roles)`: Updates the roles assigned to a user.

### Analytics
- `getOverview()`: Fetches high-level system statistics.
- `getActivityStats()`: Fetches user activity data.
- `getUserDistribution()`: Fetches data on user demographics or distribution.
- `getContentEngagement()`: Fetches metrics on how users interact with content.

## Error Handling

Services generally throw an `Error` object if the `fetch` response is not `ok`. The error object may contain additional status or data properties depending on the implementation.