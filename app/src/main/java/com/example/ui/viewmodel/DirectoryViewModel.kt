package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.InitialDataSeeder
import com.example.data.model.AdminLogEntity
import com.example.data.model.BannerEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.DirectoryStats
import com.example.data.model.FeaturedNumberEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.PhoneNumberEntity
import com.example.data.model.PhoneNumberWithCategories
import com.example.data.model.ReportEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.SettingEntity
import com.example.data.model.UserEntity
import com.example.data.remote.FirebaseAuthManager
import com.example.data.repository.DirectoryRepository
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppConfig(
    val appName: String = "دليل أرقام المنيا",
    val slogan: String = "كل الأرقام .. في مكان واحد",
    val contactPhone: String = "01000000000",
    val allowPublicSubmissions: Boolean = true,
    val allowReviews: Boolean = true
)

class DirectoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DirectoryRepository
    val isInitialized = MutableStateFlow(false)

    private val prefs = application.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)

    // Current Auth User
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Dark Mode with Persistence
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    // Toast / Feedback message flow
    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent: SharedFlow<String> = _messageEvent.asSharedFlow()

    // Search query & results
    val searchQuery = MutableStateFlow("")
    val searchCategoryFilter = MutableStateFlow<Long?>(null)

    // Statistics
    private val _stats = MutableStateFlow(DirectoryStats())
    val stats: StateFlow<DirectoryStats> = _stats.asStateFlow()

    // Firestore Connection Diagnostic State
    private val _firestoreConnectionStatus = MutableStateFlow<Pair<Boolean?, String>>(Pair(true, "Firebase متصل ويعمل"))
    val firestoreConnectionStatus: StateFlow<Pair<Boolean?, String>> = _firestoreConnectionStatus.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DirectoryRepository(database.directoryDao())

        viewModelScope.launch(Dispatchers.IO) {
            InitialDataSeeder.seedIfNeeded(database.directoryDao())
            // Check if admin was previously logged in
            val savedUserId = prefs.getLong("logged_user_id", -1L)
            if (savedUserId > 0) {
                _currentUser.value = repository.getUserById(savedUserId)
            }
            refreshStats()
            isInitialized.value = true

            // Retry any pending offline submissions
            repository.syncPendingSubmissions()

            // If a Firebase Admin session already exists, hydrate the admin cache.
            if (FirebaseAuthManager.isAdmin.value) {
                repository.refreshAdminNumbers()
            } else {
                launch {
                    FirebaseAuthManager.isAdmin.filter { it }.first()
                    repository.refreshAdminNumbers()
                }
            }
        }
    }

    fun runFirestoreDiagnostic() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.testFirestoreConnection()
                _firestoreConnectionStatus.value = Pair(result.first, result.second)
            } catch (e: Exception) {
                _firestoreConnectionStatus.value = Pair(false, "خطأ في فحص الاتصال: ${e.message}")
            }
        }
    }

    // Categories
    val visibleCategories: StateFlow<List<CategoryEntity>> = repository.getVisibleCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Phone Numbers
    val approvedPhoneNumbers: StateFlow<List<PhoneNumberEntity>> = repository.getApprovedPhoneNumbers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPhoneNumbers: StateFlow<List<PhoneNumberEntity>> = repository.getAllPhoneNumbers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Featured / Pinned
    val featuredNumbers: StateFlow<List<FeaturedNumberEntity>> = repository.getAllFeatured()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Banners
    val activeBanners: StateFlow<List<BannerEntity>> = repository.getActiveBanners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBanners: StateFlow<List<BannerEntity>> = repository.getAllBanners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pending Submissions for Admin
    val pendingSubmissions: StateFlow<List<PhoneNumberEntity>> = repository.getPhoneNumbersByStatus("pending")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reports
    val allReports: StateFlow<List<ReportEntity>> = repository.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reviews
    val allReviews: StateFlow<List<ReviewEntity>> = repository.getAllReviews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Logs
    val adminLogs: StateFlow<List<AdminLogEntity>> = repository.getAdminLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App Settings
    val appSettings: StateFlow<List<SettingEntity>> = repository.getAllSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appConfig: StateFlow<AppConfig> = repository.getAllSettings().map { list ->
        val map = list.associate { it.key to it.value }
        AppConfig(
            appName = map["app_name"] ?: "دليل أرقام نزلة عبيد",
            slogan = map["slogan"] ?: "كل الأرقام .. في مكان واحد",
            contactPhone = map["contact_phone"] ?: "01000000000",
            allowPublicSubmissions = (map["allow_public_submissions"] ?: "true") == "true",
            allowReviews = (map["allow_reviews"] ?: "true") == "true"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    fun updateConfig(newConfig: AppConfig, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSetting("app_name", newConfig.appName)
            repository.saveSetting("slogan", newConfig.slogan)
            repository.saveSetting("contact_phone", newConfig.contactPhone)
            repository.saveSetting("allow_public_submissions", newConfig.allowPublicSubmissions.toString())
            repository.saveSetting("allow_reviews", newConfig.allowReviews.toString())
            postMessage("تم حفظ إعدادات التطبيق بنجاح")
            onDone()
        }
    }

    // Favorites
    val favoritePhoneIds: StateFlow<List<Long>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getFavoriteIds(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritePhones: StateFlow<List<PhoneNumberEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getFavoritePhones(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User Submissions
    val mySubmissions: StateFlow<List<PhoneNumberEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getUserSubmissions(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications
    val myNotifications: StateFlow<List<NotificationEntity>> = _currentUser.flatMapLatest { user: UserEntity? ->
        if (user != null) repository.getNotifications(user.id) else flowOf<List<NotificationEntity>>(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactive Combined Search
    val searchResults: StateFlow<List<PhoneNumberEntity>> = combine(
        searchQuery.debounce(250).distinctUntilChanged(),
        searchCategoryFilter,
        approvedPhoneNumbers
    ) { query, catFilter, approved ->
        var list = if (query.isBlank()) approved else {
            approved.filter { phone ->
                phone.name.contains(query, ignoreCase = true) ||
                phone.phoneNumber.contains(query) ||
                phone.description.contains(query, ignoreCase = true) ||
                phone.address.contains(query, ignoreCase = true) ||
                phone.village.contains(query, ignoreCase = true)
            }
        }
        if (catFilter != null) {
            // Filter by category in IO
            list = list.filter { phone ->
                val cats = repository.getCategoriesForPhoneSync(phone.id)
                cats.any { it.id == catFilter }
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun postMessage(msg: String) {
        viewModelScope.launch { _messageEvent.emit(msg) }
    }

    fun refreshStats() {
        viewModelScope.launch(Dispatchers.IO) {
            _stats.value = repository.getStats()
        }
    }

    // --- Authentication ---
    fun login(identifier: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.login(identifier, pass)
            res.onSuccess { user ->
                _currentUser.value = user
                prefs.edit().putLong("logged_user_id", user.id).apply()
                // If user has email, authenticate with Firebase Auth as well
                if (user.email.isNotBlank()) {
                    FirebaseAuthManager.signInWithEmail(user.email, pass)
                }
                onResult(true, "تم تسجيل الدخول بنجاح")
                postMessage("أهلاً بك يا ${user.name}")
            }.onFailure {
                // If local login failed, try direct Firebase Auth login if identifier looks like email
                if (identifier.contains("@")) {
                    val fbResult = FirebaseAuthManager.signInWithEmail(identifier, pass)
                    fbResult.onSuccess { fbUser ->
                        if (FirebaseAuthManager.isAdmin.value) {
                            repository.refreshAdminNumbers()
                            onResult(true, "تم تسجيل الدخول كمسؤول بنجاح")
                            postMessage("أهلاً بك يا أدمن!")
                            return@launch
                        }
                        onResult(false, "الحساب لا يملك صلاحيات الإدارة")
                        return@launch
                    }
                }
                onResult(false, it.message ?: "فشل تسجيل الدخول")
            }
        }
    }

    fun register(name: String, phone: String, email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.register(name, phone, email, pass)
            res.onSuccess {
                _currentUser.value = it
                prefs.edit().putLong("logged_user_id", it.id).apply()
                onResult(true, "تم إنشاء الحساب بنجاح")
                postMessage("تم إنشاء الحساب بنجاح!")
            }.onFailure {
                onResult(false, it.message ?: "فشل إنشاء الحساب")
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        prefs.edit().remove("logged_user_id").apply()
        FirebaseAuthManager.signOut()
        postMessage("تم تسجيل الخروج")
    }

    // --- Phone Actions Counters ---
    fun recordView(phoneId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementViews(phoneId)
        }
    }

    fun recordCall(phoneId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementCalls(phoneId)
        }
    }

    fun recordWhatsapp(phoneId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementWhatsapp(phoneId)
        }
    }

    fun recordShare(phoneId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementShares(phoneId)
        }
    }

    // --- Favorites ---
    fun toggleFavorite(phoneId: Long) {
        val user = _currentUser.value ?: run {
            postMessage("يرجى تسجيل الدخول أولاً لإضافة الرقم للمفضلة")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val added = repository.toggleFavorite(user.id, phoneId)
            postMessage(if (added) "تمت الإضافة إلى المفضلة ❤️" else "تمت الإزالة من المفضلة")
        }
    }

    // --- Submit New Number ---
    fun submitNumber(
        name: String,
        phone: String,
        whatsapp: String,
        categoryIds: List<Long>,
        address: String,
        village: String,
        description: String,
        workingHours: String,
        website: String,
        facebook: String,
        instagram: String,
        tiktok: String,
        latitude: Double? = null,
        longitude: Double? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val user = _currentUser.value
        val isAdmin = user?.role == "ADMIN" || user?.role == "SUPER_ADMIN"
        val status = if (isAdmin) "approved" else "pending"

        val entity = PhoneNumberEntity(
            name = name.trim(),
            slug = name.trim().lowercase().replace("\\s+".toRegex(), "-"),
            phoneNumber = phone.trim(),
            whatsapp = whatsapp.trim().ifBlank { phone.trim() },
            address = address.trim(),
            village = village.trim(),
            description = description.trim(),
            workingHours = workingHours.trim().ifBlank { "9:00 ص - 10:00 م" },
            website = website.trim(),
            facebook = facebook.trim(),
            instagram = instagram.trim(),
            tiktok = tiktok.trim(),
            latitude = latitude ?: 0.0,
            longitude = longitude ?: 0.0,
            isVerified = isAdmin,
            status = status,
            userId = user?.id ?: 0
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.submitPhoneNumber(entity, categoryIds)
                refreshStats()
                val successMessage = if (isAdmin) "تمت إضافة الرقم ونشره بنجاح ✅" else "تم إرسال الرقم للمراجعة وسيظهر بعد موافقة الإدارة"
                postMessage(successMessage)
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (fe: FirebaseFirestoreException) {
                val err = "فشل الإرسال إلى Firestore: [${fe.code}] ${fe.message}"
                android.util.Log.e("DirectoryViewModel", "FirebaseFirestoreException in submitNumber: Code=${fe.code}, Message=${fe.message}", fe)
                postMessage(err)
                withContext(Dispatchers.Main) {
                    onError(err)
                }
            } catch (fe: FirebaseException) {
                val err = "فشل الاتصال بـ Firebase: ${fe.message}"
                android.util.Log.e("DirectoryViewModel", "FirebaseException in submitNumber: Message=${fe.message}", fe)
                postMessage(err)
                withContext(Dispatchers.Main) {
                    onError(err)
                }
            } catch (e: Exception) {
                val err = "حدث خطأ أثناء إرسال الرقم: ${e.message}"
                android.util.Log.e("DirectoryViewModel", "Exception in submitNumber: Message=${e.message}", e)
                postMessage(err)
                withContext(Dispatchers.Main) {
                    onError(err)
                }
            }
        }
    }

    // --- Admin Operations ---
    fun adminApprove(phoneId: Long) {
        val adminName = _currentUser.value?.name ?: "المدير"
        viewModelScope.launch(Dispatchers.IO) {
            repository.approveSubmission(phoneId, adminName)
            refreshStats()
            postMessage("تم قبول الرقم ونشره في الدليل ✅")
        }
    }

    fun adminReject(phoneId: Long, reason: String) {
        val adminName = _currentUser.value?.name ?: "المدير"
        viewModelScope.launch(Dispatchers.IO) {
            repository.rejectSubmission(phoneId, reason, adminName)
            refreshStats()
            postMessage("تم رفض الطلب وتسجيل السبب.")
        }
    }

    fun adminToggleVerified(phoneId: Long, currentVerified: Boolean) {
        val adminName = _currentUser.value?.name ?: "المدير"
        viewModelScope.launch(Dispatchers.IO) {
            repository.setVerified(phoneId, !currentVerified, adminName)
            refreshStats()
            postMessage(if (!currentVerified) "تم توثيق الرقم ✓" else "تم إلغاء التوثيق")
        }
    }

    fun adminToggleStatus(phoneId: Long, newStatus: String) {
        val adminName = _currentUser.value?.name ?: "المدير"
        viewModelScope.launch(Dispatchers.IO) {
            repository.setStatus(phoneId, newStatus, adminName)
            refreshStats()
            postMessage("تم تغيير حالة الرقم إلى $newStatus")
        }
    }

    fun adminDeletePhone(phoneId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePhoneNumber(phoneId)
            refreshStats()
            postMessage("تم حذف الرقم بنجاح")
        }
    }

    fun adminPinNumber(
        phoneId: Long,
        categoryId: Long?,
        isGlobal: Boolean,
        sortOrder: Int
    ) {
        val adminName = _currentUser.value?.name ?: "المدير"
        viewModelScope.launch(Dispatchers.IO) {
            repository.pinNumber(phoneId, categoryId, isGlobal, sortOrder, true, adminName)
            refreshStats()
            postMessage("تم تثبيت الرقم في الدليل ⭐")
        }
    }

    fun adminUnpinNumber(phoneId: Long) {
        val adminName = _currentUser.value?.name ?: "المدير"
        viewModelScope.launch(Dispatchers.IO) {
            repository.unpinNumber(phoneId, adminName)
            refreshStats()
            postMessage("تم إلغاء تثبيت الرقم")
        }
    }

    // --- Admin Category Operations ---
    fun adminSaveCategory(category: CategoryEntity, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCategory(category)
            refreshStats()
            postMessage("تم حفظ بيانات القسم بنجاح")
            onDone()
        }
    }

    fun adminDeleteCategory(categoryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(categoryId)
            refreshStats()
            postMessage("تم حذف القسم")
        }
    }

    // --- Admin Banner Operations ---
    fun adminSaveBanner(banner: BannerEntity, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveBanner(banner)
            postMessage("تم حفظ البنر بنجاح")
            onDone()
        }
    }

    fun adminDeleteBanner(bannerId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBanner(bannerId)
            postMessage("تم حذف البنر")
        }
    }

    // --- User Feedback & Reports ---
    fun submitReport(phoneId: Long, name: String, reason: String, details: String, onDone: () -> Unit) {
        val user = _currentUser.value
        viewModelScope.launch(Dispatchers.IO) {
            repository.submitReport(phoneId, user?.id ?: 0, name, reason, details)
            refreshStats()
            postMessage("تم استلام بلاغك وسيقوم فريق الإدارة بمراجعته. شكراً لحرصك.")
            onDone()
        }
    }

    fun adminResolveReport(reportId: Long, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateReportStatus(reportId, status)
            refreshStats()
            postMessage("تم تحديث حالة البلاغ")
        }
    }

    fun addReview(phoneId: Long, rating: Int, comment: String, onDone: () -> Unit) {
        val user = _currentUser.value ?: run {
            postMessage("يرجى تسجيل الدخول أولاً لإضافة تقييم")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.addReview(phoneId, user.id, user.name, rating, comment)
            refreshStats()
            postMessage("تم نشر تقييمك بنجاح ⭐")
            onDone()
        }
    }

    fun adminDeleteReview(reviewId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteReview(reviewId)
            refreshStats()
            postMessage("تم حذف التقييم")
        }
    }

    // --- Phone Details Provider ---
    suspend fun getPhoneDetails(phoneId: Long): PhoneNumberWithCategories? {
        return repository.getPhoneWithDetails(phoneId, _currentUser.value?.id ?: 0)
    }

    fun getReviewsForPhone(phoneId: Long): StateFlow<List<ReviewEntity>> {
        return repository.getReviewsForPhone(phoneId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun getApprovedPhonesForCategory(categoryId: Long): StateFlow<List<PhoneNumberEntity>> {
        return repository.getApprovedPhonesForCategory(categoryId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // --- Unread Notifications ---
    val unreadNotificationsCount: StateFlow<Int> = repository.getUnreadNotificationsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markAllNotificationsAsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAllNotificationsAsRead()
        }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markNotificationRead(id)
        }
    }

    fun sendBroadcastNotification(title: String, message: String, targetUrl: String = "", onDone: () -> Unit = {}) {
        val adminName = _currentUser.value?.name ?: "المدير"
        viewModelScope.launch(Dispatchers.IO) {
            repository.sendBroadcastNotification(title, message, targetUrl, adminName)
            postMessage("تم إرسال التنبيه العام بنجاح 📢")
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }
}
