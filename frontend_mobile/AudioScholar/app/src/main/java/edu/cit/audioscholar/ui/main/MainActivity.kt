package edu.cit.audioscholar.ui.main

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import com.google.firebase.auth.FirebaseAuth
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import edu.cit.audioscholar.R
import edu.cit.audioscholar.data.remote.dto.UserProfileDto
import edu.cit.audioscholar.service.NAVIGATE_TO_EXTRA
import edu.cit.audioscholar.service.RECORDING_DETAIL_DESTINATION
import edu.cit.audioscholar.service.RECORDING_ID_EXTRA
import edu.cit.audioscholar.service.UPLOAD_SCREEN_VALUE
import edu.cit.audioscholar.service.LIBRARY_CLOUD_DESTINATION
import edu.cit.audioscholar.ui.about.AboutScreen
import edu.cit.audioscholar.ui.auth.*
import edu.cit.audioscholar.ui.details.RecordingDetailsScreen
import edu.cit.audioscholar.ui.library.LibraryScreen
import edu.cit.audioscholar.ui.onboarding.OnboardingScreen
import edu.cit.audioscholar.ui.admin.AdminAnalyticsScreen
import edu.cit.audioscholar.ui.admin.AdminDashboardScreen
import edu.cit.audioscholar.ui.admin.AdminUserListScreen
import edu.cit.audioscholar.ui.profile.EditProfileScreen
import edu.cit.audioscholar.ui.profile.UserProfileScreen
import edu.cit.audioscholar.ui.recording.RecordingScreen
import edu.cit.audioscholar.ui.settings.SettingsViewModel
import edu.cit.audioscholar.ui.settings.ThemeSetting
import edu.cit.audioscholar.ui.theme.AudioScholarTheme
import edu.cit.audioscholar.util.Resource
import edu.cit.audioscholar.domain.repository.AuthRepository
import edu.cit.audioscholar.data.remote.dto.FirebaseTokenRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import edu.cit.audioscholar.ui.subscription.SubscriptionPricingScreen
import edu.cit.audioscholar.ui.subscription.PaymentMethodSelectionScreen
import edu.cit.audioscholar.ui.subscription.CardPaymentDetailsScreen
import edu.cit.audioscholar.ui.subscription.EWalletPaymentDetailsScreen
import java.net.URLDecoder
import java.net.URLEncoder
import androidx.hilt.navigation.compose.hiltViewModel
import edu.cit.audioscholar.ui.subscription.PaymentViewModel
import edu.cit.audioscholar.util.PremiumStatusManager

sealed class Screen(val route: String, val labelResId: Int, val icon: ImageVector? = null) {
    object Onboarding : Screen("onboarding", R.string.nav_onboarding, Icons.Filled.Info)
    object Record : Screen("record", R.string.nav_record, Icons.Filled.Mic)
    object Library : Screen("library", R.string.nav_library, Icons.AutoMirrored.Filled.List)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings)
    object Profile : Screen("profile", R.string.nav_profile, Icons.Filled.AccountCircle)
    object EditProfile : Screen("edit_profile", R.string.nav_edit_profile, Icons.Filled.Edit)
    object About : Screen("about", R.string.nav_about, Icons.Filled.Info)
    object Login : Screen("login?fromLogout={fromLogout}&isFirstLogin={isFirstLogin}", R.string.nav_login, Icons.AutoMirrored.Filled.Login) {
        fun createRoute(fromLogout: Boolean = false, isFirstLogin: Boolean = false) =
            "login?fromLogout=$fromLogout&isFirstLogin=$isFirstLogin"
    }
    object Registration : Screen("registration", R.string.nav_registration, Icons.Filled.PersonAdd)
    object EmailVerification : Screen("email_verification", R.string.nav_email_verification, Icons.Filled.Email)
    object ForgotPassword : Screen("forgot_password", R.string.forgot_password_title, Icons.Filled.Lock)
    object ChangePassword : Screen("change_password", R.string.nav_change_password, Icons.Filled.Password)
    object ResetPasswordConfirm : Screen("reset_password_confirm?oobCode={oobCode}", R.string.nav_change_password, Icons.Filled.Lock) {
        fun createRoute(code: String) = "reset_password_confirm?oobCode=$code"
    }
    object SubscriptionPricing : Screen("subscription_pricing", R.string.nav_audioscholar_pro, Icons.Filled.School)

    object AdminDashboard : Screen("admin_dashboard", R.string.nav_admin_dashboard, Icons.Filled.Dashboard)
    object AdminUserManagement : Screen("admin_user_management", R.string.nav_admin_user_management, Icons.Filled.People)
    object AdminAnalytics : Screen("admin_analytics", R.string.nav_admin_analytics, Icons.Filled.Analytics)

    companion object {
        const val ARG_PLAN_ID = "planId"
        const val ARG_FORMATTED_PRICE = "formattedPrice"
        const val ARG_PRICE_AMOUNT = "priceAmount"
    }

    object PaymentMethodSelection : Screen(
        route = "payment_method_selection/{$ARG_PLAN_ID}/{$ARG_FORMATTED_PRICE}/{$ARG_PRICE_AMOUNT}",
        labelResId = R.string.nav_payment_method_selection
    ) {
        fun createRoute(planId: String, formattedPrice: String, priceAmount: String): String {
            val encodedFormattedPrice = URLEncoder.encode(formattedPrice, "UTF-8")
            return "payment_method_selection/$planId/$encodedFormattedPrice/$priceAmount"
        }
    }

    object CardPaymentDetails : Screen(
        route = "card_payment_details/{$ARG_PLAN_ID}/{$ARG_FORMATTED_PRICE}/{$ARG_PRICE_AMOUNT}",
        labelResId = R.string.nav_card_payment_details
    ) {
        fun createRoute(planId: String, formattedPrice: String, priceAmount: String): String {
            val encodedFormattedPrice = URLEncoder.encode(formattedPrice, "UTF-8")
            return "card_payment_details/$planId/$encodedFormattedPrice/$priceAmount"
        }
    }

    object EWalletPaymentDetails : Screen(
        route = "ewallet_payment_details/{$ARG_PLAN_ID}/{$ARG_FORMATTED_PRICE}/{$ARG_PRICE_AMOUNT}",
        labelResId = R.string.nav_ewallet_payment_details
    ) {
        fun createRoute(planId: String, formattedPrice: String, priceAmount: String): String {
            val encodedFormattedPrice = URLEncoder.encode(formattedPrice, "UTF-8")
            return "ewallet_payment_details/$planId/$encodedFormattedPrice/$priceAmount"
        }
    }

    object RecordingDetails : Screen("recording_details", R.string.nav_recording_details) {
        const val ARG_LOCAL_FILE_PATH = "localFilePath"
        const val ARG_CLOUD_ID = "cloudId"
        const val ARG_CLOUD_RECORDING_ID = "cloudRecordingId"
        const val ARG_CLOUD_TITLE = "cloudTitle"
        const val ARG_CLOUD_FILENAME = "cloudFileName"
        const val ARG_CLOUD_TIMESTAMP_SECONDS = "cloudTimestampSeconds"
        const val ARG_CLOUD_STORAGE_URL = "cloudStorageUrl"
        const val ARG_CLOUD_AUDIO_URL = "cloudAudioUrl"
        const val ARG_CLOUD_PDF_URL = "cloudPdfUrl"

        const val ROUTE_PATTERN = "recording_details" +
                "?$ARG_LOCAL_FILE_PATH={$ARG_LOCAL_FILE_PATH}" +
                "&$ARG_CLOUD_ID={$ARG_CLOUD_ID}" +
                "&$ARG_CLOUD_RECORDING_ID={$ARG_CLOUD_RECORDING_ID}" +
                "&$ARG_CLOUD_TITLE={$ARG_CLOUD_TITLE}" +
                "&$ARG_CLOUD_FILENAME={$ARG_CLOUD_FILENAME}" +
                "&$ARG_CLOUD_TIMESTAMP_SECONDS={$ARG_CLOUD_TIMESTAMP_SECONDS}" +
                "&$ARG_CLOUD_STORAGE_URL={$ARG_CLOUD_STORAGE_URL}" +
                "&$ARG_CLOUD_AUDIO_URL={$ARG_CLOUD_AUDIO_URL}" +
                "&$ARG_CLOUD_PDF_URL={$ARG_CLOUD_PDF_URL}"

        fun createLocalRoute(filePath: String): String {
            return "recording_details?$ARG_LOCAL_FILE_PATH=${Uri.encode(filePath)}"
        }

        fun createCloudRoute(
            id: String,
            recordingId: String,
            title: String?,
            fileName: String?,
            timestampSeconds: Long?,
            storageUrl: String?,
            audioUrl: String? = null,
            pdfUrl: String? = null
        ): String {
            if (id.isBlank()) {
                Log.e("Screen.RecordingDetails", "Cannot create cloud route, primary 'id' is null or blank.")
                return "recording_details/error"
            }

            val encodedTitle = Uri.encode(title ?: fileName ?: "Cloud Recording")
            val encodedFileName = Uri.encode(fileName ?: "Unknown Filename")
            val timestamp = timestampSeconds ?: 0L
            val encodedStorageUrl = Uri.encode(storageUrl ?: "")
            val encodedAudioUrl = Uri.encode(audioUrl ?: "")
            val encodedPdfUrl = Uri.encode(pdfUrl ?: "")

            return "recording_details?$ARG_CLOUD_ID=$id" +
                    "&$ARG_CLOUD_RECORDING_ID=$recordingId" +
                    "&$ARG_CLOUD_TITLE=$encodedTitle" +
                    "&$ARG_CLOUD_FILENAME=$encodedFileName" +
                    "&$ARG_CLOUD_TIMESTAMP_SECONDS=$timestamp" +
                    "&$ARG_CLOUD_STORAGE_URL=$encodedStorageUrl" +
                    "&$ARG_CLOUD_AUDIO_URL=$encodedAudioUrl" +
                    "&$ARG_CLOUD_PDF_URL=$encodedPdfUrl"
        }
    }
}

val screensWithDrawer = listOf(
    Screen.Record.route,
    Screen.Library.route,
    Screen.Settings.route,
    Screen.Profile.route,
    Screen.About.route,
    Screen.ChangePassword.route,
    Screen.SubscriptionPricing.route
)


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefs: SharedPreferences

    @Inject
    lateinit var authRepository: AuthRepository

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val loginViewModel: LoginViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var navController: NavHostController

    private val onOnboardingCompleteAction: () -> Unit = {
        if (::navController.isInitialized) {
            navController.navigate(Screen.Login.createRoute(isFirstLogin = true)) {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
                launchSingleTop = true
            }
            Log.d("MainActivity", "Onboarding complete. Navigating to Login screen (isFirstLogin=true).")
        } else {
            Log.e("MainActivity", "Onboarding complete called but NavController not ready.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called. Intent received: $intent")
        logIntentExtras("onCreate", intent)

        if (!handleDeepLink(intent)) {
            handleGitHubRedirectIntent(intent, "onCreate")
        }

        val startDestination = intent?.getStringExtra(SplashActivity.EXTRA_START_DESTINATION)
            ?: run {
                Log.w("MainActivity", "Missing EXTRA_START_DESTINATION from SplashActivity! Using fallback logic.")
                val isOnboardingComplete = prefs.getBoolean(SplashActivity.KEY_ONBOARDING_COMPLETE, false)
                val isLoggedIn = prefs.getBoolean(SplashActivity.KEY_IS_LOGGED_IN, false)
                when {
                    !isOnboardingComplete -> Screen.Onboarding.route
                    !isLoggedIn -> Screen.Login.route
                    else -> Screen.Record.route
                }
            }
        Log.d("MainActivity", "Using start destination: $startDestination")

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.gitHubLoginCompleteSignal.collectLatest {
                    Log.i("MainActivity", "[GitHubNavCollector] Collected gitHubLoginCompleteSignal.")
                    if (::navController.isInitialized) {
                        val currentRoute = navController.currentDestination?.route
                        if (currentRoute == Screen.Login.route || currentRoute == Screen.Registration.route) {
                            Log.d("MainActivity", "[GitHubNavCollector] Navigating from Login to Record.")
                            navController.navigate(Screen.Record.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            Log.w("MainActivity", "[GitHubNavCollector] Signal received, but not on Login screen (current: $currentRoute). Skipping navigation.")
                        }
                    } else {
                        Log.e("MainActivity", "[GitHubNavCollector] Signal received, but NavController not initialized!")
                    }
                }
            }
            Log.d("MainActivity", "[GitHubNavCollector] Collection stopped.")
        }

        setContent {
            val themeSetting by settingsViewModel.selectedTheme.collectAsStateWithLifecycle()
            val systemIsDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeSetting) {
                ThemeSetting.Light -> false
                ThemeSetting.Dark -> true
                ThemeSetting.System -> systemIsDark
            }

            AudioScholarTheme(darkTheme = useDarkTheme) {
                navController = rememberNavController()

                LaunchedEffect(intent) {
                    Log.d("MainActivity", "LaunchedEffect in setContent triggered for initial intent.")
                    if (intent?.action != Intent.ACTION_VIEW || intent.data?.scheme != LoginViewModel.GITHUB_REDIRECT_URI_SCHEME) {
                        handleNavigationIntent(intent, navController)
                    }
                }


                MainAppScreen(
                    navController = navController,
                    startDestination = startDestination,
                    onOnboardingComplete = onOnboardingCompleteAction,
                    loginViewModel = loginViewModel,
                    mainViewModel = mainViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent called. Intent received: $intent")
        logIntentExtras("onNewIntent", intent)

        val currentIntentDataString = getIntent()?.dataString
        val newIntentDataString = if (intent.action == Intent.ACTION_VIEW) intent.dataString else null
        if (newIntentDataString != null && newIntentDataString == currentIntentDataString) {
            Log.w("MainActivity", "onNewIntent: Ignoring duplicate delivery of the same intent data: $newIntentDataString")
            setIntent(intent)
            return
        }

        setIntent(intent)

        if (::navController.isInitialized) {
            if (!handleDeepLink(intent)) {
                if (!handleGitHubRedirectIntent(intent, "onNewIntent")) {
                    handleNavigationIntent(intent, navController)
                }
            }
        } else {
            Log.e("MainActivity", "onNewIntent called but navController not initialized yet.")
        }
    }

    private fun handleDeepLink(intent: Intent?): Boolean {
        val data = intent?.data ?: return false
        Log.d("MainActivity", "handleDeepLink: Checking intent data: $data")

        val supportedHosts = listOf("audioscholar.page.link", "audioscholar-39b22.web.app", "audioscholar-39b22.firebaseapp.com")
        if (data.scheme != "https" || !supportedHosts.contains(data.host)) {
            return false
        }

        try {
            var oobCode = data.getQueryParameter("oobCode")
            // If oobCode is not directly in the URL, check if it's nested in a 'link' parameter (Firebase behavior)
            if (oobCode.isNullOrBlank()) {
                val linkParam = data.getQueryParameter("link")
                if (!linkParam.isNullOrBlank()) {
                    Log.d("MainActivity", "oobCode not found in top level, checking nested 'link' param: $linkParam")
                    val linkUri = Uri.parse(linkParam)
                    oobCode = linkUri.getQueryParameter("oobCode")
                }
            }

            val mode = data.getQueryParameter("mode")
            val path = data.path

            if (oobCode.isNullOrBlank()) {
                // Fallback Logic for State Mismatch: Browser might have already verified the user.
                if (data.toString().contains("continueUrl") || data.path?.contains("/verify") == true) {
                    Log.d("MainActivity", "oobCode missing. Attempting to reload user profile to check verification status.")
                    val user = FirebaseAuth.getInstance().currentUser
                    user?.reload()?.addOnCompleteListener { task ->
                        if (task.isSuccessful && user.isEmailVerified) {
                            Log.d("MainActivity", "User is verified after reload. Refreshing token.")
                            user.getIdToken(true).addOnCompleteListener { tokenTask ->
                                if (tokenTask.isSuccessful) {
                                    val firebaseToken = tokenTask.result?.token
                                    if (firebaseToken != null) {
                                        lifecycleScope.launch {
                                            val result = authRepository.verifyFirebaseToken(FirebaseTokenRequest(firebaseToken))
                                            when (result) {
                                                is Resource.Success -> {
                                                    Log.d("MainActivity", "Token exchanged and saved. Signing out and redirecting to Login.")
                                                    Toast.makeText(this@MainActivity, "Email verified successfully! Redirecting to login in 3 seconds...", Toast.LENGTH_LONG).show()
                                                    lifecycleScope.launch {
                                                        delay(3000)
                                                        FirebaseAuth.getInstance().signOut()
                                                        mainViewModel.performLogout()
                                                    }
                                                }
                                                is Resource.Error -> {
                                                    Log.e("MainActivity", "Token exchange failed: ${result.message}")
                                                    Toast.makeText(this@MainActivity, "Verification successful, but login failed: ${result.message}", Toast.LENGTH_LONG).show()
                                                }
                                                else -> {}
                                            }
                                        }
                                    }
                                } else {
                                    Log.e("MainActivity", "Failed to refresh token: ${tokenTask.exception?.message}")
                                }
                            }
                        } else {
                            Log.w("MainActivity", "Reload failed or user still not verified.")
                        }
                    }
                    return true // Handled as a fallback check
                }

                Log.w("MainActivity", "Deep link received but oobCode is missing or blank.")
                Toast.makeText(this, "Invalid link: Action code is missing.", Toast.LENGTH_LONG).show()
                return false // Considered handled as it's a known invalid link format
            }

            Log.d("MainActivity", "Deep link path: $path, mode: $mode, oobCode present.")

            // Check mode in the nested link as well if needed, or rely on the code
            val isVerifyEmail = mode == "verifyEmail" || path?.startsWith("/verify") == true
            val isResetPassword = mode == "resetPassword" || path?.startsWith("/resetPassword") == true

            when {
                isVerifyEmail -> {
                    Log.i("MainActivity", "Handling email verification deep link.")
                    FirebaseAuth.getInstance().applyActionCode(oobCode)
                        .addOnSuccessListener {
                            Log.d("MainActivity", "Email verified. Reloading user to refresh token.")
                            val user = FirebaseAuth.getInstance().currentUser
                            user?.reload()?.addOnCompleteListener { reloadTask ->
                                if (reloadTask.isSuccessful) {
                                    user.getIdToken(true).addOnCompleteListener { tokenTask ->
                                        if (tokenTask.isSuccessful) {
                                            val firebaseToken = tokenTask.result?.token
                                            if (firebaseToken != null) {
                                                lifecycleScope.launch {
                                                    val result = authRepository.verifyFirebaseToken(FirebaseTokenRequest(firebaseToken))
                                                    when (result) {
                                                        is Resource.Success -> {
                                                            Log.d("MainActivity", "Token exchanged and saved. Signing out and redirecting to Login.")
                                                            Toast.makeText(this@MainActivity, "Email verified successfully! Redirecting to login in 3 seconds...", Toast.LENGTH_LONG).show()
                                                            lifecycleScope.launch {
                                                                delay(3000)
                                                                FirebaseAuth.getInstance().signOut()
                                                                mainViewModel.performLogout()
                                                            }
                                                        }
                                                        is Resource.Error -> {
                                                            Log.e("MainActivity", "Token exchange failed: ${result.message}")
                                                            Toast.makeText(this@MainActivity, "Verified, but login failed: ${result.message}", Toast.LENGTH_LONG).show()
                                                        }
                                                        else -> {}
                                                    }
                                                }
                                            }
                                        } else {
                                            Log.e("MainActivity", "Failed to get fresh token: ${tokenTask.exception?.message}")
                                            Toast.makeText(this@MainActivity, "Verified, but failed to refresh session. Please login manually.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            val errorMessage = e.message ?: "An unknown error occurred."
                            Log.e("MainActivity", "Email verification failed: $errorMessage", e)
                            Toast.makeText(this, "Verification failed: $errorMessage", Toast.LENGTH_LONG).show()
                        }
                    return true
                }
                isResetPassword -> {
                    Log.i("MainActivity", "Handling password reset deep link.")
                    FirebaseAuth.getInstance().verifyPasswordResetCode(oobCode)
                        .addOnSuccessListener { email ->
                            Log.i("MainActivity", "Password reset code verified for $email. Navigating to confirm screen.")
                            if (::navController.isInitialized) {
                                navController.navigate(Screen.ResetPasswordConfirm.createRoute(oobCode)) {
                                    launchSingleTop = true
                                }
                            } else {
                                Log.e("MainActivity", "NavController not initialized, cannot navigate to reset password.")
                                Toast.makeText(this, "Could not open reset screen. Please restart the app.", Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            val errorMessage = e.message ?: "Invalid or expired link."
                            Log.e("MainActivity", "Invalid password reset code: $errorMessage", e)
                            Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                        }
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Unexpected error while handling deep link: ${e.message}", e)
            Toast.makeText(this, "Error processing link. It may be malformed.", Toast.LENGTH_LONG).show()
            return true
        }
        return false
    }

    private fun handleGitHubRedirectIntent(intent: Intent?, source: String): Boolean {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data
            if (uri != null &&
                uri.scheme == LoginViewModel.GITHUB_REDIRECT_URI_SCHEME &&
                uri.host == LoginViewModel.GITHUB_REDIRECT_URI_HOST)
            {
                Log.i("MainActivity", "[$source] Received GitHub OAuth Redirect URI: $uri")
                val code = uri.getQueryParameter("code")
                val state = uri.getQueryParameter("state")
                loginViewModel.handleGitHubRedirect(code, state)
                return true
            }
        }
        return false
    }

    @Suppress("DEPRECATION")
    private fun logIntentExtras(source: String, intent: Intent?) {
        if (intent == null) {
            Log.d("MainActivity", "[$source] Intent is null.")
            return
        }
        Log.d("MainActivity", "[$source] Intent Action: ${intent.action}")
        Log.d("MainActivity", "[$source] Intent Data: ${intent.dataString}")
        intent.extras?.let { bundle ->
            Log.d("MainActivity", "[$source] Intent extras:")
            for (key in bundle.keySet()) {
                val valueString = bundle.get(key)?.toString() ?: "null"
                Log.d("MainActivity", "  Key=$key, Value=${valueString.take(100)}${if (valueString.length > 100) "..." else ""}")
            }
        } ?: Log.d("MainActivity", "[$source] Intent has no extras.")
    }

    private fun handleNavigationIntent(intent: Intent?, navController: NavHostController) {
        Log.d("MainActivity", "[handleNavigationIntent] Checking intent for navigation...")
        logIntentExtras("handleNavigationIntent", intent)

        if (intent == null) return

        if (intent.action == Intent.ACTION_VIEW && intent.data?.scheme == LoginViewModel.GITHUB_REDIRECT_URI_SCHEME) {
            Log.d("MainActivity", "[handleNavigationIntent] Intent was GitHub redirect, skipping navigation handling here.")
            return
        }

        val navigateTo = intent.getStringExtra(NAVIGATE_TO_EXTRA)
        Log.d("MainActivity", "[handleNavigationIntent] Value from getExtra(NAVIGATE_TO_EXTRA): $navigateTo")

        when (navigateTo) {
            RECORDING_DETAIL_DESTINATION -> {
                val recordingId = intent.getStringExtra(RECORDING_ID_EXTRA)
                Log.i("MainActivity", "[handleNavigationIntent] Intent requests navigation to Recording Details for recordingId: $recordingId")
                if (!recordingId.isNullOrBlank()) {
                    val route = Screen.RecordingDetails.createCloudRoute(
                        id = recordingId,
                        recordingId = recordingId,
                        title = null,
                        fileName = null,
                        timestampSeconds = null,
                        storageUrl = null
                    )
                    if (route != "recording_details/error") {
                         Log.d("MainActivity", "[handleNavigationIntent] Navigating to: $route")
                         navController.navigate(route) {
                             launchSingleTop = true
                         }
                    } else {
                        Log.e("MainActivity", "[handleNavigationIntent] Failed to create route for recording details.")
                    }
                } else {
                    Log.w("MainActivity", "[handleNavigationIntent] Destination is RECORDING_DETAIL, but recordingId extra is missing or blank.")
                }
                intent.removeExtra(NAVIGATE_TO_EXTRA)
                intent.removeExtra(RECORDING_ID_EXTRA)
            }
            UPLOAD_SCREEN_VALUE -> {
                Log.w("MainActivity", "[handleNavigationIntent] Intent requested navigation to removed Upload screen. Ignoring.")
                intent.removeExtra(NAVIGATE_TO_EXTRA)
            }
            LIBRARY_CLOUD_DESTINATION -> {
                Log.i("MainActivity", "[handleNavigationIntent] Intent requests navigation to Library (Cloud). Navigating...")
                navController.navigate(Screen.Library.route) {
                    launchSingleTop = true
                }
                intent.removeExtra(NAVIGATE_TO_EXTRA)
                intent.removeExtra(RECORDING_ID_EXTRA)
            }
            null -> {
                Log.d("MainActivity", "[handleNavigationIntent] No specific navigation target in intent extras (NAVIGATE_TO_EXTRA is null)." )
            }
            else -> {
                Log.d("MainActivity", "[handleNavigationIntent] Received unhandled navigation target: $navigateTo")
                intent.removeExtra(NAVIGATE_TO_EXTRA)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    navController: NavHostController,
    startDestination: String,
    onOnboardingComplete: () -> Unit,
    loginViewModel: LoginViewModel,
    mainViewModel: MainViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val gesturesEnabled = currentRoute in screensWithDrawer

    val userProfileState by mainViewModel.userProfileState.collectAsStateWithLifecycle()
    
    val isPremium = mainViewModel.isPremiumUser

    val userProfile: UserProfileDto? = if (userProfileState is Resource.Success) {
        userProfileState.data
    } else {
        (userProfileState as? Resource.Loading)?.data ?: (userProfileState as? Resource.Error)?.data
    }

    LaunchedEffect(Unit) {
        mainViewModel.logoutCompleteEventFlow.collectLatest {
            Log.d("MainAppScreen", "Logout complete event received. Navigating to Login with fromLogout=true.")
            navController.navigate(Screen.Login.createRoute(fromLogout = true, isFirstLogin = false)) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }


    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(id = R.drawable.ic_navigation_logo), null, Modifier.size(84.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(id = R.string.app_name), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { drawerState.close() }
                                if (currentRoute != Screen.Profile.route) {
                                    navController.navigate(Screen.Profile.route) {
                                        popUpTo(navController.graph.id) { saveState = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Box {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(userProfile?.profileImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = stringResource(R.string.desc_user_avatar),
                                placeholder = painterResource(id = R.drawable.avatar_placeholder),
                                error = painterResource(id = R.drawable.avatar_placeholder),
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentScale = ContentScale.Crop
                            )
                            
                            if (isPremium) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(24.dp)
                                        .offset(x = 4.dp, y = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = stringResource(R.string.premium_badge_description),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(16.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = userProfile?.displayName ?: stringResource(R.string.placeholder_username),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = userProfile?.email ?: stringResource(R.string.placeholder_email),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (isPremium) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.premium_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()

                val navItems = listOf(
                    Screen.Profile,
                    Screen.SubscriptionPricing,
                    Screen.Record,
                    Screen.Library,
                    Screen.Settings,
                    Screen.About
                )

                Spacer(Modifier.height(12.dp))
                
                navItems.forEach { screen ->
                    screen.icon?.let { icon ->
                        NavigationDrawerItem(
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(stringResource(screen.labelResId)) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.id) { saveState = true }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                
                Spacer(Modifier.weight(1f))
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_logout)) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            Log.d("DrawerFooter", "Logout clicked - Calling ViewModel performLogout.")
                            mainViewModel.performLogout()
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    ) { Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    modifier = Modifier.fillMaxSize(),
                    onOnboardingComplete = onOnboardingComplete
                )
            }
            composable(Screen.Record.route) {
                RecordingScreen(
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    navController = navController,
                    drawerState = drawerState,
                )
            }
            composable(Screen.Settings.route) {
                edu.cit.audioscholar.ui.settings.SettingsScreen(
                    drawerState = drawerState,
                    scope = scope
                )
            }
            composable(Screen.Profile.route) {
                UserProfileScreen(
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope
                )
            }
            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    navController = navController
                )
            }
            composable(Screen.About.route) {
                AboutScreen(
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope
                )
            }
            composable(Screen.AdminDashboard.route) {
                AdminDashboardScreen(navController = navController)
            }
            composable(Screen.AdminUserManagement.route) {
                AdminUserListScreen(navController = navController)
            }
            composable(Screen.AdminAnalytics.route) {
                AdminAnalyticsScreen(navController = navController)
            }
            composable(
                route = Screen.Login.route,
                arguments = listOf(
                    navArgument("fromLogout") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("isFirstLogin") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) {
                LoginScreen(navController = navController)
            }
            composable(Screen.Registration.route) {
                RegistrationScreen(navController = navController)
            }
            composable(Screen.EmailVerification.route) {
                EmailVerificationScreen(navController = navController)
            }
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(navController = navController)
            }
            composable(Screen.ChangePassword.route) {
                edu.cit.audioscholar.ui.settings.ChangePasswordScreen(
                    navController = navController
                )
            }
            composable(
                route = Screen.ResetPasswordConfirm.route,
                arguments = listOf(
                    navArgument("oobCode") { type = NavType.StringType }
                )
            ) {
                edu.cit.audioscholar.ui.auth.ResetPasswordConfirmScreen(
                    navController = navController
                )
            }
            composable(
                route = Screen.RecordingDetails.ROUTE_PATTERN,
                arguments = listOf(
                    navArgument(Screen.RecordingDetails.ARG_LOCAL_FILE_PATH) {
                        type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Screen.RecordingDetails.ARG_CLOUD_ID) {
                        type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Screen.RecordingDetails.ARG_CLOUD_RECORDING_ID) {
                        type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Screen.RecordingDetails.ARG_CLOUD_TITLE) {
                        type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Screen.RecordingDetails.ARG_CLOUD_FILENAME) {
                        type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Screen.RecordingDetails.ARG_CLOUD_TIMESTAMP_SECONDS) {
                        type = NavType.LongType; defaultValue = 0L },
                    navArgument(Screen.RecordingDetails.ARG_CLOUD_STORAGE_URL) {
                        type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Screen.RecordingDetails.ARG_CLOUD_AUDIO_URL) {
                        type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Screen.RecordingDetails.ARG_CLOUD_PDF_URL) {
                        type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                RecordingDetailsScreen(navController = navController)
            }
            composable("recording_details/error") {
                Text("Error: Could not navigate to recording details.")
                LaunchedEffect(Unit) {
                    delay(2000)
                    navController.popBackStack()
                }
            }
            composable(Screen.SubscriptionPricing.route) {
                SubscriptionPricingScreen(navController = navController, drawerState = drawerState, scope = scope)
            }
            composable(
                route = Screen.PaymentMethodSelection.route,
                arguments = listOf(
                    navArgument(Screen.ARG_PLAN_ID) { type = NavType.StringType },
                    navArgument(Screen.ARG_FORMATTED_PRICE) { type = NavType.StringType },
                    navArgument(Screen.ARG_PRICE_AMOUNT) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val planId = backStackEntry.arguments?.getString(Screen.ARG_PLAN_ID) ?: ""
                val formattedPrice = URLDecoder.decode(backStackEntry.arguments?.getString(Screen.ARG_FORMATTED_PRICE) ?: "", "UTF-8")
                val priceAmount = backStackEntry.arguments?.getString(Screen.ARG_PRICE_AMOUNT)?.toDoubleOrNull() ?: 0.0
                val viewModel: PaymentViewModel = hiltViewModel()
                PaymentMethodSelectionScreen(
                    navController = navController,
                    planId = planId,
                    formattedPrice = formattedPrice,
                    priceAmount = priceAmount
                )
            }
            composable(
                route = Screen.CardPaymentDetails.route,
                arguments = listOf(
                    navArgument(Screen.ARG_PLAN_ID) { type = NavType.StringType },
                    navArgument(Screen.ARG_FORMATTED_PRICE) { type = NavType.StringType },
                    navArgument(Screen.ARG_PRICE_AMOUNT) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val formattedPrice = URLDecoder.decode(backStackEntry.arguments?.getString(Screen.ARG_FORMATTED_PRICE) ?: "", "UTF-8")
                val priceAmount = backStackEntry.arguments?.getString(Screen.ARG_PRICE_AMOUNT)?.toDoubleOrNull() ?: 0.0
                val viewModel: PaymentViewModel = hiltViewModel()
                CardPaymentDetailsScreen(
                    navController = navController,
                    formattedPrice = formattedPrice,
                    priceAmount = priceAmount,
                    authRepository = viewModel.authRepository
                )
            }
            composable(
                route = Screen.EWalletPaymentDetails.route,
                arguments = listOf(
                    navArgument(Screen.ARG_PLAN_ID) { type = NavType.StringType },
                    navArgument(Screen.ARG_FORMATTED_PRICE) { type = NavType.StringType },
                    navArgument(Screen.ARG_PRICE_AMOUNT) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val formattedPrice = URLDecoder.decode(backStackEntry.arguments?.getString(Screen.ARG_FORMATTED_PRICE) ?: "", "UTF-8")
                val priceAmount = backStackEntry.arguments?.getString(Screen.ARG_PRICE_AMOUNT)?.toDoubleOrNull() ?: 0.0
                val viewModel: PaymentViewModel = hiltViewModel()
                EWalletPaymentDetailsScreen(
                    navController = navController,
                    formattedPrice = formattedPrice,
                    priceAmount = priceAmount,
                    authRepository = viewModel.authRepository
                )
            }
        }
    }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
}