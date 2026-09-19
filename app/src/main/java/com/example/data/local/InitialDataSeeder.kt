package com.example.data.local

import com.example.data.model.BannerEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.FeaturedNumberEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.PhoneNumberCategoryEntity
import com.example.data.model.PhoneNumberEntity
import com.example.data.model.SettingEntity
import com.example.data.model.UserEntity

object InitialDataSeeder {

    suspend fun seedIfNeeded(dao: DirectoryDao) {
        // Production build: no local admin/demo users are seeded.
        // Authentication and admin authorization are handled by Firebase Auth/Rules.
        var demoUserId = 0L

        // 2. Seed Categories & Numbers if empty
        if (dao.getCategoriesCount() == 0) {
            val categories = listOf(
                CategoryEntity(name = "موبايلات", slug = "mobile", description = "بيع وصيانة هواتف واكسسوارات محمول", iconName = "phone_android", sortOrder = 1),
                CategoryEntity(name = "صيانة", slug = "maintenance", description = "صيانة أجهزة كهربائية ومنازل وإلكترونيات", iconName = "build", sortOrder = 2),
                CategoryEntity(name = "مطاعم", slug = "restaurants", description = "أكلات سريعة، مشويات، بيتزا ومأكولات شعبية", iconName = "restaurant", sortOrder = 3),
                CategoryEntity(name = "محلات", slug = "shops", description = "سوبر ماركت، ملابس، خردوات وأدوات منزلية", iconName = "store", sortOrder = 4),
                CategoryEntity(name = "أطباء", slug = "doctors", description = "عيادات تخصصية واستشارات طبية بالمنيا", iconName = "medical_services", sortOrder = 5),
                CategoryEntity(name = "صيدليات", slug = "pharmacies", description = "أدوية ومستلزمات طبية وتوصيل للمنازل", iconName = "local_pharmacy", sortOrder = 6),
                CategoryEntity(name = "مدرسين", slug = "teachers", description = "دروس خصوصية وتأسيس لجميع المراحل التعليمية", iconName = "school", sortOrder = 7),
                CategoryEntity(name = "مواصلات", slug = "transport", description = "سائقين وتاكسي وميكروباص وخدمات توصيل", iconName = "directions_car", sortOrder = 8),
                CategoryEntity(name = "عقارات", slug = "real_estate", description = "بيع وإيجار شقق ومحلات وأراضي بالمنيا", iconName = "home", sortOrder = 9),
                CategoryEntity(name = "محامين", slug = "lawyers", description = "استشارات قانونية ومحاماة وقضايا", iconName = "gavel", sortOrder = 10),
                CategoryEntity(name = "خدمات", slug = "services", description = "خدمات حكومية وحرفية متنوعة", iconName = "handyman", sortOrder = 11),
                CategoryEntity(name = "شركات", slug = "companies", description = "مؤسسات تجارية ومكاتب خدمات بالمنيا", iconName = "business", sortOrder = 12)
            )

            val catIdMap = mutableMapOf<String, Long>()
            for (cat in categories) {
                val id = dao.insertCategory(cat)
                catIdMap[cat.slug] = id
            }

            // No demo phone numbers are seeded. Real numbers come from Firestore.
        }

        // 3. Seed Banners if empty
        if (dao.getBannersCount() == 0) {
            dao.insertBanner(
                BannerEntity(
                    title = "دليل أرقام المنيا",
                    description = "كل الأرقام والمحلات والخدمات في مكان واحد لخدمة أهلنا بمحافظة المنيا",
                    buttonText = "أضف رقمك الآن",
                    buttonUrl = "add-number",
                    targetType = "HOME",
                    sortOrder = 1,
                    isActive = true
                )
            )
            dao.insertBanner(
                BannerEntity(
                    title = "طوارئ وخدمات طبية سريعة",
                    description = "تصفح أرقام الأطباء والصيدليات والعيادات المتاحة على مدار 24 ساعة",
                    buttonText = "تصفح الأطباء",
                    buttonUrl = "category/doctors",
                    targetType = "HOME",
                    sortOrder = 2,
                    isActive = true
                )
            )
        }

        // 4. Seed Notifications if needed
        if (dao.getNotificationsForUser(demoUserId).let { false }) { /* skip */ }

        // 5. Seed Settings if needed
        if (dao.getSetting("app_name") == null) {
            dao.insertSetting(SettingEntity("app_name", "دليل أرقام المنيا"))
            dao.insertSetting(SettingEntity("app_slogan", "كل الأرقام .. في مكان واحد"))
            dao.insertSetting(SettingEntity("allow_submissions", "true"))
            dao.insertSetting(SettingEntity("allow_reviews", "true"))
            dao.insertSetting(SettingEntity("allow_registration", "true"))
            dao.insertSetting(SettingEntity("contact_phone", "01000000000"))
            dao.insertSetting(SettingEntity("about_text", "دليل أرقام المنيا هو دليل محلي يساعد أهل المحافظة في الوصول بسهولة إلى أرقام وخدمات ومحلات وأنشطة المنيا في مكان واحد."))
        }
    }
}
