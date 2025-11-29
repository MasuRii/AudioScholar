# AudioScholar Admin API Design Document

## 1. Overview
This document outlines the design for the new Admin API in AudioScholar. The goal is to provide restricted endpoints for system administrators to manage users, analyze platform usage, and monitor system health.

Key features include:
- **Role-Based Access Control (RBAC):** Implementation of a secure `ROLE_ADMIN`.
- **User Management:** Listing, searching, role modification (promote/demote), and account status management (ban/unban).
- **Data-Driven Analytics:** Insights into user growth, content engagement, and platform activity.
- **System Monitoring:** Infrastructure health and service status.

## 2. Security Architecture

### 2.1 Current State vs. Required Changes
**Current State:**
- Authentication is handled via JWT.
- `JwtTokenProvider` generates tokens with a custom claim `"roles": ["ROLE_USER", ...]`.
- `SecurityConfig` uses the default `jwt()` configuration, which expects authorities in `scope` or `scp` claims.
- **Problem:** `@PreAuthorize("hasRole('ADMIN')")` will fail because Spring Security doesn't see the authorities in the "roles" claim.

**Proposed Solution:**
Implement a custom `JwtAuthenticationConverter` to map the `"roles"` claim to Spring Security Authorities.

### 2.2 Implementation Details
We will create `JwtAuthorityConverter.java` (or define it as a bean in `SecurityConfig`) that:
1.  Extracts the `roles` claim from the JWT.
2.  Converts each role string (e.g., "ROLE_ADMIN") into a `SimpleGrantedAuthority`.
3.  Returns the collection of authorities.

**Configuration Update (`SecurityConfig.java`):**
```java
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> jwt
        .decoder(jwtDecoder())
        .jwtAuthenticationConverter(jwtAuthenticationConverter()) // New Converter
    )
)
```

## 3. Schema Improvements

To support the analytics requirements, the following schema changes are necessary.

### 3.1 User Model
*   **Add `createdAt` (Timestamp):**
    *   *Reason:* Required to plot user growth over time (e.g., "New Users per Day").
    *   *Implementation:* Populate this field upon user registration. Backfill existing users with a default date if necessary.
*   **Add `lastLoginAt` (Timestamp):**
    *   *Reason:* Essential for calculating DAU (Daily Active Users) and retention metrics.

### 3.2 Recording Model
*   **Add `fileSize` (Long):**
    *   *Reason:* Stores the size of the audio file in bytes. Required to calculate total storage usage and average cost per user.
*   **Add `favoriteCount` (Integer):**
    *   *Reason:* Denormalized counter for the number of users who have favorited this recording. Enables efficient querying of "Most Popular Content" without costly aggregation.

## 4. API Specification

### Base URL: `/api/admin`
**Authentication:** Required (Bearer Token)
**Authorization:** `ROLE_ADMIN` required for all endpoints.

### 4.1 User Management

#### 4.1.1 List Users
Retrieves a paginated list of registered users.

- **Endpoint:** `GET /api/admin/users`
- **Query Parameters:**
    - `page` (int, default: 0): Page number.
    - `size` (int, default: 20): Items per page.
    - `email` (string, optional): Filter by email (partial or exact match depending on Firebase capabilities).
- **Response (200 OK):**
```json
{
  "users": [
    {
      "userId": "firebase_uid_123",
      "email": "user@example.com",
      "displayName": "John Doe",
      "roles": ["ROLE_USER"],
      "disabled": false,
      "createdAt": "2023-10-01T12:00:00Z"
    }
  ],
  "pagination": {
    "offset": 0,
    "limit": 20,
    "total": 150
  }
}
```

#### 4.1.2 Get User Details
Retrieves full profile details for a specific user.

- **Endpoint:** `GET /api/admin/users/{userId}`
- **Response (200 OK):**
```json
{
  "userId": "firebase_uid_123",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["ROLE_USER", "ROLE_ADMIN"],
  "provider": "google",
  "fcmTokens": ["token1", "token2"],
  "accountStatus": {
    "emailVerified": true,
    "disabled": false
  },
  "stats": {
      "recordingCount": 5,
      "lastLoginAt": "2023-10-05T10:00:00Z"
  }
}
```

#### 4.1.3 Update User Roles (Promote/Demote)
Updates the list of roles assigned to a user.

- **Endpoint:** `PUT /api/admin/users/{userId}/roles`
- **Request Body:**
```json
{
  "roles": ["ROLE_USER", "ROLE_ADMIN"]
}
```
- **Response (200 OK):** Returns the updated User object.

#### 4.1.4 Ban/Unban User
Enables or disables a user account. This should disable the user in Firebase Auth.

- **Endpoint:** `PUT /api/admin/users/{userId}/status`
- **Request Body:**
```json
{
  "disabled": true
}
```
- **Response (200 OK):**
```json
{
  "userId": "firebase_uid_123",
  "disabled": true,
  "message": "User account has been disabled."
}
```

### 4.2 Analytics API

#### 4.2.1 Overview Stats
Provides aggregated counts for the dashboard header.

- **Endpoint:** `GET /api/admin/analytics/overview`
- **Response (200 OK):**
```json
{
  "totalUsers": 1250,
  "totalRecordings": 5400,
  "totalDurationSeconds": 3600000,
  "totalStorageBytes": 10737418240, // ~10GB
  "activeUsersLast24h": 45
}
```

#### 4.2.2 Time-Series Activity
Returns historical data for plotting activity charts.

- **Endpoint:** `GET /api/admin/analytics/activity`
- **Query Parameters:**
    - `range` (string, default: `7d`): Time range (e.g., `7d`, `30d`, `90d`).
    - `metric` (string, required): `users` (new registrations) or `recordings` (uploads).
- **Response (200 OK):**
```json
{
  "metric": "users",
  "range": "7d",
  "data": [
    { "date": "2023-10-01", "count": 12 },
    { "date": "2023-10-02", "count": 8 },
    { "date": "2023-10-03", "count": 15 }
  ]
}
```

#### 4.2.3 User Insights (Distribution)
Breakdown of user demographics and properties.

- **Endpoint:** `GET /api/admin/analytics/users/distribution`
- **Query Parameters:**
    - `type` (string, required): `auth_provider` (e.g., google, github) or `roles`.
- **Response (200 OK):**
```json
{
  "type": "auth_provider",
  "distribution": {
    "google": 800,
    "github": 300,
    "password": 150
  }
}
```

#### 4.2.4 Content Insights
Metrics on content engagement.

- **Endpoint:** `GET /api/admin/analytics/content/engagement`
- **Query Parameters:**
    - `sort` (string, default: `favorites`): Criteria for top content.
    - `limit` (int, default: 10).
- **Response (200 OK):**
```json
{
  "topFavorites": [
    { "recordingId": "rec_1", "title": "Lecture 1", "favoriteCount": 45 },
    { "recordingId": "rec_2", "title": "Physics 101", "favoriteCount": 32 }
  ]
}
```

### 4.3 System Monitoring

#### 4.3.1 System Health
Provides infrastructure status and health metrics.

- **Endpoint:** `GET /api/admin/system/health`
- **Response (200 OK):**
```json
{
  "status": "UP",
  "components": {
      "db": { "status": "UP", "details": "Firestore connected" },
      "queue": { "status": "UP", "queueSize": 0 },
      "diskSpace": { "status": "UP", "free": "100GB" }
  },
  "uptime": 123456,
  "version": "1.0.0"
}
```

## 5. Implementation Steps

### Phase 1: Security & Schema (Prerequisites)
1.  **Modify `SecurityConfig.java`** for RBAC (`JwtAuthenticationConverter`).
2.  **Update Models:**
    -   Update `User.java` to include `createdAt` and `lastLoginAt`.
    -   Update `Recording.java` to include `fileSize` and `favoriteCount`.
3.  **Database Migration (Optional/Lazy):** Plan for backfilling `createdAt` for existing users if possible (e.g., using Firebase metadata).

### Phase 2: Service Layer Extensions
1.  **Extend `FirebaseService.java`**:
    -   Add `listAllUsers` functionality (wrapping Firebase Admin SDK).
    -   Add `setUserDisabledStatus`.
2.  **Extend `UserService.java`**:
    -   Add `updateUserRoles`.
    -   Ensure `getAllUsers` supports filtering/sorting.
    -   **Update:** Set `createdAt` and `lastLoginAt` during auth flows.
3.  **Recording Service:**
    -   **Update:** Set `fileSize` on upload.
    -   **Update:** Handle `favoriteCount` increments/decrements.
4.  **Create `AnalyticsService.java`**:
    -   Implement logic for aggregations (Overview, Activity, Distribution).
    -   Implement methods to query Firestore or a dedicated stats collection.

### Phase 3: Controller Implementation
1.  **Create `AdminController.java`**: Implement User Management endpoints.
2.  **Create `AdminAnalyticsController.java`**: Implement the new Analytics endpoints.
3.  **Refine `AdminSystemController.java`**: Implement the Health check endpoint.

## 6. Potential Issues & Mitigations
-   **Aggregation Performance:** Calculating "Total Duration" or "Total Users" on the fly in Firestore is expensive and slow.
    -   *Mitigation:* Use Firestore Aggregation Queries (count) where available. For sums (duration, size), implement **Distributed Counters** (sharded counters) in a `stats` collection that updates on write.
-   **Time-Series Data:** Firestore isn't optimized for time-series queries.
    -   *Mitigation:* Create a `daily_stats` collection. A scheduled Cloud Function or Cron Job can aggregate daily metrics (new users, new recordings) and write to this collection for fast read by the dashboard.
-   **Pagination Consistency:** Firebase Auth `listUsers` uses page tokens, while frontend table components often prefer offset/limit.
    -   *Mitigation:* We will initially support standard Firestore pagination (User collection) for the table view.