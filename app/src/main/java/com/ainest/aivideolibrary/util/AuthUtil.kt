package com.ainest.aivideolibrary.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object AuthUtil {

    fun currentUser(): FirebaseUser? = FirebaseAuth.getInstance().currentUser

    fun signOut(context: Context) {
        FirebaseAuth.getInstance().signOut()
        try {
            googleClient(context).signOut()
        } catch (_: Exception) {
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * True only if Firebase's web client ID (needed for Google Sign-In) was generated
     * into resources. This requires a SHA-1 fingerprint to have been added to the
     * Firebase project - until then, Google Sign-In is hidden and Email/Password
     * or Skip are used instead.
     */
    fun isGoogleSignInAvailable(context: Context): Boolean = webClientId(context) != null

    private fun webClientId(context: Context): String? {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) context.getString(resId) else null
    }

    private fun googleClient(context: Context): GoogleSignInClient {
        val webClientId = webClientId(context)
        val optionsBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        if (webClientId != null) optionsBuilder.requestIdToken(webClientId)
        return GoogleSignIn.getClient(context, optionsBuilder.build())
    }

    fun googleSignInIntent(context: Context): Intent = googleClient(context).signInIntent

    suspend fun handleGoogleSignInResult(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = FirebaseAuth.getInstance().signInWithCredential(credential).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
