package com.example.data.model

data class PhoneNumberWithCategories(
    val number: PhoneNumberEntity,
    val categories: List<CategoryEntity> = emptyList(),
    val isFeatured: Boolean = false,
    val isGlobalFeatured: Boolean = false,
    val featuredSortOrder: Int = 0,
    val ratingAvg: Float = 5.0f,
    val ratingCount: Int = 0,
    val isFavorite: Boolean = false
)

data class DirectoryStats(
    val totalNumbers: Int = 0,
    val activeNumbers: Int = 0,
    val pendingNumbers: Int = 0,
    val verifiedNumbers: Int = 0,
    val featuredNumbers: Int = 0,
    val totalUsers: Int = 0,
    val totalCategories: Int = 0,
    val totalReports: Int = 0,
    val totalReviews: Int = 0,
    val totalViews: Int = 0
)

data class SearchFilter(
    val query: String = "",
    val categoryId: Long? = null,
    val onlyVerified: Boolean = false,
    val onlyOpenNow: Boolean = false
)
