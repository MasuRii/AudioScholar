# AudioScholar Help Center

Welcome to the AudioScholar Help Center! Here you can find answers to common questions and learn how
to get the most out of AudioScholar.

## Getting Started

* **What is AudioScholar?**
  AudioScholar is a smart application designed to help students transform lecture audio into
  actionable insights. It records lectures, uses AI to generate summaries, and provides customized
  suggestions for related learning materials.
* **How does AudioScholar work?**
  You can record your lectures using the AudioScholar mobile app. The app records the audio, and
  with an internet connection and synchronization enabled, it can upload the recording to our
  server. Our server uses AI (specifically, Google Gemini) to process the audio, transcribe it, and
  generate a summary. You can access your recordings and summaries through both the mobile app and
  the web interface.
* **What devices can I use AudioScholar on?**
  AudioScholar is available as an Android mobile application and a web interface accessible through
  a browser on any device.
* **Do I need an internet connection to use AudioScholar?**
  No, you do not need an internet connection to record lectures. AudioScholar has robust offline
  capabilities, allowing you to record directly on your device. However, an internet connection is
  required for AI processing, cloud synchronization, accessing cloud storage, and receiving
  recommendations.
* **How do I sign up for AudioScholar?**
  You can sign up for AudioScholar by logging in with your Google account or GitHub account.

## Recording Lectures

* **How do I start recording a lecture?**
  Open the AudioScholar mobile app and find the recording function. Tap the record button to begin.
* **Can I record a lecture without an internet connection?**
  Yes, you can record lectures offline. The audio will be stored locally on your device.
* **Where are my recordings stored initially?**
  When you record offline, your recordings are stored locally on your device.
* **How do I synchronize my recordings to the cloud?**
  You can manage cloud synchronization in the application settings. You have the option to choose
  between automatic or manual synchronization. Automatic synchronization will upload your recordings
  to the cloud when an internet connection is available and the app is running. Manual
  synchronization requires you to explicitly trigger the upload.
* **What happens if I lose internet connection during a recording?**
  If you lose internet connection while recording, the recording will continue to be stored locally
  on your device. Cloud synchronization will pause until a connection is re-established and, if
  enabled, triggered.
* **Can I record in the background?**
  Background recording is available for **Logged-In Premium Users** only. If you are a non-logged-in
  user or a logged-in free user, the recording will automatically pause if you switch apps or lock
  the screen.
* **How do I pause or stop a recording?**
  Use the pause and stop buttons within the mobile application's recording interface.
* **Can I provide lecture PowerPoint presentations?**
  Yes, AudioScholar optionally allows you to upload lecture PowerPoint presentations. This can
  significantly improve the quality of the AI analysis and summarization process by providing
  valuable context. Look for the option to upload presentations associated with a lecture within the
  app.

## Summaries and Recommendations

* **How are summaries generated?**
  Once your recording is synchronized to the cloud and processed, AudioScholar uses external AI
  APIs (specifically, Google Gemini) to transcribe the audio and generate a concise summary of the
  lecture content.
* **Can I view summaries offline?**
  If the summary has been generated and synchronized to your device while you had an internet
  connection, you may be able to view it offline. However, initial processing and generation require
  an internet connection.
* **How many summaries can I have?**
  The number of active summaries you can have depends on your user tier:
    * **Non-Logged-In Users:** Limited to **one active summary** at a time. You must delete the
      existing summary to create a new one.
    * **Logged-In Free Users:** Can generate up to **three active summaries** at a time.
    * **Logged-In Premium Users:** Have **unlimited summaries**.
* **What are recommendations?**
  AudioScholar provides recommendations for related learning materials to help you further
  understand the lecture topics.
* **Where do the recommendations come from?**
  The source of recommendations varies based on your user tier:
    * **Non-Logged-In Users:** No access to the recommendation engine.
    * **Logged-In Free Users:** Recommendations are limited to **YouTube** as the source.
    * **Logged-In Premium Users:** Recommendations are drawn from an **expanded range of learning
      resources** beyond YouTube.
* **How can I improve the accuracy of summaries and recommendations?**
  Providing associated lecture PowerPoint presentations can significantly improve the accuracy of
  the AI analysis and the relevance of recommendations.

## Account and Data

* **How do I log in?**
  You can log in using your Google account or GitHub account.
* **What is the difference between non-logged-in, logged-in free, and logged-in premium users?**
  These tiers offer different levels of access to features:
    * **Non-Logged-In Users:** Local storage only, single summary limit, no account features, no
      recommendations.
    * **Logged-In Free Users:** Limited cloud access and synchronization, three summary limit,
      background recording disabled, limited YouTube-only recommendations.
    * **Logged-In Premium Users:** Unlimited cloud access and synchronization, unlimited summaries,
      background recording enabled, expanded recommendations.
* **Where is my data stored?**
  Your recordings and summaries are stored locally on your device. If you are a logged-in user with
  cloud synchronization enabled, they are also stored securely in Firebase Storage in the cloud.
  User data, lecture metadata, summaries, and preferences are stored in Firebase Firestore or
  Realtime Database.
* **How secure is my data?**
  We use industry-standard security measures to protect your data. Cloud storage and user
  authentication are handled through secure Firebase services.
* **Can I delete my data?**
  Yes, you can delete your recordings, summaries, and account data within the application. Refer to
  the application's settings or account management section.
* **What is the difference between automatic and manual synchronization?**
  Automatic synchronization will upload your recordings to the cloud automatically when an internet
  connection is available and the app is running. Manual synchronization requires you to explicitly
  initiate the upload process within the app.

## Troubleshooting

* **My recording stopped unexpectedly.**
  Check if you are a non-logged-in or logged-in free user. Background recording is disabled for
  these tiers and will pause if you switch apps or lock the screen. For premium users, ensure you
  have a stable internet connection if synchronization is enabled, though offline recording should
  continue.
* **My summary is not generating.**
  Ensure you have an active internet connection and that your recording has been successfully
  synchronized to the cloud (if you are a logged-in user). AI processing requires an internet
  connection.
* **I am unable to log in with Google/GitHub.**
  Ensure you have a stable internet connection and that you are providing the correct credentials.
  Check the permissions requested by the OAuth process.
* **I have reached my summary limit.**
  If you are a non-logged-in user, you must delete your existing summary to create a new one. If you
  are a logged-in free user, you can delete existing summaries to stay within your limit of three
  active summaries, or consider upgrading to a premium account for unlimited summaries.

## Contact Us

If you have any other questions or need further assistance, please contact us through the support
options available within the AudioScholar application or website.