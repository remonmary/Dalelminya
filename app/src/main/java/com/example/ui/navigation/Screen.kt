package com.example.ui.navigation

sealed class Screen(val route: String) {
    // Bottom Bar Primary Tabs
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Categories : Screen("categories")
    data object Favorites : Screen("favorites")
    data object Profile : Screen("profile")

    // Listing & Category Detail
    data object CategoryDetail : Screen("category/{categoryId}") {
        fun createRoute(categoryId: Long) = "category/$categoryId"
    }
    data object NumberDetail : Screen("number/{phoneId}") {
        fun createRoute(phoneId: Long) = "number/$phoneId"
    }

    // User Actions
    data object AddNumber : Screen("add-number")
    data object MySubmissions : Screen("my-submissions")
    data object Notifications : Screen("notifications")

    // Informational Pages
    data object About : Screen("about")
    data object Contact : Screen("contact")
    data object Terms : Screen("terms")
    data object Privacy : Screen("privacy")

    // Admin Screens
    data object AdminDashboard : Screen("admin/dashboard")
    data object AdminNumbers : Screen("admin/numbers")
    data object AdminSubmissions : Screen("admin/submissions")
    data object AdminCategories : Screen("admin/categories")
    data object AdminBanners : Screen("admin/banners")
    data object AdminFeatured : Screen("admin/featured")
    data object AdminReports : Screen("admin/reports")
    data object AdminReviews : Screen("admin/reviews")
    data object AdminSettings : Screen("admin/settings")
    data object AdminLogs : Screen("admin/logs")
}
