package edu.cit.audioscholar.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import edu.cit.audioscholar.BuildConfig
import edu.cit.audioscholar.data.local.UserDataStore
import edu.cit.audioscholar.data.local.file.RecordingFileHandler
import edu.cit.audioscholar.data.remote.service.ApiService
import edu.cit.audioscholar.domain.repository.*
import edu.cit.audioscholar.network.ServerConnectionManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.*

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val PREFS_NAME = "AudioScholarPrefs"
    private const val TAG = "NetworkModule"

    @Singleton
    class AuthInterceptor @Inject constructor(
        private val prefs: SharedPreferences
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val token = prefs.getString("auth_token", null)
            val originalRequest = chain.request()

            val requestBuilder = originalRequest.newBuilder()
            if (!token.isNullOrBlank()) {
                Log.d(TAG, "AuthInterceptor: Adding Authorization header")
                requestBuilder.addHeader("Authorization", "Bearer $token")
            } else {
                Log.d(TAG, "AuthInterceptor: No token found, proceeding without Authorization header")
            }

            val request = requestBuilder.build()
            return chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        Log.d(TAG, "Providing OkHttpClient instance")
        return OkHttpClient.Builder()
            .addInterceptor(DynamicBaseUrlInterceptor())
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        val dynamicUrl = ServerConnectionManager.currentBaseUrl
        Log.d(TAG, "Providing Retrofit instance with base URL: $dynamicUrl")
        return Retrofit.Builder()
            .baseUrl(dynamicUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        apiService: ApiService,
        application: Application,
        gson: Gson,
        userDataStore: UserDataStore,
        firebaseAuth: FirebaseAuth,
        prefs: SharedPreferences
    ): AuthRepository {
        return AuthRepositoryImpl(apiService, application, gson, userDataStore, firebaseAuth, prefs)
    }

    @Provides
    @Singleton
    fun provideRecordingFileHandler(
        @ApplicationContext context: Context,
        @Named(PreferencesModule.SETTINGS_PREFERENCES) prefs: SharedPreferences
    ): RecordingFileHandler {
        return RecordingFileHandler(context, prefs)
    }

    @Provides
    @Singleton
    fun provideLocalAudioRepository(
        @ApplicationContext context: Context,
        application: Application,
        gson: Gson,
        recordingFileHandler: RecordingFileHandler
    ): LocalAudioRepository {
        return LocalAudioRepositoryImpl(context, application, gson, recordingFileHandler)
    }

    @Provides
    @Singleton
    fun provideRemoteAudioRepository(
        apiService: ApiService,
        application: Application,
        gson: Gson
    ): RemoteAudioRepository {
        return RemoteAudioRepositoryImpl(apiService, application, gson)
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideUserDataStore(@ApplicationContext context: Context): UserDataStore {
        return UserDataStore(context)
    }

    @Provides
    @Singleton
    fun provideKtorHttpClient(prefs: SharedPreferences): HttpClient {
        Log.d(TAG, "Creating Ktor HttpClient instance.")
        return HttpClient(OkHttp) {

            engine {
            }

            defaultRequest {
                 val dynamicUrl = ServerConnectionManager.currentBaseUrl
                 url(dynamicUrl)
                 Log.d("$TAG-Ktor", "Applying defaultRequest with base URL: $dynamicUrl")
            }

            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
                Log.d("$TAG-Ktor", "Installed ContentNegotiation with Kotlinx JSON.")
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.v("$TAG-Ktor-Logger", message)
                    }
                }
                level = LogLevel.ALL
                 Log.d("$TAG-Ktor", "Installed Ktor Logging plugin.")
            }

            install(Auth) {
                 bearer {
                     loadTokens {
                         val token = prefs.getString("auth_token", null)
                         if (!token.isNullOrBlank()) {
                             Log.d("$TAG-Ktor", "Auth Plugin: Loading token from prefs.")
                             BearerTokens(token, "")
                         } else {
                             Log.d("$TAG-Ktor", "Auth Plugin: No token found in prefs.")
                             null
                         }
                     }

                     sendWithoutRequest { request ->
                         val path = request.url.encodedPath
                         val shouldNotSend = path.startsWith("/api/auth/")
                         Log.d("$TAG-Ktor", "Auth Plugin: Checking sendWithoutRequest for path: $path. Should NOT send: $shouldNotSend")
                         shouldNotSend
                     }
                 }
                 Log.d("$TAG-Ktor", "Installed Ktor Auth plugin with Bearer provider.")
            }
            Log.i("$TAG-Ktor", "Ktor HttpClient configuration complete.")
        }
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        httpClient: HttpClient,
        prefs: SharedPreferences
    ): NotificationRepository {
         Log.d(TAG, "Providing NotificationRepository implementation.")
         return edu.cit.audioscholar.domain.repository.NotificationRepositoryImpl(httpClient, prefs)
    }

    class DynamicBaseUrlInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val currentUrl = ServerConnectionManager.currentBaseUrl
            
            // Safe parsing of the current base URL
            val newBaseUrl = try {
                currentUrl.toHttpUrlOrNull()
            } catch (e: Exception) {
                null
            }

            if (newBaseUrl == null) {
                return chain.proceed(originalRequest)
            }

            val newUrl = originalRequest.url.newBuilder()
                .scheme(newBaseUrl.scheme)
                .host(newBaseUrl.host)
                .port(newBaseUrl.port)
                .build()

            val newRequest = originalRequest.newBuilder()
                .url(newUrl)
                .build()

            return chain.proceed(newRequest)
        }
    }
}