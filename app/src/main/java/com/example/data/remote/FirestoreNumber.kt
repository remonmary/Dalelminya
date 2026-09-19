package com.example.data.remote

import androidx.annotation.Keep
import com.example.data.model.PhoneNumberEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Firestore Document Model for "numbers" collection.
 * Conforms strictly to requested fields:
 * id, name, phone, whatsapp, description, categoryId, categoryName, address,
 * createdAt, updatedAt, isActive
 */
@Keep
@IgnoreExtraProperties
data class FirestoreNumber(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val description: String = "",
    val categoryId: Long = 0L,
    val categoryName: String = "",
    val address: String = "المنيا",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    // Extra directory fields
    val isVerified: Boolean = false,
    val status: String = "approved",
    val viewsCount: Int = 0,
    val callCount: Int = 0,
    val whatsappCount: Int = 0,
    val shareCount: Int = 0,
    val workingHours: String = "9:00 ص - 10:00 م",
    val website: String = "",
    val facebook: String = "",
    val instagram: String = "",
    val tiktok: String = "",
    val village: String = "المنيا",
    val userId: Long = 0L,
    val createdBy: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    fun toEntity(localId: Long = 0L): PhoneNumberEntity {
        return PhoneNumberEntity(
            id = localId,
            name = name,
            slug = name.trim().lowercase().replace("\\s+".toRegex(), "-"),
            phoneNumber = phone,
            whatsapp = whatsapp.ifBlank { phone },
            address = address,
            village = village,
            description = description,
            workingHours = workingHours,
            isVerified = isVerified,
            status = status,
            viewsCount = viewsCount,
            callCount = callCount,
            whatsappCount = whatsappCount,
            shareCount = shareCount,
            createdAt = createdAt,
            userId = userId,
            website = website,
            facebook = facebook,
            instagram = instagram,
            tiktok = tiktok,
            firestoreId = id,
            primaryCategoryId = categoryId,
            primaryCategoryName = categoryName,
            latitude = latitude,
            longitude = longitude,
            syncStatus = "synced"
        )
    }

    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): FirestoreNumber? {
            return try {
                val data = doc.data ?: return null
                val id = doc.id
                val name = data["name"] as? String ?: ""
                val phone = (data["phone"] as? String) ?: (data["phoneNumber"] as? String) ?: ""
                val whatsapp = data["whatsapp"] as? String ?: phone
                val description = data["description"] as? String ?: ""
                val categoryName = (data["categoryName"] as? String)
                    ?: (data["category"] as? String)
                    ?: ""
                val categoryId = when (val c = data["categoryId"] ?: data["category_id"]) {
                    is Number -> c.toLong()
                    is String -> c.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val address = data["address"] as? String ?: "المنيا"
                val createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val isActive = data["isActive"] as? Boolean ?: (data["status"] == "approved")
                val isVerified = data["isVerified"] as? Boolean ?: false
                val status = data["status"] as? String ?: if (isActive) "approved" else "pending"
                val viewsCount = (data["viewsCount"] as? Number)?.toInt() ?: 0
                val callCount = (data["callCount"] as? Number)?.toInt() ?: 0
                val whatsappCount = (data["whatsappCount"] as? Number)?.toInt() ?: 0
                val shareCount = (data["shareCount"] as? Number)?.toInt() ?: 0
                val workingHours = data["workingHours"] as? String ?: "9:00 ص - 10:00 م"
                val website = data["website"] as? String ?: ""
                val facebook = data["facebook"] as? String ?: ""
                val instagram = data["instagram"] as? String ?: ""
                val tiktok = data["tiktok"] as? String ?: ""
                val village = data["village"] as? String ?: "المنيا"
                val userId = (data["userId"] as? Number)?.toLong() ?: 0L
                val latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0
                val longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0

                FirestoreNumber(
                    id = id,
                    name = name,
                    phone = phone,
                    whatsapp = whatsapp,
                    description = description,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    address = address,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    isActive = isActive,
                    isVerified = isVerified,
                    status = status,
                    viewsCount = viewsCount,
                    callCount = callCount,
                    whatsappCount = whatsappCount,
                    shareCount = shareCount,
                    workingHours = workingHours,
                    website = website,
                    facebook = facebook,
                    instagram = instagram,
                    tiktok = tiktok,
                    village = village,
                    userId = userId,
                    createdBy = data["createdBy"] as? String ?: "",
                    latitude = latitude,
                    longitude = longitude
                )
            } catch (e: Exception) {
                null
            }
        }

        fun fromEntity(entity: PhoneNumberEntity, categoryId: Long = 0L, categoryName: String = ""): FirestoreNumber {
            return FirestoreNumber(
                id = entity.firestoreId,
                name = entity.name,
                phone = entity.phoneNumber,
                whatsapp = entity.whatsapp,
                description = entity.description,
                categoryId = if (categoryId != 0L) categoryId else entity.primaryCategoryId,
                categoryName = categoryName.ifBlank { entity.primaryCategoryName },
                address = entity.address,
                createdAt = entity.createdAt,
                updatedAt = System.currentTimeMillis(),
                isActive = entity.status == "approved",
                isVerified = entity.isVerified,
                status = entity.status,
                viewsCount = entity.viewsCount,
                callCount = entity.callCount,
                whatsappCount = entity.whatsappCount,
                shareCount = entity.shareCount,
                workingHours = entity.workingHours,
                website = entity.website,
                facebook = entity.facebook,
                instagram = entity.instagram,
                tiktok = entity.tiktok,
                village = entity.village,
                userId = entity.userId,
                createdBy = if (entity.userId > 0) entity.userId.toString() else FirebaseAuthManager.getCurrentUid(),
                latitude = entity.latitude,
                longitude = entity.longitude
            )
        }
    }
}
