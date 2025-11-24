# AudioScholar Development Task List

## 1. Landing Page Improvements
- [x] **Testimonials Carousel:**
    - Convert the "Focus More, Study Smarter" reviews section into a horizontal carousel.
    - **Requirement:** Add at least 5 examples.
    - **Localization:** Use fictional names with universities based in Cebu/Philippines.
    - *Sample Data:*
        1. *Juan dela Cruz* - University of San Carlos (USC)
        2. *Maria Santos* - Cebu Institute of Technology - University (CIT-U)
        3. *Miguel Reyes* - University of Cebu (UC)
        4. *Angela Lim* - University of the Philippines Cebu (UP Cebu)
        5. *Rico Tan* - University of San Jose-Recoletos (USJ-R)
- [x] **Input Label Update:**
    - Remove the text "(Optional)" from the "PowerPoint Context" label. This field is now mandatory.

## 2. Authentication & Security (Sign In/Up)
- [x] **UI/Theming Fixes (Light Mode):**
    - Fix opacity/visibility issues for "Previous" and "Next" buttons in Light Mode (currently invisible/white-on-white).
    - Adjust button color to a "lighter dark" shade instead of full black `#000000`.
- [x] **Error Handling:**
    - Replace raw Firebase errors (e.g., `Firebase: Error (auth/invalid-credential).`) with user-friendly messages (e.g., "Incorrect email or password. Please try again.").
    - Ensure all error states are clearly communicated to the user.
- [x] **Loading States:**
    - Add a visual loading indicator (spinner/skeleton) when OAuth (Google) login is processing. The UI must not appear frozen while the backend handshake occurs.
- [x] **Custom Action Handlers (Reset Password & Verify Email):**
    - **Bug:** Forgot Password currently redirects to the default Firebase template (`/__/auth/action...`).
    - **Task:** Implement custom React pages to handle Firebase Auth actions.
    - **Reference:** [Firebase Custom Action Handlers Guide](https://support.google.com/firebase/answer/7000714)
    - Ensure the link sent to email redirects to the application domain, not the Firebase app domain.

## 3. Global UI & Theming
- [x] **Dark/Light Mode Implementation:**
    - Implement a global theme toggle (complimentary to app design).
    - **Logic:**
        - Default: Detect System Preference.
        - Manual: User toggle overrides system.
    - Apply consistently across **all** pages.

## 4. Feature: Recordings & Dashboard
- [x] **Search & Filter:**
    - Implement a search bar for the "My Recordings" list.
- [x] **Grouping:**
    - Group recordings chronologically: "Today", "This Week", "This Month", "Older".

## 5. Feature: Recording Details Page
- [x] **User Notes (Frontend Implementation):**
    - Add a "My Notes" tab next to Summary/Transcript.
    - **Editor:** Integrate a Markdown text editor with a Preview mode.
    - **Constraint:** Implementation is currently *Frontend Only* (local state/storage) until the backend API is ready.
- [x] **Layout Standardization:**
    - Move the "Download" button to a standard corner position to improve layout consistency.

## 6. Feature: User Profile
- [x] **Header Layout:**
    - Display User Avatar/Profile Picture alongside the Username in the top navigation/header.
    - Ensure layout aligns with standard web design patterns.

## 7. Payment & Checkout Flow
- [x] **Payment Page UI:**
    - Add standard icons for "Credit/Debit Card" input fields.
    - **Validation:**
        - `Name on Card`: Auto-capitalize input.
        - `Expiry Date`: Block input if the date is in the past.
- [x] **Checkout Success:**
    - Refactor the success message.
    - Remove generic/localhost phrasing ("Subscription successful!").
    - Replace with a professional, standard confirmation message (e.g., "Thank you for your purchase! Your Premium membership is now active.").