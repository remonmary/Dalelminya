package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String,
    val passwordHash: String,
    val role: String = "USER", // "SUPER_ADMIN", "ADMIN", "MODERATOR", "USER"
    val createdAt: Long = System.currentTimeMillis()
) {
    val isAdmin: Boolean get() = role == "SUPER_ADMIN" || role == "ADMIN"
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val slug: String,
    val description: String,
    val iconName: String,
    val imageUrl: String = "",
    val sortOrder: Int = 0,
    val isHidden: Boolean = false
)

@Entity(
    tableName = "phone_numbers",
    indices = [
        Index(value = ["phoneNumber"]),
        Index(value = ["status"]),
        Index(value = ["isVerified"])
    ]
)
data class PhoneNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val slug: String,
    val phoneNumber: String,
    val whatsapp: String = "",
    val address: String = "المنيا",
    val village: String = "المنيا",
    val markaz: String = "مركز المنيا",
    val governorate: String = "المنيا",
    val description: String = "",
    val logoUrl: String = "",
    val website: String = "",
    val facebook: String = "",
    val instagram: String = "",
    val tiktok: String = "",
    val workingHours: String = "9:00 ص - 10:00 م",
    val isVerified: Boolean = false,
    val status: String = "approved", // "pending", "approved", "rejected", "suspended"
    val rejectionReason: String = "",
    val viewsCount: Int = 0,
    val callCount: Int = 0,
    val whatsappCount: Int = 0,
    val shareCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val userId: Long = 0,
    val firestoreId: String = "",
    val primaryCategoryId: Long = 0,
    val primaryCategoryName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val syncStatus: String = "synced" // "synced", "pending_sync"
)

@Entity(
    tableName = "phone_number_categories",
    primaryKeys = ["phoneId", "categoryId"],
    indices = [Index(value = ["categoryId"])]
)
data class PhoneNumberCategoryEntity(
    val phoneId: Long,
    val categoryId: Long
)

@Entity(
    tableName = "featured_numbers",
    indices = [Index(value = ["phoneId"]), Index(value = ["categoryId"])]
)
data class FeaturedNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneId: Long,
    val categoryId: Long? = null,
    val isGlobal: Boolean = false,
    val sortOrder: Int = 1,
    val startDate: Long = 0,
    val endDate: Long = 0,
    val noExpiration: Boolean = true,
    val status: String = "active",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "banners")
data class BannerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val imageUrl: String = "",
    val buttonText: String = "تصفح الآن",
    val buttonUrl: String = "",
    val targetType: String = "HOME", // "HOME", "CATEGORY", "ALL"
    val targetCategoryId: Long? = null,
    val sortOrder: Int = 0,
    val startDate: Long = 0,
    val endDate: Long = 0,
    val isActive: Boolean = true,
    val firestoreId: String = ""
)

@Entity(
    tableName = "favorites",
    primaryKeys = ["userId", "phoneId"]
)
data class FavoriteEntity(
    val userId: Long,
    val phoneId: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneId: Long,
    val userId: Long = 0,
    val reporterName: String = "",
    val reason: String,
    val details: String = "",
    val status: String = "PENDING", // "PENDING", "RESOLVED", "DISMISSED"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneId: Long,
    val userId: Long,
    val userName: String,
    val rating: Int, // 1 - 5
    val comment: String,
    val isApproved: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,
    val message: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_logs")
data class AdminLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val adminName: String,
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localPhoneId: Long,
    val clientUuid: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "pending" // "pending", "syncing", "synced", "failed"
)
