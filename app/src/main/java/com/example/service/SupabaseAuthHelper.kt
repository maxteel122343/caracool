package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class SupabaseUser(
    val id: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val accessToken: String? = null
) {
    // Compatibility getter
    val uid: String get() = id
}

object SupabaseAuthHelper {
    private const val TAG = "SupabaseAuthHelper"
    private const val PREFS_AUTH = "pacoca_supabase_auth"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_PHOTO = "user_photo"

    private const val FALLBACK_WEB_CLIENT_ID = "651868925341-ucpjafrvnkf5v1s7igpg0ss0jl920fik.apps.googleusercontent.com"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val _currentUser = MutableStateFlow<SupabaseUser?>(null)
    val currentUser: StateFlow<SupabaseUser?> = _currentUser.asStateFlow()

    fun getSupabaseUrl(): String {
        val url = try {
            val fromConfig = BuildConfig::class.java.getField("SUPABASE_URL").get(null) as? String
            fromConfig?.trim()?.removeSuffix("/") ?: ""
        } catch (_: Exception) {
            ""
        }
        return if (url.isNotBlank() && url != "https://your-project.supabase.co") {
            url
        } else {
            "https://placeholder-project.supabase.co"
        }
    }

    fun getSupabaseAnonKey(): String {
        return try {
            val fromConfig = BuildConfig::class.java.getField("SUPABASE_ANON_KEY").get(null) as? String
                ?: BuildConfig::class.java.getField("SUPABASE_KEY").get(null) as? String
            val key = fromConfig?.trim() ?: ""
            if (key.isNotBlank() && key != "your-anon-key") key else "placeholder-anon-key"
        } catch (_: Exception) {
            "placeholder-anon-key"
        }
    }

    fun isConfigured(): Boolean {
        val url = getSupabaseUrl()
        val key = getSupabaseAnonKey()
        return url.isNotBlank() && !url.contains("your-project") && !url.contains("placeholder-project") &&
                key.isNotBlank() && key != "your-anon-key" && key != "placeholder-anon-key"
    }

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE)
        val savedUserId = prefs.getString(KEY_USER_ID, null)
        val savedToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val savedEmail = prefs.getString(KEY_USER_EMAIL, null)
        val savedName = prefs.getString(KEY_USER_NAME, null)
        val savedPhoto = prefs.getString(KEY_USER_PHOTO, null)

        if (!savedUserId.isNullOrBlank()) {
            _currentUser.value = SupabaseUser(
                id = savedUserId,
                email = savedEmail,
                displayName = savedName,
                photoUrl = savedPhoto,
                accessToken = savedToken
            )
            Log.d(TAG, "Restored Supabase user session: $savedUserId ($savedEmail)")
        }
    }

    private fun persistSession(context: Context?, user: SupabaseUser?) {
        val targetContext = context ?: appContext
        if (targetContext != null) {
            val prefs = targetContext.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE)
            if (user != null) {
                prefs.edit()
                    .putString(KEY_USER_ID, user.id)
                    .putString(KEY_ACCESS_TOKEN, user.accessToken)
                    .putString(KEY_USER_EMAIL, user.email)
                    .putString(KEY_USER_NAME, user.displayName)
                    .putString(KEY_USER_PHOTO, user.photoUrl)
                    .apply()
            } else {
                prefs.edit().clear().apply()
            }
        }
        _currentUser.value = user
    }

    private fun getWebClientId(context: Context): String {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                context.getString(resId)
            } else {
                FALLBACK_WEB_CLIENT_ID
            }
        } catch (_: Exception) {
            FALLBACK_WEB_CLIENT_ID
        }
    }

    suspend fun signInWithGoogle(context: Context): Result<SupabaseUser> = withContext(Dispatchers.IO) {
        val webClientId = getWebClientId(context)
        Log.d(TAG, "Initiating Google Sign-In with Web Client ID: $webClientId")

        try {
            val credentialManager = CredentialManager.create(context)
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
                .build()

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleCred.idToken
                val email = googleCred.id
                val displayName = googleCred.displayName ?: email.substringBefore("@")
                val photoUrl = googleCred.profilePictureUri?.toString()

                // Attempt to authenticate with Supabase Auth via ID token if configured
                var supabaseUid: String? = null
                var accessToken: String? = null

                if (isConfigured()) {
                    try {
                        val authUrl = "${getSupabaseUrl()}/auth/v1/token?grant_type=id_token"
                        val jsonBody = """{"provider":"google","id_token":"$idToken"}"""
                        val req = Request.Builder()
                            .url(authUrl)
                            .addHeader("apikey", getSupabaseAnonKey())
                            .addHeader("Content-Type", "application/json")
                            .post(jsonBody.toRequestBody("application/json".toMediaType()))
                            .build()

                        val response = httpClient.newCall(req).execute()
                        val resStr = response.body?.string() ?: ""
                        if (response.isSuccessful && resStr.isNotBlank()) {
                            val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                            val adapter = moshi.adapter<Map<String, Any>>(mapType)
                            val jsonMap = adapter.fromJson(resStr)
                            accessToken = jsonMap?.get("access_token") as? String
                            val userObj = jsonMap?.get("user") as? Map<*, *>
                            supabaseUid = userObj?.get("id") as? String
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Supabase GoTrue ID token exchange fallback: ${e.message}")
                    }
                }

                // If GoTrue exchange isn't available or direct token mode, generate stable user ID from Google email
                val resolvedUid = supabaseUid ?: "google_${Math.abs(email.hashCode())}"
                val user = SupabaseUser(
                    id = resolvedUid,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    accessToken = accessToken
                )
                persistSession(context, user)
                Log.d(TAG, "Google Sign-In successful for Supabase: ${user.id} (${user.email})")
                Result.success(user)
            } else {
                Result.failure(Exception("Tipo de credencial incompatível recebido"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Google Sign-In was cancelled by user")
            Result.failure(Exception("Login cancelado pelo usuário."))
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No credentials available on device: ${e.message}")
            Result.failure(
                Exception(
                    "Nenhuma conta Google encontrada no aparelho. Cadastre-se ou entre com E-mail e Senha abaixo."
                )
            )
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Sign-In failed: ${e.message}", e)
            Result.failure(Exception("Falha ao autenticar Google: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Log.e(TAG, "Auth exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(context: Context? = null, email: String, password: String): Result<SupabaseUser> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            // Local fallback simulation when secrets not yet configured
            val simulatedUid = "user_${Math.abs(email.hashCode())}"
            val user = SupabaseUser(
                id = simulatedUid,
                email = email,
                displayName = email.substringBefore("@"),
                accessToken = "simulated_token"
            )
            persistSession(context, user)
            return@withContext Result.success(user)
        }

        try {
            val url = "${getSupabaseUrl()}/auth/v1/token?grant_type=password"
            val jsonBody = """{"email":"${email.trim()}","password":"${password.trim()}"}"""
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", getSupabaseAnonKey())
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(req).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                val adapter = moshi.adapter<Map<String, Any>>(mapType)
                val jsonMap = adapter.fromJson(responseBody)

                val token = jsonMap?.get("access_token") as? String
                val userObj = jsonMap?.get("user") as? Map<*, *>
                val uid = (userObj?.get("id") as? String) ?: "user_${Math.abs(email.hashCode())}"
                val userMeta = userObj?.get("user_metadata") as? Map<*, *>
                val name = (userMeta?.get("full_name") as? String) ?: email.substringBefore("@")
                val photo = userMeta?.get("avatar_url") as? String

                val user = SupabaseUser(
                    id = uid,
                    email = email,
                    displayName = name,
                    photoUrl = photo,
                    accessToken = token
                )
                persistSession(context, user)
                Result.success(user)
            } else {
                val errorMsg = extractErrorMessage(responseBody) ?: "E-mail ou senha incorretos."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in signInWithEmail: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(context: Context? = null, email: String, password: String): Result<SupabaseUser> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            val simulatedUid = "user_${Math.abs(email.hashCode())}"
            val user = SupabaseUser(
                id = simulatedUid,
                email = email,
                displayName = email.substringBefore("@"),
                accessToken = "simulated_token"
            )
            persistSession(context, user)
            return@withContext Result.success(user)
        }

        try {
            val url = "${getSupabaseUrl()}/auth/v1/signup"
            val jsonBody = """{"email":"${email.trim()}","password":"${password.trim()}"}"""
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", getSupabaseAnonKey())
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(req).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                val adapter = moshi.adapter<Map<String, Any>>(mapType)
                val jsonMap = adapter.fromJson(responseBody)

                val token = jsonMap?.get("access_token") as? String
                val userObj = jsonMap?.get("user") as? Map<*, *> ?: jsonMap
                val uid = (userObj?.get("id") as? String) ?: "user_${Math.abs(email.hashCode())}"

                val user = SupabaseUser(
                    id = uid,
                    email = email,
                    displayName = email.substringBefore("@"),
                    accessToken = token
                )
                persistSession(context, user)
                Result.success(user)
            } else {
                val errorMsg = extractErrorMessage(responseBody) ?: "Falha ao criar conta no Supabase."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in signUpWithEmail: ${e.message}")
            Result.failure(e)
        }
    }

    fun signOut(context: Context? = null) {
        persistSession(context, null)
    }

    private fun extractErrorMessage(json: String): String? {
        return try {
            val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            val adapter = moshi.adapter<Map<String, Any>>(mapType)
            val map = adapter.fromJson(json)
            (map?.get("msg") as? String)
                ?: (map?.get("error_description") as? String)
                ?: (map?.get("message") as? String)
        } catch (_: Exception) {
            null
        }
    }
}
