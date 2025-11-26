# AudioScholar Frontend Tasks (GEMINI.md)
**Date:** November 25, 2025
**Priority:** High
**Status:** Pending

## 🎨 Global UI & Navigation

- [ ] **Fix Light/Dark Mode Functionality**
    - [ ] Debug broken toggle on **Landing Page**.
    - [ ] Debug broken toggle on **Dashboard**.
    - [ ] Ensure persistence across reloads/navigation.

- [ ] **Footer Overhaul**
    - [ ] **Link Resolution:** Replace all placeholder links. Navigate to existing pages or create new ones (ensure no duplication and maintain styling consistency).
    - [ ] **Brand Navigation:** Ensure "AudioScholar" text/logo clicks back to the Landing Page.
    - [ ] **About Page Visibility:** Fix logic to ensure the "About AudioScholar" link appears in the footer (currently only accessible via Developer profile).

## 🔐 Authentication & Security

- [ ] **Sign In / Sign Up UI Consistency**
    - [ ] **Animations:** Add entry animation to Signup page to match the Sign In page behavior.
    - [ ] **Password Visibility:** Add "Show/Hide" eye icon toggle for password fields on both pages.
    - [ ] **Validation:** Implement a visual **Strong Password Strength Meter** on the Signup page following modern standards.

- [ ] **Login Loading State Logic**
    - [ ] Fix dynamic loading indicators.
        - *Current Issue:* OAuth spinner triggers even when doing standard login; Standard login has no spinner.
        - *Fix:* Ensure the loading spinner only appears on the specific button (Standard vs. Google vs. GitHub) being clicked.

- [ ] **Email Verification Flow**
    - [ ] Debug the "Continue" link behavior.
        - *Current Issue:* Clicking link leads to "Failed Verification" page even if verified.
    - [ ] Investigate Firebase Console vs. Frontend Route handling.

- [ ] **GitHub OAuth False Positive**
    - [ ] Debug error message: *"Error: Login failed: GitHub server error during authentication"* (occurs even when login is successful).
    - [ ] Check Frontend error handling vs. Backend response parsing.

## 👤 User Profile & Header

- [ ] **Header Profile Picture Logic**
    - [ ] **Display Logic:** Replace default generic user icon with the actual User Profile Picture.
    - [ ] **Fallback:** If no custom picture exists, use the specific default image from the Profile Page (not the generic header icon).
    - [ ] **Reactivity:** Ensure header updates immediately when the user changes their photo.
    - [ ] **Google OAuth Bug:** Debug why Google OAuth profile pictures are not reflecting in the header (GitHub OAuth is currently working).

## 🎙️ Core Features (Recordings)

- [ ] **Recordings List Error State**
    - [ ] Style the *"Failed to fetch recordings"* error message.
    - [ ] Use a modern, user-friendly layout (e.g., empty state illustration + "Retry" button) instead of plain text.

- [ ] **Recording Detail Loading State**
    - [ ] Create a skeleton loader or modern spinner for the **Summary** and **Recommendations** sections while data is processing.

- [ ] **Update Recording Status Logic**
    - [ ] Sync frontend with recent backend database updates regarding the `status` field for recordings.

## 💳 Payments & Checkout

- [ ] **Payment Method UI**
    - [ ] **Icons:** Add proper brand icons for Credit/Debit card options (similar to existing GCash/Maya icons).
    - [ ] **Placeholders:** Update "Name on Card" placeholder. Change "FULL NAME" to a standard format (e.g., "John Doe").

- [ ] **Checkout Success Experience**
    - [ ] **Modal Implementation:** Replace the plain text *"Thank you for your purchase..."* message.
    - [ ] Create a modern **Success Modal/Popup** confirming Premium membership activation.