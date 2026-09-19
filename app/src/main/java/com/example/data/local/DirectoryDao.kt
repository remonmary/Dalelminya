package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.AdminLogEntity
import com.example.data.model.BannerEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.FavoriteEntity
import com.example.data.model.FeaturedNumberEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.PhoneNumberCategoryEntity
import com.example.data.model.PhoneNumberEntity
import com.example.data.model.ReportEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.SettingEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectoryDao {

    // --- Users ---
    @Query("SELECT * FROM users WHERE email = :identifier OR phone = :identifier LIMIT 1")
    suspend fun getUserByEmailOrPhone(identifier: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUsersCount(): Int

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllCategoriesSync(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE isHidden = 0 ORDER BY sortOrder ASC, name ASC")
    fun getVisibleCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE slug = :slug LIMIT 1")
    suspend fun getCategoryBySlug(slug: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoriesCount(): Int

    // --- Phone Numbers ---
    @Query("SELECT * FROM phone_numbers WHERE status = 'approved' ORDER BY createdAt DESC")
    fun getApprovedPhoneNumbers(): Flow<List<PhoneNumberEntity>>

    @Query("SELECT * FROM phone_numbers ORDER BY createdAt DESC")
    fun getAllPhoneNumbers(): Flow<List<PhoneNumberEntity>>

    @Query("SELECT * FROM phone_numbers")
    suspend fun getAllPhoneNumbersSync(): List<PhoneNumberEntity>

    @Query("SELECT * FROM phone_numbers WHERE status = :status ORDER BY createdAt DESC")
    fun getPhoneNumbersByStatus(status: String): Flow<List<PhoneNumberEntity>>

    @Query("SELECT * FROM phone_numbers WHERE id = :id LIMIT 1")
    suspend fun getPhoneNumberById(id: Long): PhoneNumberEntity?

    @Query("SELECT * FROM phone_numbers WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getPhoneNumberByFirestoreId(firestoreId: String): PhoneNumberEntity?

    @Query("SELECT * FROM phone_numbers WHERE slug = :slug LIMIT 1")
    suspend fun getPhoneNumberBySlug(slug: String): PhoneNumberEntity?

    @Query("SELECT * FROM phone_numbers WHERE userId = :userId ORDER BY createdAt DESC")
    fun getUserSubmissions(userId: Long): Flow<List<PhoneNumberEntity>>

    @Query("""
        SELECT * FROM phone_numbers 
        WHERE status = 'approved' 
        AND (name LIKE '%' || :query || '%' 
             OR phoneNumber LIKE '%' || :query || '%' 
             OR description LIKE '%' || :query || '%' 
             OR address LIKE '%' || :query || '%' 
             OR village LIKE '%' || :query || '%')
        ORDER BY isVerified DESC, viewsCount DESC
    """)
    fun searchApprovedPhoneNumbers(query: String): Flow<List<PhoneNumberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoneNumber(phoneNumber: PhoneNumberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoneNumbers(phoneNumbers: List<PhoneNumberEntity>)

    @Query("DELETE FROM phone_numbers")
    suspend fun clearAllPhoneNumbers()

    @Update
    suspend fun updatePhoneNumber(phoneNumber: PhoneNumberEntity)

    @Query("DELETE FROM phone_numbers WHERE id = :id")
    suspend fun deletePhoneNumber(id: Long)

    @Query("DELETE FROM phone_numbers WHERE firestoreId = :firestoreId")
    suspend fun deletePhoneNumberByFirestoreId(firestoreId: String)

    @Query("UPDATE phone_numbers SET viewsCount = viewsCount + 1 WHERE id = :id")
    suspend fun incrementViews(id: Long)

    @Query("UPDATE phone_numbers SET callCount = callCount + 1 WHERE id = :id")
    suspend fun incrementCalls(id: Long)

    @Query("UPDATE phone_numbers SET whatsappCount = whatsappCount + 1 WHERE id = :id")
    suspend fun incrementWhatsapp(id: Long)

    @Query("UPDATE phone_numbers SET shareCount = shareCount + 1 WHERE id = :id")
    suspend fun incrementShares(id: Long)

    @Query("UPDATE phone_numbers SET status = :status, rejectionReason = :reason WHERE id = :id")
    suspend fun updatePhoneNumberStatus(id: Long, status: String, reason: String = "")

    @Query("UPDATE phone_numbers SET isVerified = :isVerified WHERE id = :id")
    suspend fun updatePhoneNumberVerification(id: Long, isVerified: Boolean)

    // --- Phone Number Categories (Many-to-Many) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoneNumberCategory(relation: PhoneNumberCategoryEntity)

    @Query("DELETE FROM phone_number_categories WHERE phoneId = :phoneId")
    suspend fun deleteCategoriesForPhone(phoneId: Long)

    @Query("""
        SELECT c.* FROM categories c 
        INNER JOIN phone_number_categories pnc ON c.id = pnc.categoryId 
        WHERE pnc.phoneId = :phoneId
        ORDER BY c.sortOrder ASC
    """)
    suspend fun getCategoriesForPhoneSync(phoneId: Long): List<CategoryEntity>

    @Query("""
        SELECT c.* FROM categories c 
        INNER JOIN phone_number_categories pnc ON c.id = pnc.categoryId 
        WHERE pnc.phoneId = :phoneId
        ORDER BY c.sortOrder ASC
    """)
    fun getCategoriesForPhone(phoneId: Long): Flow<List<CategoryEntity>>

    @Query("""
        SELECT DISTINCT p.* FROM phone_numbers p
        LEFT JOIN phone_number_categories pnc ON p.id = pnc.phoneId
        LEFT JOIN categories c ON (c.id = :categoryId)
        WHERE (
            pnc.categoryId = :categoryId 
            OR p.primaryCategoryId = :categoryId 
            OR (c.name IS NOT NULL AND c.name != '' AND (p.primaryCategoryName = c.name OR p.primaryCategoryName LIKE '%' || c.name || '%'))
            OR (c.slug IS NOT NULL AND c.slug != '' AND p.slug LIKE '%' || c.slug || '%')
        )
        AND p.status = 'approved'
        ORDER BY p.isVerified DESC, p.createdAt DESC
    """)
    fun getApprovedPhonesForCategory(categoryId: Long): Flow<List<PhoneNumberEntity>>

    // --- Featured / Pinned Numbers ---
    @Query("SELECT * FROM featured_numbers ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllFeatured(): Flow<List<FeaturedNumberEntity>>

    @Query("SELECT * FROM featured_numbers WHERE isGlobal = 1 AND status = 'active' ORDER BY sortOrder ASC")
    suspend fun getGlobalFeaturedSync(): List<FeaturedNumberEntity>

    @Query("SELECT * FROM featured_numbers WHERE (categoryId = :categoryId OR isGlobal = 1) AND status = 'active' ORDER BY sortOrder ASC")
    suspend fun getFeaturedForCategorySync(categoryId: Long): List<FeaturedNumberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeatured(featured: FeaturedNumberEntity): Long

    @Query("DELETE FROM featured_numbers WHERE id = :id")
    suspend fun deleteFeatured(id: Long)

    @Query("DELETE FROM featured_numbers WHERE phoneId = :phoneId")
    suspend fun deleteFeaturedByPhoneId(phoneId: Long)

    @Query("UPDATE featured_numbers SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateFeaturedSortOrder(id: Long, sortOrder: Int)

    // --- Banners ---
    @Query("SELECT * FROM banners WHERE isActive = 1 ORDER BY sortOrder ASC, id DESC")
    fun getActiveBanners(): Flow<List<BannerEntity>>

    @Query("SELECT * FROM banners ORDER BY sortOrder ASC, id DESC")
    fun getAllBanners(): Flow<List<BannerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(banner: BannerEntity): Long

    @Update
    suspend fun updateBanner(banner: BannerEntity)

    @Query("DELETE FROM banners WHERE id = :id")
    suspend fun deleteBanner(id: Long)

    @Query("SELECT COUNT(*) FROM banners")
    suspend fun getBannersCount(): Int

    // --- Favorites ---
    @Query("SELECT phoneId FROM favorites WHERE userId = :userId")
    fun getFavoritePhoneIds(userId: Long): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND phoneId = :phoneId)")
    suspend fun isFavorite(userId: Long, phoneId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND phoneId = :phoneId")
    suspend fun deleteFavorite(userId: Long, phoneId: Long)

    @Query("""
        SELECT p.* FROM phone_numbers p
        INNER JOIN favorites f ON p.id = f.phoneId
        WHERE f.userId = :userId AND p.status = 'approved'
        ORDER BY f.createdAt DESC
    """)
    fun getFavoritePhonesForUser(userId: Long): Flow<List<PhoneNumberEntity>>

    // --- Reports ---
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity): Long

    @Query("UPDATE reports SET status = :status WHERE id = :id")
    suspend fun updateReportStatus(id: Long, status: String)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteReport(id: Long)

    // --- Reviews ---
    @Query("SELECT * FROM reviews WHERE phoneId = :phoneId AND isApproved = 1 ORDER BY createdAt DESC")
    fun getApprovedReviewsForPhone(phoneId: Long): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews ORDER BY createdAt DESC")
    fun getAllReviews(): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity): Long

    @Query("UPDATE reviews SET isApproved = :isApproved WHERE id = :id")
    suspend fun updateReviewApproval(id: Long, isApproved: Boolean)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun deleteReview(id: Long)

    // --- Notifications ---
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun getNotificationsForUser(userId: Long): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Long)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)

    // --- Admin Logs ---
    @Query("SELECT * FROM admin_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<AdminLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AdminLogEntity): Long

    // --- Settings ---
    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): SettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingEntity)

    // --- Stats Helpers ---
    @Query("SELECT COUNT(*) FROM phone_numbers")
    suspend fun getTotalNumbersCount(): Int

    @Query("SELECT COUNT(*) FROM phone_numbers WHERE status = 'approved'")
    suspend fun getActiveNumbersCount(): Int

    @Query("SELECT COUNT(*) FROM phone_numbers WHERE status = 'pending'")
    suspend fun getPendingNumbersCount(): Int

    @Query("SELECT COUNT(*) FROM phone_numbers WHERE isVerified = 1 AND status = 'approved'")
    suspend fun getVerifiedNumbersCount(): Int

    @Query("SELECT COUNT(*) FROM featured_numbers WHERE status = 'active'")
    suspend fun getFeaturedNumbersCount(): Int

    @Query("SELECT COUNT(*) FROM reports WHERE status = 'PENDING'")
    suspend fun getPendingReportsCount(): Int

    @Query("SELECT COUNT(*) FROM reviews")
    suspend fun getTotalReviewsCount(): Int

    @Query("SELECT SUM(viewsCount) FROM phone_numbers")
    suspend fun getTotalViewsSum(): Int?

    // --- Sync Queue & Offline ---
    @Query("SELECT * FROM sync_queue WHERE status = 'pending' ORDER BY createdAt ASC")
    suspend fun getPendingSyncQueue(): List<com.example.data.model.SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncQueue(item: com.example.data.model.SyncQueueEntity): Long

    @Query("UPDATE sync_queue SET status = :status WHERE id = :id")
    suspend fun updateSyncQueueStatus(id: Long, status: String)

    @Query("UPDATE phone_numbers SET firestoreId = :firestoreId, syncStatus = :syncStatus WHERE id = :id")
    suspend fun updatePhoneSyncStatus(id: Long, firestoreId: String, syncStatus: String)

    @Query("SELECT * FROM banners WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getBannerByFirestoreId(firestoreId: String): BannerEntity?

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadNotificationsCount(): Flow<Int>

    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllNotificationsAsRead()
}
