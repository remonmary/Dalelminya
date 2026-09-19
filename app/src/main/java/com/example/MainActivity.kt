package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.repository.DirectoryRepository
import com.example.ui.components.AppBottomBar
import com.example.ui.components.AppTopBar
import com.example.ui.navigation.Screen
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AddNumberScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CategoryDetailScreen
import com.example.ui.screens.ContactScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MySubmissionsScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.NumberDetailScreen
import com.example.ui.screens.PrivacyScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.TermsScreen
import com.example.ui.screens.admin.AdminBannersScreen
import com.example.ui.screens.admin.AdminCategoriesScreen
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.admin.AdminFeaturedScreen
import com.example.ui.screens.admin.AdminLogsScreen
import com.example.ui.screens.admin.AdminNumbersScreen
import com.example.ui.screens.admin.AdminReportsScreen
import com.example.ui.screens.admin.AdminReviewsScreen
import com.example.ui.screens.admin.AdminSettingsScreen
import com.example.ui.screens.admin.AdminSubmissionsScreen
import com.example.ui.theme.NazletObeidTheme
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.DirectoryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val application = context.applicationContext as android.app.Application
            val viewModel = remember { DirectoryViewModel(application) }
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val navController = rememberNavController()

            NazletObeidTheme(darkTheme = isDarkMode) {
                DirectoryApp(
                    viewModel = viewModel,
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun DirectoryApp(
    viewModel: DirectoryViewModel,
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val currentUser by viewModel.currentUser.collectAsState()
    val notifications by viewModel.myNotifications.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val unreadCount = notifications.count { !it.isRead }
    val firebaseIsAdmin by com.example.data.remote.FirebaseAuthManager.isAdmin.collectAsState()
    val isAdmin = firebaseIsAdmin

    // Primary Bottom Navigation destinations
    val mainRoutes = listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.Categories.route,
        Screen.Favorites.route,
        Screen.Profile.route
    )

    val isMainScreen = currentDestination in mainRoutes

    Scaffold(
        topBar = {
            if (isMainScreen) {
                AppTopBar(
                    currentUser = currentUser,
                    unreadNotificationsCount = unreadCount,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onSearchClick = { navController.navigate(Screen.Search.route) },
                    onAddClick = { navController.navigate(Screen.AddNumber.route) },
                    onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                    onAdminClick = { navController.navigate(Screen.AdminDashboard.route) }
                )
            }
        },
        bottomBar = {
            if (isMainScreen) {
                AppBottomBar(
                    currentRoute = currentDestination,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (currentDestination == Screen.Home.route || currentDestination == Screen.Categories.route) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.AddNumber.route) },
                    containerColor = PrimaryBlue,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("أضف رقمك", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("fab_add_number_main")
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Home
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                    onNavigateToCategory = { catId -> navController.navigate(Screen.CategoryDetail.createRoute(catId)) },
                    onNavigateToNumber = { phoneId -> navController.navigate(Screen.NumberDetail.createRoute(phoneId)) },
                    onNavigateToAddNumber = { navController.navigate(Screen.AddNumber.route) }
                )
            }

            // 2. Search
            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = viewModel,
                    onNavigateToNumber = { phoneId -> navController.navigate(Screen.NumberDetail.createRoute(phoneId)) }
                )
            }

            // 3. Categories
            composable(Screen.Categories.route) {
                CategoriesScreen(
                    viewModel = viewModel,
                    onNavigateToCategory = { catId -> navController.navigate(Screen.CategoryDetail.createRoute(catId)) }
                )
            }

            // 4. Favorites
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    viewModel = viewModel,
                    onNavigateToNumber = { phoneId -> navController.navigate(Screen.NumberDetail.createRoute(phoneId)) }
                )
            }

            // 5. Profile
            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToMySubmissions = { navController.navigate(Screen.MySubmissions.route) },
                    onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToAdminDashboard = { navController.navigate(Screen.AdminDashboard.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) },
                    onNavigateToContact = { navController.navigate(Screen.Contact.route) },
                    onNavigateToTerms = { navController.navigate(Screen.Terms.route) },
                    onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) }
                )
            }

            // 6. Category Detail
            composable(
                route = Screen.CategoryDetail.route,
                arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L
                CategoryDetailScreen(
                    categoryId = categoryId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToNumber = { phoneId -> navController.navigate(Screen.NumberDetail.createRoute(phoneId)) },
                    onNavigateToAddNumber = { navController.navigate(Screen.AddNumber.route) }
                )
            }

            // 7. Number Detail
            composable(
                route = Screen.NumberDetail.route,
                arguments = listOf(navArgument("phoneId") { type = NavType.LongType })
            ) { backStackEntry ->
                val phoneId = backStackEntry.arguments?.getLong("phoneId") ?: 0L
                NumberDetailScreen(
                    phoneId = phoneId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // 8. Add Number Screen
            composable(Screen.AddNumber.route) {
                AddNumberScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSuccessSubmitted = {
                        navController.popBackStack()
                        navController.navigate(Screen.MySubmissions.route)
                    }
                )
            }

            // 9. My Submissions
            composable(Screen.MySubmissions.route) {
                MySubmissionsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // 10. Notifications
            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // 11. Info Screens
            composable(Screen.About.route) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Contact.route) {
                ContactScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Terms.route) {
                TermsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Privacy.route) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }

            // 12. Admin Screens (Protected with AdminRouteGuard)
            composable(Screen.AdminDashboard.route) {
                AdminRouteGuard(
                    isAdmin = isAdmin,
                    onBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Screen.Profile.route) }
                ) {
                    AdminDashboardScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToSubmissions = { navController.navigate(Screen.AdminSubmissions.route) },
                        onNavigateToNumbers = { navController.navigate(Screen.AdminNumbers.route) },
                        onNavigateToCategories = { navController.navigate(Screen.AdminCategories.route) },
                        onNavigateToBanners = { navController.navigate(Screen.AdminBanners.route) },
                        onNavigateToFeatured = { navController.navigate(Screen.AdminFeatured.route) },
                        onNavigateToReports = { navController.navigate(Screen.AdminReports.route) },
                        onNavigateToReviews = { navController.navigate(Screen.AdminReviews.route) },
                        onNavigateToSettings = { navController.navigate(Screen.AdminSettings.route) },
                        onNavigateToLogs = { navController.navigate(Screen.AdminLogs.route) }
                    )
                }
            }

            composable(Screen.AdminSubmissions.route) {
                AdminRouteGuard(
                    isAdmin = isAdmin,
                    onBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Screen.Profile.route) }
                ) {
                    AdminSubmissionsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Screen.AdminNumbers.route) {
                AdminRouteGuard(
                    isAdmin = isAdmin,
                    onBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Screen.Profile.route) }
                ) {
                    AdminNumbersScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Screen.AdminCategories.route) {
                AdminRouteGuard(
                    isAdmin = isAdmin,
                    onBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Screen.Profile.route) }
                ) {
                    AdminCategoriesScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Screen.AdminBanners.route) {
                AdminRouteGuard(
                    isAdmin = isAdmin,
                    onBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Screen.Profile.route) }
                ) {
                    AdminBannersScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Screen.AdminFeatured.route) {
                AdminRouteGuard(
                    isAdmin = isAdmin,
                    onBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Screen.Profile.route) }
                ) {
                    AdminFeaturedScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Screen.AdminReports.route) {
                AdminRouteGuard(
                    isAdmin = isAdmin,
                    onBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Screen.Profile.route) }
                ) {
                    AdminReportsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Screen.AdminReviews.route) {
                AdminRouteGuard(
                    isAdmin = isAdmin,
                    onBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Screen.Profile.route) }
                ) {
                    AdminReviewsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Screen.AdminSettings.route) {
                AdminRouteGuard(
                    isAdmin = isAdmin,
                    onBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Screen.Profile.route) }
                ) {
                    AdminSettingsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Screen.AdminLogs.route) {
                AdminRouteGuard(
                    isAdmin = isAdmin,
                    onBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Screen.Profile.route) }
                ) {
                    AdminLogsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRouteGuard(
    isAdmin: Boolean,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    content: @Composable () -> Unit
) {
    if (isAdmin) {
        content()
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("صلاحية الوصول", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = "منطقة مخصصة لإدارة دليل المنيا",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "هذه الشاشة تتطلب صلاحيات المشرفين والمسؤولين. يرجى تسجيل الدخول بحساب المدير للوصول إليها.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onNavigateToLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("admin_guard_login_button")
                        ) {
                            Text("تسجيل دخول كمسؤول", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
