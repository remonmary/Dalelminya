package com.example.data.remote

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Firestore Data Source for the "numbers" collection.
 * Project ID: dalel-elminya
 * Collection: numbers
 * Document fields: id, name, phone, whatsapp, description, categoryId, categoryName,
 * address, createdAt, updatedAt, isActive (+ other directory fields)
 */
class FirestoreService {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(com.example.DirectoryApplication.instance).isNotEmpty()) {
                val db = FirebaseFirestore.getInstance()
                // Explicitly ensure default host and standard SSL connection
                val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                db.firestoreSettings = settings
                db
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("FirestoreService", "FirebaseApp not initialized: ${e.message}")
            null
        }
    }

    val isFirebaseAvailable: Boolean
        get() = firestore != null

    /**
     * Diagnostic test: performs a lightweight get() query on the "numbers" collection.
     * Logs exact SUCCESS or error code + message to Logcat.
     */
    suspend fun testConnection(): Pair<Boolean, String> {
        val db = firestore ?: return Pair(false, "FirebaseApp is not initialized")
        return try {
            Log.d("FirestoreService", "Starting Firestore diagnostic test connection to collection('numbers').limit(1)...")
            val snapshot = db.collection("numbers").whereEqualTo("status", "approved").whereEqualTo("isActive", true).limit(1).get().await()
            val msg = "SUCCESS: Firestore connected! Found ${snapshot.size()} documents in sample."
            Log.d("FirestoreService", msg)
            Pair(true, msg)
        } catch (fe: FirebaseFirestoreException) {
            val err = "FirebaseFirestoreException [${fe.code}]: ${fe.message}"
            Log.e("FirestoreService", "Diagnostic test failed! $err", fe)
            Pair(false, err)
        } catch (fe: FirebaseException) {
            val err = "FirebaseException: ${fe.message}"
            Log.e("FirestoreService", "Diagnostic test failed! $err", fe)
            Pair(false, err)
        } catch (e: Exception) {
            val err = "Exception [${e.javaClass.simpleName}]: ${e.message}"
            Log.e("FirestoreService", "Diagnostic test failed! $err", e)
            Pair(false, err)
        }
    }

    /**
     * Real-time listener for all numbers in the "numbers" collection.
     * Yields updates whenever any number is added, edited, or deleted.
     */
    fun observeAllNumbers(): Flow<List<FirestoreNumber>> = callbackFlow {
        val db = firestore
        if (db == null) {
            Log.w("FirestoreService", "Firestore is not available. Yielding empty list.")
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            // Public app sync is intentionally restricted to records that are visible to users.
            // Admin moderation reads are performed through the authenticated admin path.
            registration = db.collection("numbers")
                .whereEqualTo("status", "approved")
                .whereEqualTo("isActive", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirestoreService", "Listen failed: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            FirestoreNumber.fromSnapshot(doc)
                        }
                        trySend(list)
                    }
                }
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error setting up listener: ${e.message}", e)
        }

        awaitClose {
            registration?.remove()
        }
    }

    /**
     * Real-time listener for active/approved numbers.
     */
    fun observeActiveNumbers(): Flow<List<FirestoreNumber>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = db.collection("numbers")
                .whereEqualTo("isActive", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirestoreService", "Listen failed: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            FirestoreNumber.fromSnapshot(doc)
                        }
                        trySend(list)
                    }
                }
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error setting up listener: ${e.message}", e)
        }

        awaitClose {
            registration?.remove()
        }
    }

    /**
     * Adds a new number to Firestore "numbers" collection.
     * Ensures an authenticated user (anonymous fallback if needed) before writing.
     * Returns the generated Document ID.
     */
    suspend fun addNumber(number: FirestoreNumber): String {
        val db = firestore ?: throw IllegalStateException("Firebase is not initialized")
        
        // 1. Ensure user is authenticated if auth is available (fallback gracefully if offline or anonymous)
        val authUser = try {
            FirebaseAuthManager.ensureAuthenticated()
        } catch (authEx: Exception) {
            Log.w("FirestoreService", "FirebaseAuth attempt returned: ${authEx.message}. Proceeding with write...")
            null
        }

        val collection = db.collection("numbers")
        val docRef = if (number.id.isNotBlank()) collection.document(number.id) else collection.document()
        val docId = docRef.id

        // Conforms strictly to required fields:
        // name, phone, categoryId, categoryName, description, address, status, isActive, createdAt
        val map = hashMapOf<String, Any?>(
            "id" to docId,
            "name" to number.name.trim(),
            "phone" to number.phone.trim(),
            "categoryId" to number.categoryId,
            "categoryName" to number.categoryName.trim(),
            "description" to number.description.trim(),
            "address" to number.address.trim(),
            "status" to "pending",
            "isActive" to false,
            "isVerified" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            // Supplementary directory details
            "whatsapp" to number.whatsapp.trim().ifBlank { number.phone.trim() },
            "workingHours" to number.workingHours.trim().ifBlank { "9:00 ص - 10:00 م" },
            "website" to number.website.trim(),
            "facebook" to number.facebook.trim(),
            "instagram" to number.instagram.trim(),
            "tiktok" to number.tiktok.trim(),
            "village" to number.village.trim(),
            "userId" to number.userId,
            "latitude" to number.latitude,
            "longitude" to number.longitude,
            "createdBy" to (authUser?.uid ?: FirebaseAuthManager.getCurrentUid())
        )

        try {
            Log.d("FirestoreService", "Attempting Firestore write to numbers/$docId with user ${authUser?.uid}")
            docRef.set(map).await()
            Log.d("FirestoreService", "Successfully wrote document numbers/$docId to Firestore")
            return docId
        } catch (fe: FirebaseFirestoreException) {
            Log.e("FirestoreService", "Firestore write failed with FirebaseFirestoreException! Code=${fe.code}, Message=${fe.message}", fe)
            throw fe
        } catch (fe: FirebaseException) {
            Log.e("FirestoreService", "Firestore write failed with FirebaseException! Message=${fe.message}", fe)
            throw fe
        } catch (e: Exception) {
            Log.e("FirestoreService", "Firestore write failed with Exception! Type=${e.javaClass.simpleName}, Message=${e.message}", e)
            throw e
        }
    }

    /**
     * Updates an existing document in "numbers" collection.
     */
    suspend fun updateNumber(number: FirestoreNumber) {
        val db = firestore ?: return
        if (number.id.isBlank()) return
        val docRef = db.collection("numbers").document(number.id)
        val map = hashMapOf<String, Any?>(
            "name" to number.name,
            "phone" to number.phone,
            "whatsapp" to number.whatsapp,
            "description" to number.description,
            "categoryId" to number.categoryId,
            "categoryName" to number.categoryName,
            "address" to number.address,
            "updatedAt" to System.currentTimeMillis(),
            "isActive" to number.isActive,
            "isVerified" to number.isVerified,
            "status" to number.status,
            "viewsCount" to number.viewsCount,
            "callCount" to number.callCount,
            "whatsappCount" to number.whatsappCount,
            "shareCount" to number.shareCount,
            "workingHours" to number.workingHours,
            "website" to number.website,
            "facebook" to number.facebook,
            "instagram" to number.instagram,
            "tiktok" to number.tiktok,
            "village" to number.village,
            "latitude" to number.latitude,
            "longitude" to number.longitude
        )
        docRef.update(map).await()
    }

    /**
     * Admin operations for number moderation
     */
    suspend fun approveNumber(docId: String) {
        val db = firestore ?: return
        if (docId.isBlank()) return
        db.collection("numbers").document(docId).update(
            mapOf(
                "status" to "approved",
                "isActive" to true,
                "rejectionReason" to "",
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun rejectNumber(docId: String, reason: String) {
        val db = firestore ?: return
        if (docId.isBlank()) return
        db.collection("numbers").document(docId).update(
            mapOf(
                "status" to "rejected",
                "isActive" to false,
                "rejectionReason" to reason,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun setVerified(docId: String, verified: Boolean) {
        val db = firestore ?: return
        if (docId.isBlank()) return
        db.collection("numbers").document(docId).update(
            mapOf(
                "isVerified" to verified,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun setStatus(docId: String, status: String) {
        val db = firestore ?: return
        if (docId.isBlank()) return
        val isActive = status == "approved"
        db.collection("numbers").document(docId).update(
            mapOf(
                "status" to status,
                "isActive" to isActive,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    // --- Banners Operations ---
    suspend fun getAllNumbersForAdmin(): List<FirestoreNumber> {
        val db = firestore ?: return emptyList()
        val snapshot = db.collection("numbers").get().await()
        return snapshot.documents.mapNotNull { FirestoreNumber.fromSnapshot(it) }
    }

    fun observeBanners(): Flow<List<com.example.data.model.BannerEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = db.collection("banners")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirestoreService", "Banners listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val banners = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data ?: return@mapNotNull null
                                com.example.data.model.BannerEntity(
                                    firestoreId = doc.id,
                                    title = data["title"] as? String ?: "",
                                    description = data["description"] as? String ?: "",
                                    imageUrl = data["imageUrl"] as? String ?: "",
                                    buttonText = data["buttonText"] as? String ?: "تصفح الآن",
                                    buttonUrl = data["buttonUrl"] as? String ?: "",
                                    targetType = data["targetType"] as? String ?: "HOME",
                                    sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
                                    isActive = data["isActive"] as? Boolean ?: true
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        trySend(banners)
                    }
                }
        } catch (e: Exception) {
            Log.e("FirestoreService", "Failed to observe banners: ${e.message}")
        }

        awaitClose { registration?.remove() }
    }

    suspend fun addBanner(banner: com.example.data.model.BannerEntity): String {
        val db = firestore ?: throw IllegalStateException("Firebase is not initialized")
        val docRef = db.collection("banners").document()
        val map = hashMapOf(
            "title" to banner.title,
            "description" to banner.description,
            "imageUrl" to banner.imageUrl,
            "buttonText" to banner.buttonText,
            "buttonUrl" to banner.buttonUrl,
            "targetType" to banner.targetType,
            "sortOrder" to banner.sortOrder,
            "isActive" to banner.isActive,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(map).await()
        return docRef.id
    }

    suspend fun updateBanner(banner: com.example.data.model.BannerEntity) {
        val db = firestore ?: return
        if (banner.firestoreId.isBlank()) return
        val map = hashMapOf<String, Any?>(
            "title" to banner.title,
            "description" to banner.description,
            "imageUrl" to banner.imageUrl,
            "buttonText" to banner.buttonText,
            "buttonUrl" to banner.buttonUrl,
            "targetType" to banner.targetType,
            "sortOrder" to banner.sortOrder,
            "isActive" to banner.isActive,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("banners").document(banner.firestoreId).update(map).await()
    }

    suspend fun deleteBanner(firestoreId: String) {
        val db = firestore ?: return
        if (firestoreId.isBlank()) return
        db.collection("banners").document(firestoreId).delete().await()
    }

    // --- Broadcast Notifications ---
    suspend fun sendBroadcastNotification(title: String, message: String, targetUrl: String = "") {
        val db = firestore ?: return
        val docRef = db.collection("notifications").document()
        val map = hashMapOf(
            "title" to title,
            "message" to message,
            "targetUrl" to targetUrl,
            "userId" to 0L,
            "createdAt" to System.currentTimeMillis()
        )
        docRef.set(map).await()
    }

    // --- Check Firestore Admin Collection ---
    suspend fun isUserAdminInFirestore(uid: String): Boolean {
        val db = firestore ?: return false
        return try {
            val doc = db.collection("admins").document(uid).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Deletes a number from Firestore.
     */
    suspend fun deleteNumber(docId: String) {
        val db = firestore ?: return
        if (docId.isBlank()) return
        db.collection("numbers").document(docId).delete().await()
    }

    /**
     * Increments view count in Firestore.
     */
    suspend fun incrementViews(docId: String) {
        val db = firestore ?: return
        if (docId.isBlank()) return
        db.collection("numbers").document(docId)
            .update("viewsCount", com.google.firebase.firestore.FieldValue.increment(1))
            .await()
    }

    /**
     * Increments call count in Firestore.
     */
    suspend fun incrementCalls(docId: String) {
        val db = firestore ?: return
        if (docId.isBlank()) return
        db.collection("numbers").document(docId)
            .update("callCount", com.google.firebase.firestore.FieldValue.increment(1))
            .await()
    }

    /**
     * Increments whatsapp count in Firestore.
     */
    suspend fun incrementWhatsapp(docId: String) {
        val db = firestore ?: return
        if (docId.isBlank()) return
        db.collection("numbers").document(docId)
            .update("whatsappCount", com.google.firebase.firestore.FieldValue.increment(1))
            .await()
    }
}
