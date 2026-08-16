package com.example.service

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Bridge de compatibilidade que delega para [SupabaseAuthHelper].
 */
object FirebaseAuthHelper {
    val currentUser: StateFlow<SupabaseUser?> get() = SupabaseAuthHelper.currentUser

    suspend fun signInWithGoogle(context: Context): Result<SupabaseUser> {
        return SupabaseAuthHelper.signInWithGoogle(context)
    }

    suspend fun signInWithEmail(context: Context?, email: String, password: String): Result<SupabaseUser> {
        return SupabaseAuthHelper.signInWithEmail(context, email, password)
    }

    suspend fun signUpWithEmail(context: Context?, email: String, password: String): Result<SupabaseUser> {
        return SupabaseAuthHelper.signUpWithEmail(context, email, password)
    }

    fun signOut(context: Context? = null) {
        SupabaseAuthHelper.signOut(context)
    }
}
