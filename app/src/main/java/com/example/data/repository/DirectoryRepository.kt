package com.example.data.repository

import android.util.Log
import com.example.data.local.DirectoryDao
import com.example.data.model.AdminLogEntity
import com.example.data.model.BannerEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.DirectoryStats
import com.example.data.model.FavoriteEntity
import com.example.data.model.FeaturedNumberEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.PhoneNumberCategoryEntity
import com.example.data.model.PhoneNumberEntity
import com.example.data.model.PhoneNumberWithCategories
import com.example.data.model.ReportEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.SettingEntity
import com.example.data.model.UserEntity
import com.example.data.remote.FirestoreNumber
import com.example.data.remote.FirestoreService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * DirectoryRepository:
 * - Firebase Firestore is the Source of Truth for phone numbers (collection "numbers").
 * - Room database acts as the reactive Local / Offline Cache for UI performance and offline usage.
 * - Real-time listener continuously synchronizes Firestore documents into Room cache.
 */
class DirectoryRepository(
    private val dao: DirectoryDao,
    private val firestoreService: FirestoreService = FirestoreService()
) {
    private val repoScope = CoroutineScope(Dispatchers.IO)

    init {
        // Start real-time Firestore sync for numbers and banners
        startFirestoreSync()
        startBannersSync()
    }

    suspend fun testFirestoreConnection(): Pair<Boolean, String> {
        return firestoreService.testConnection()
    }

    suspend fun refreshAdminNumbers(): Result<Unit> {
        return try {
            val remoteList = firestoreService.getAllNumbersForAdmin()
            syncRemoteNumbersToCache(remoteList)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("DirectoryRepository", "Admin numbers refresh failed: ${e.message}")
            Result.failure(e)
        }
    }


    private fun startFirestoreSync() {
        repoScope.launch {
            try {
                firestoreService.observeAllNumbers().collect { remoteList ->
                    if (remoteList.isNotEmpty()) {
                        syncRemoteNumbersToCache(remoteList)
                    }
                }
            } catch (e: Exception) {
                Log.w("DirectoryRepository", "Firestore numbers sync exception: ${e.message}")
            }
        }
    }

    private fun startBannersSync() {
        repoScope.launch {
            try {
                firestoreService.observeBanners().collect { remoteBanners ->
                    if (remoteBanners.isNotEmpty()) {
                        for (remote in remoteBanners) {
                            val existing = dao.getBannerByFirestoreId(remote.firestoreId)
                            if (existing == null) {
                                dao.insertBanner(remote)
                            } else {
                                dao.updateBanner(remote.copy(id = existing.id))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("DirectoryRepository", "Firestore banners sync exception: ${e.message}")
            }
        }
    }

    private suspend fun syncRemoteNumbersToCache(remoteList: List<FirestoreNumber>) {
        val remoteIds = remoteList.map { it.id }.filter { it.isNotBlank() }.toSet()
        val allCategories = dao.getAllCategoriesSync()
        val categoryMapByName = allCategories.associateBy { it.name.trim().lowercase() }
        
        // Remove locally cached items that were deleted from Firestore
        val currentCached = dao.getAllPhoneNumbersSync()
        for (local in currentCached) {
            if (local.firestoreId.isNotBlank() && local.firestoreId !in remoteIds) {
                dao.deletePhoneNumber(local.id)
                dao.deleteCategoriesForPhone(local.id)
            }
        }

        // Upsert all remote documents into Room local cache
        for (remote in remoteList) {
            var matchedCatId = remote.categoryId
            var matchedCatName = remote.categoryName
            if (matchedCatId == 0L && matchedCatName.isNotBlank()) {
                val matched = categoryMapByName[matchedCatName.trim().lowercase()]
                if (matched != null) {
                    matchedCatId = matched.id
                }
            }

            val existing = if (remote.id.isNotBlank()) {
                dao.getPhoneNumberByFirestoreId(remote.id)
            } else null

            val entityToCache = remote.toEntity(localId = existing?.id ?: 0L).copy(
                primaryCategoryId = matchedCatId,
                primaryCategoryName = matchedCatName
            )
            val savedId = dao.insertPhoneNumber(entityToCache)

            // Sync category relation if present
            if (matchedCatId > 0L) {
                dao.insertPhoneNumberCategory(
                    PhoneNumberCategoryEntity(phoneId = savedId, categoryId = matchedCatId)
                )
            }
        }
    }

    // --- Users & Authentication ---
    suspend fun login(identifier: String, password: String): Result<UserEntity> {
        val user = dao.getUserByEmailOrPhone(identifier.trim())
            ?: return Result.failure(Exception("المستخدم غير مسجل"))
        return if (user.passwordHash == password.trim()) {
            Result.success(user)
        } else {
            Result.failure(Exception("كلمة المرور غير صحيحة"))
        }
    }

    suspend fun register(name: String, phone: String, email: String, password: String): Result<UserEntity> {
        val existing = dao.getUserByEmailOrPhone(phone.trim()) ?: dao.getUserByEmailOrPhone(email.trim())
        if (existing != null) {
            return Result.failure(Exception("رقم الهاتف أو البريد الإلكتروني مسجل بالفعل"))
        }
        val user = UserEntity(
            name = name.trim(),
            phone = phone.trim(),
            email = email.trim(),
            passwordHash = password.trim(),
            role = "USER"
        )
        val id = dao.insertUser(user)
        return Result.success(user.copy(id = id))
    }

    suspend fun getUserById(id: Long): UserEntity? = dao.getUserById(id)
    fun getAllUsers(): Flow<List<UserEntity>> = dao.getAllUsers()

    // --- Categories ---
    fun getAllCategories(): Flow<List<CategoryEntity>> = dao.getAllCategories()
    fun getVisibleCategories(): Flow<List<CategoryEntity>> = dao.getVisibleCategories()
    suspend fun getCategoryById(id: Long): CategoryEntity? = dao.getCategoryById(id)
    suspend fun getCategoryBySlug(slug: String): CategoryEntity? = dao.getCategoryBySlug(slug)
    suspend fun saveCategory(category: CategoryEntity): Long {
        return if (category.id == 0L) {
            dao.insertCategory(category)
        } else {
            dao.updateCategory(category)
            category.id
        }
    }
    suspend fun deleteCategory(id: Long) = dao.deleteCategory(id)

    // --- Phone Numbers (Flow backed by Room cache) ---
    fun getApprovedPhoneNumbers(): Flow<List<PhoneNumberEntity>> = dao.getApprovedPhoneNumbers()
    fun getAllPhoneNumbers(): Flow<List<PhoneNumberEntity>> = dao.getAllPhoneNumbers()
    fun getPhoneNumbersByStatus(status: String): Flow<List<PhoneNumberEntity>> = dao.getPhoneNumbersByStatus(status)
    fun getUserSubmissions(userId: Long): Flow<List<PhoneNumberEntity>> = dao.getUserSubmissions(userId)

    suspend fun getPhoneNumberById(id: Long): PhoneNumberEntity? = dao.getPhoneNumberById(id)
    suspend fun getPhoneNumberBySlug(slug: String): PhoneNumberEntity? = dao.getPhoneNumberBySlug(slug)

    suspend fun getPhoneWithDetails(phoneId: Long, currentUserId: Long): PhoneNumberWithCategories? {
        val phone = dao.getPhoneNumberById(phoneId) ?: return null
        val cats = dao.getCategoriesForPhoneSync(phoneId)
        val isFav = if (currentUserId > 0) dao.isFavorite(currentUserId, phoneId) else false
        val featured = dao.getAllFeatured().firstOrNull()?.find { it.phoneId == phoneId && it.status == "active" }

        return PhoneNumberWithCategories(
            number = phone,
            categories = cats,
            isFeatured = featured != null,
            isGlobalFeatured = featured?.isGlobal == true,
            featuredSortOrder = featured?.sortOrder ?: 0,
            isFavorite = isFav
        )
    }

    fun searchNumbers(query: String): Flow<List<PhoneNumberEntity>> = dao.searchApprovedPhoneNumbers(query)

    suspend fun submitPhoneNumber(
        phone: PhoneNumberEntity,
        categoryIds: List<Long>
    ): Long {
        val catName = if (categoryIds.isNotEmpty()) {
            dao.getCategoryById(categoryIds.first())?.name ?: ""
        } else ""
        val primaryCatId = categoryIds.firstOrNull() ?: 0L

        val phonePrepared = phone.copy(
            primaryCategoryId = primaryCatId,
            primaryCategoryName = catName,
            status = "pending",
            isVerified = false
        )

        var firestoreDocId = ""
        var syncStatus = "synced"

        // 1. Try writing to Firestore
        try {
            val firestoreNumber = FirestoreNumber.fromEntity(
                entity = phonePrepared,
                categoryId = primaryCatId,
                categoryName = catName
            )
            firestoreDocId = firestoreService.addNumber(firestoreNumber)
            Log.d("DirectoryRepository", "Saved to Firestore successfully with Document ID: $firestoreDocId")
        } catch (e: Exception) {
            Log.w("DirectoryRepository", "Firestore write error/offline: ${e.message}. Saving to local offline queue.")
            syncStatus = "pending_sync"
        }

        // 2. Cache in Room locally
        val phoneToCache = phonePrepared.copy(
            firestoreId = firestoreDocId,
            syncStatus = syncStatus
        )
        val insertedId = dao.insertPhoneNumber(phoneToCache)
        for (catId in categoryIds) {
            dao.insertPhoneNumberCategory(PhoneNumberCategoryEntity(insertedId, catId))
        }

        // If offline, add to sync queue
        if (syncStatus == "pending_sync") {
            dao.insertSyncQueue(
                com.example.data.model.SyncQueueEntity(
                    localPhoneId = insertedId,
                    clientUuid = java.util.UUID.randomUUID().toString()
                )
            )
        }

        // Notify user if submitted
        if (phone.userId > 0) {
            dao.insertNotification(
                NotificationEntity(
                    userId = phone.userId,
                    title = "تم استلام طلب إضافة رقمك",
                    message = "طلبك لإضافة '${phone.name}' قيد المراجعة حالياً من قبل إدارة دليل أرقام المنيا وسيتم إشعارك فور الموافقة."
                )
            )
        }
        return insertedId
    }

    suspend fun syncPendingSubmissions() {
        val queue = dao.getPendingSyncQueue()
        for (item in queue) {
            val phone = dao.getPhoneNumberById(item.localPhoneId)
            if (phone != null && phone.syncStatus == "pending_sync") {
                try {
                    val firestoreNumber = FirestoreNumber.fromEntity(
                        entity = phone,
                        categoryId = phone.primaryCategoryId,
                        categoryName = phone.primaryCategoryName
                    )
                    val docId = firestoreService.addNumber(firestoreNumber)
                    dao.updatePhoneSyncStatus(phone.id, docId, "synced")
                    dao.updateSyncQueueStatus(item.id, "synced")
                    Log.d("DirectoryRepository", "Synced pending offline submission: ${phone.name} ($docId)")
                } catch (e: Exception) {
                    Log.w("DirectoryRepository", "Sync pending item retry failed: ${e.message}")
                }
            }
        }
    }

    suspend fun updatePhoneNumber(phone: PhoneNumberEntity, categoryIds: List<Long>) {
        val catName = if (categoryIds.isNotEmpty()) {
            dao.getCategoryById(categoryIds.first())?.name ?: ""
        } else ""
        val primaryCatId = categoryIds.firstOrNull() ?: 0L

        // 1. Update in Firestore
        if (phone.firestoreId.isNotBlank()) {
            try {
                firestoreService.updateNumber(
                    FirestoreNumber.fromEntity(
                        entity = phone.copy(
                            primaryCategoryId = primaryCatId,
                            primaryCategoryName = catName
                        ),
                        categoryId = primaryCatId,
                        categoryName = catName
                    )
                )
            } catch (e: Exception) {
                Log.w("DirectoryRepository", "Firestore update error: ${e.message}")
            }
        }

        // 2. Update Room cache
        dao.updatePhoneNumber(
            phone.copy(primaryCategoryId = primaryCatId, primaryCategoryName = catName)
        )
        dao.deleteCategoriesForPhone(phone.id)
        for (catId in categoryIds) {
            dao.insertPhoneNumberCategory(PhoneNumberCategoryEntity(phone.id, catId))
        }
    }

    suspend fun deletePhoneNumber(phoneId: Long) {
        val phone = dao.getPhoneNumberById(phoneId)
        if (phone != null && phone.firestoreId.isNotBlank()) {
            try {
                firestoreService.deleteNumber(phone.firestoreId)
            } catch (e: Exception) {
                Log.w("DirectoryRepository", "Firestore delete error: ${e.message}")
            }
        }
        dao.deletePhoneNumber(phoneId)
        dao.deleteCategoriesForPhone(phoneId)
        dao.deleteFeaturedByPhoneId(phoneId)
    }

    suspend fun incrementViews(id: Long) {
        dao.incrementViews(id)
        val phone = dao.getPhoneNumberById(id)
        if (phone != null && phone.firestoreId.isNotBlank()) {
            try {
                firestoreService.incrementViews(phone.firestoreId)
            } catch (e: Exception) {
                // Ignore background increment errors
            }
        }
    }

    suspend fun incrementCalls(id: Long) {
        dao.incrementCalls(id)
        val phone = dao.getPhoneNumberById(id)
        if (phone != null && phone.firestoreId.isNotBlank()) {
            try {
                firestoreService.incrementCalls(phone.firestoreId)
            } catch (e: Exception) {
                // Ignore background increment errors
            }
        }
    }

    suspend fun incrementWhatsapp(id: Long) {
        dao.incrementWhatsapp(id)
        val phone = dao.getPhoneNumberById(id)
        if (phone != null && phone.firestoreId.isNotBlank()) {
            try {
                firestoreService.incrementWhatsapp(phone.firestoreId)
            } catch (e: Exception) {
                // Ignore background increment errors
            }
        }
    }

    suspend fun incrementShares(id: Long) = dao.incrementShares(id)

    // --- Admin Actions ---
    suspend fun approveSubmission(phoneId: Long, adminName: String) {
        val phone = dao.getPhoneNumberById(phoneId) ?: return

        // Firestore is the source of truth. Complete the remote moderation first.
        if (phone.firestoreId.isNotBlank()) {
            try {
                firestoreService.approveNumber(phone.firestoreId)
                firestoreService.updateNumber(
                    FirestoreNumber.fromEntity(
                        phone.copy(status = "approved", isVerified = true)
                    )
                )
            } catch (e: Exception) {
                Log.w("DirectoryRepository", "Firestore approve error: ${e.message}")
                return
            }
        }

        // Only after the remote write succeeds, update the local cache.
        dao.updatePhoneNumberStatus(phoneId, "approved", "")
        dao.updatePhoneNumberVerification(phoneId, true)

        val updatedPhone = dao.getPhoneNumberById(phoneId)
        if (updatedPhone != null) {
            if (updatedPhone.userId > 0) {
                dao.insertNotification(
                    NotificationEntity(
                        userId = phone.userId,
                        title = "تمت الموافقة على رقمك بنجاح! 🎉",
                        message = "تمت مراجعة وقبول إضافة '${phone.name}' وأصبح متاحاً للعامة في دليل أرقام المنيا."
                    )
                )
            }
            dao.insertLog(AdminLogEntity(adminName = adminName, action = "الموافقة على طلب", details = "تم قبول الرقم: ${phone.name} (ID: $phoneId)"))
        }
    }

    suspend fun rejectSubmission(phoneId: Long, reason: String, adminName: String) {
        dao.updatePhoneNumberStatus(phoneId, "rejected", reason)
        val phone = dao.getPhoneNumberById(phoneId)
        if (phone != null) {
            if (phone.firestoreId.isNotBlank()) {
                try {
                    val updated = phone.copy(status = "rejected", rejectionReason = reason)
                    firestoreService.updateNumber(FirestoreNumber.fromEntity(updated))
                } catch (e: Exception) {
                    Log.w("DirectoryRepository", "Firestore reject error: ${e.message}")
                }
            }
            if (phone.userId > 0) {
                dao.insertNotification(
                    NotificationEntity(
                        userId = phone.userId,
                        title = "تم رفض طلب إضافة الرقم",
                        message = "نأسف، تم رفض طلب إضافة '${phone.name}'. السبب: $reason"
                    )
                )
            }
            dao.insertLog(AdminLogEntity(adminName = adminName, action = "رفض طلب", details = "تم رفض الرقم: ${phone.name}، السبب: $reason"))
        }
    }

    suspend fun setVerified(phoneId: Long, isVerified: Boolean, adminName: String) {
        dao.updatePhoneNumberVerification(phoneId, isVerified)
        val phone = dao.getPhoneNumberById(phoneId)
        if (phone != null && phone.firestoreId.isNotBlank()) {
            try {
                firestoreService.updateNumber(FirestoreNumber.fromEntity(phone.copy(isVerified = isVerified)))
            } catch (e: Exception) {
                Log.w("DirectoryRepository", "Firestore verification update error: ${e.message}")
            }
        }
        dao.insertLog(AdminLogEntity(adminName = adminName, action = if (isVerified) "توثيق رقم" else "إلغاء توثيق", details = "ID: $phoneId"))
    }

    suspend fun setStatus(phoneId: Long, status: String, adminName: String) {
        dao.updatePhoneNumberStatus(phoneId, status, "")
        val phone = dao.getPhoneNumberById(phoneId)
        if (phone != null && phone.firestoreId.isNotBlank()) {
            try {
                firestoreService.updateNumber(FirestoreNumber.fromEntity(phone.copy(status = status)))
            } catch (e: Exception) {
                Log.w("DirectoryRepository", "Firestore status update error: ${e.message}")
            }
        }
        dao.insertLog(AdminLogEntity(adminName = adminName, action = "تغيير حالة الرقم", details = "ID: $phoneId إلى $status"))
    }

    // --- Featured Numbers Management ---
    fun getAllFeatured(): Flow<List<FeaturedNumberEntity>> = dao.getAllFeatured()
    suspend fun pinNumber(
        phoneId: Long,
        categoryId: Long?,
        isGlobal: Boolean,
        sortOrder: Int,
        noExpiration: Boolean,
        adminName: String
    ) {
        dao.deleteFeaturedByPhoneId(phoneId)
        dao.insertFeatured(
            FeaturedNumberEntity(
                phoneId = phoneId,
                categoryId = categoryId,
                isGlobal = isGlobal,
                sortOrder = sortOrder,
                noExpiration = noExpiration,
                status = "active"
            )
        )
        dao.insertLog(AdminLogEntity(adminName = adminName, action = "تثبيت رقم ⭐", details = "Phone ID: $phoneId, Global: $isGlobal, Order: $sortOrder"))
    }

    suspend fun unpinNumber(phoneId: Long, adminName: String) {
        dao.deleteFeaturedByPhoneId(phoneId)
        dao.insertLog(AdminLogEntity(adminName = adminName, action = "إلغاء تثبيت رقم", details = "Phone ID: $phoneId"))
    }

    // --- Banners ---
    fun getActiveBanners(): Flow<List<BannerEntity>> = dao.getActiveBanners()
    fun getAllBanners(): Flow<List<BannerEntity>> = dao.getAllBanners()
    suspend fun saveBanner(banner: BannerEntity): Long {
        var firestoreId = banner.firestoreId
        if (firestoreId.isBlank()) {
            try {
                firestoreId = firestoreService.addBanner(banner)
            } catch (e: Exception) {
                Log.w("DirectoryRepository", "Firestore addBanner error: ${e.message}")
            }
        } else {
            try {
                firestoreService.updateBanner(banner)
            } catch (e: Exception) {
                Log.w("DirectoryRepository", "Firestore updateBanner error: ${e.message}")
            }
        }

        val bannerWithFirestore = banner.copy(firestoreId = firestoreId)
        return if (banner.id == 0L) {
            dao.insertBanner(bannerWithFirestore)
        } else {
            dao.updateBanner(bannerWithFirestore)
            banner.id
        }
    }

    suspend fun deleteBanner(id: Long) {
        val all = dao.getAllBanners().firstOrNull() ?: emptyList()
        val banner = all.find { it.id == id }
        if (banner != null && banner.firestoreId.isNotBlank()) {
            try {
                firestoreService.deleteBanner(banner.firestoreId)
            } catch (e: Exception) {
                Log.w("DirectoryRepository", "Firestore deleteBanner error: ${e.message}")
            }
        }
        dao.deleteBanner(id)
    }

    // --- Notifications ---
    fun getNotifications(userId: Long): Flow<List<NotificationEntity>> = dao.getNotificationsForUser(userId)
    fun getUnreadNotificationsCount(): Flow<Int> = dao.getUnreadNotificationsCount()
    suspend fun markNotificationRead(id: Long) = dao.markNotificationAsRead(id)
    suspend fun markAllNotificationsAsRead() = dao.markAllNotificationsAsRead()

    suspend fun sendBroadcastNotification(title: String, message: String, targetUrl: String = "", adminName: String = "Admin") {
        try {
            firestoreService.sendBroadcastNotification(title, message, targetUrl)
        } catch (e: Exception) {
            Log.w("DirectoryRepository", "Firestore broadcast notification error: ${e.message}")
        }
        // Also insert locally
        dao.insertNotification(
            NotificationEntity(
                userId = 0L,
                title = title,
                message = message,
                isRead = false
            )
        )
        dao.insertLog(AdminLogEntity(adminName = adminName, action = "إرسال إشعار عام", details = "$title: $message"))
    }
    fun getFavoritePhones(userId: Long): Flow<List<PhoneNumberEntity>> = dao.getFavoritePhonesForUser(userId)
    fun getFavoriteIds(userId: Long): Flow<List<Long>> = dao.getFavoritePhoneIds(userId)

    suspend fun toggleFavorite(userId: Long, phoneId: Long): Boolean {
        return if (dao.isFavorite(userId, phoneId)) {
            dao.deleteFavorite(userId, phoneId)
            false
        } else {
            dao.insertFavorite(FavoriteEntity(userId, phoneId))
            true
        }
    }

    // --- Reports ---
    fun getAllReports(): Flow<List<ReportEntity>> = dao.getAllReports()
    suspend fun submitReport(phoneId: Long, userId: Long, name: String, reason: String, details: String): Long {
        return dao.insertReport(
            ReportEntity(
                phoneId = phoneId,
                userId = userId,
                reporterName = name,
                reason = reason,
                details = details
            )
        )
    }
    suspend fun updateReportStatus(id: Long, status: String) = dao.updateReportStatus(id, status)
    suspend fun deleteReport(id: Long) = dao.deleteReport(id)

    // --- Reviews ---
    fun getReviewsForPhone(phoneId: Long): Flow<List<ReviewEntity>> = dao.getApprovedReviewsForPhone(phoneId)
    fun getAllReviews(): Flow<List<ReviewEntity>> = dao.getAllReviews()
    suspend fun addReview(phoneId: Long, userId: Long, userName: String, rating: Int, comment: String): Long {
        return dao.insertReview(
            ReviewEntity(
                phoneId = phoneId,
                userId = userId,
                userName = userName,
                rating = rating,
                comment = comment,
                isApproved = true
            )
        )
    }
    suspend fun updateReviewApproval(id: Long, isApproved: Boolean) = dao.updateReviewApproval(id, isApproved)
    suspend fun deleteReview(id: Long) = dao.deleteReview(id)

    // --- Admin Logs ---
    fun getAdminLogs(): Flow<List<AdminLogEntity>> = dao.getAllLogs()

    // --- Settings ---
    fun getAllSettings(): Flow<List<SettingEntity>> = dao.getAllSettings()
    suspend fun saveSetting(key: String, value: String) = dao.insertSetting(SettingEntity(key, value))

    // --- Statistics ---
    suspend fun getStats(): DirectoryStats {
        return DirectoryStats(
            totalNumbers = dao.getTotalNumbersCount(),
            activeNumbers = dao.getActiveNumbersCount(),
            pendingNumbers = dao.getPendingNumbersCount(),
            verifiedNumbers = dao.getVerifiedNumbersCount(),
            featuredNumbers = dao.getFeaturedNumbersCount(),
            totalUsers = dao.getUsersCount(),
            totalCategories = dao.getCategoriesCount(),
            totalReports = dao.getPendingReportsCount(),
            totalReviews = dao.getTotalReviewsCount(),
            totalViews = dao.getTotalViewsSum() ?: 0
        )
    }

    suspend fun getCategoriesForPhoneSync(phoneId: Long): List<CategoryEntity> {
        return dao.getCategoriesForPhoneSync(phoneId)
    }

    fun getApprovedPhonesForCategory(categoryId: Long): Flow<List<PhoneNumberEntity>> {
        return dao.getApprovedPhonesForCategory(categoryId)
    }
}
