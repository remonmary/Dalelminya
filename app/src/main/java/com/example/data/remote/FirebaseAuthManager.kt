package com.example.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Handles Firebase Authentication securely.
 * Supports:
 * 1. Anonymous sign-in so every public visitor/user has a legitimate Firebase UID.
 * 2. Email/Password sign-in for Administrators.
 * 3. Token-based admin verification (custom claims or verified admin email).
 */
object FirebaseAuthManager {

    private val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("FirebaseAuthManager", "FirebaseAuth not available: ${e.message}")
            null
        }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    fun init() {
        val fa = auth ?: return
        _currentUser.value = fa.currentUser
        checkAdminStatus(fa.currentUser)

        fa.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            checkAdminStatus(user)
        }

        // Ensure at least anonymous user exists so all writes/reads carry legitimate request.auth
        if (fa.currentUser == null) {
            signInAnonymously()
        }
    }

    suspend fun ensureAuthenticated(): FirebaseUser? {
        val fa = auth ?: return null
        val existing = fa.currentUser
        if (existing != null) {
            return existing
        }
        return try {
            val result = fa.signInAnonymously().await()
            val user = result.user
            _currentUser.value = user
            Log.d("FirebaseAuthManager", "ensureAuthenticated: Signed in anonymously UID=${user?.uid}")
            user
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "ensureAuthenticated failed: ${e.message}", e)
            throw e
        }
    }

    fun signInAnonymously(onComplete: ((Boolean) -> Unit)? = null) {
        val fa = auth ?: run {
            onComplete?.invoke(false)
            return
        }
        fa.signInAnonymously()
            .addOnSuccessListener {
                Log.d("FirebaseAuthManager", "Signed in anonymously: ${it.user?.uid}")
                onComplete?.invoke(true)
            }
            .addOnFailureListener {
                Log.w("FirebaseAuthManager", "Anonymous sign-in failed: ${it.message}")
                onComplete?.invoke(false)
            }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
        val fa = auth ?: return Result.failure(IllegalStateException("Firebase Auth not initialized"))
        return try {
            val result = fa.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: throw IllegalStateException("User is null")
            val tokenResult = user.getIdToken(true).await()
            val claims = tokenResult.claims
            val emailAllowed = user.email?.lowercase() in setOf(
                "admin@nazletobeid.com",
                "admin@dalel-elminya.com",
                "admin@minya.com",
                "remonmary550@gmail.com"
            )
            val claimAllowed = claims["admin"] == true ||
                    claims["role"] == "ADMIN" ||
                    claims["role"] == "SUPER_ADMIN"
            _currentUser.value = user
            _isAdmin.value = emailAllowed || claimAllowed
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Email sign-in failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun signOut() {
        auth?.signOut()
        // Sign back in anonymously for standard user operations
        signInAnonymously()
    }

    fun checkAdminStatus(user: FirebaseUser?) {
        if (user == null || user.isAnonymous) {
            _isAdmin.value = false
            return
        }

        val adminEmails = listOf(
            "admin@dalel-elminya.com",
            "admin@minya.com",
            "remonmary550@gmail.com",
            "admin@nazletobeid.com"
        )
        val userEmail = user.email?.lowercase() ?: ""
        val isAdminEmail = adminEmails.contains(userEmail)

        user.getIdToken(false)
            .addOnSuccessListener { tokenResult ->
                val claims = tokenResult.claims
                val isAdminClaim = claims["admin"] == true ||
                        claims["role"] == "ADMIN" ||
                        claims["role"] == "SUPER_ADMIN"
                _isAdmin.value = isAdminClaim || isAdminEmail
            }
            .addOnFailureListener {
                _isAdmin.value = isAdminEmail
            }
    }


    fun getCurrentUid(): String {
        return auth?.currentUser?.uid ?: "anonymous"
    }
}
