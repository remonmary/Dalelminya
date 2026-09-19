package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.GoldenStar
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusRejected
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.DirectoryViewModel

data class AdminMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
    val iconColor: Color,
    val onClick: () -> Unit,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: DirectoryViewModel,
    onBack: () -> Unit,
    onNavigateToSubmissions: () -> Unit,
    onNavigateToNumbers: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToBanners: () -> Unit,
    onNavigateToFeatured: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToReviews: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsState()
    val pendingList by viewModel.pendingSubmissions.collectAsState()
    val reportsList by viewModel.allReports.collectAsState()

    var showBroadcastDialog by remember { mutableStateOf(false) }
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastBody by remember { mutableStateOf("") }
    var isSendingNotification by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshStats()
    }

    val pendingReportsCount = reportsList.count { it.status == "PENDING" }

    val menuItems = listOf(
        AdminMenuItem(
            title = "طلبات الإضافة المعلقة",
            subtitle = "${pendingList.size} طلب بحاجة لمراجعة",
            icon = Icons.Default.PendingActions,
            badgeCount = pendingList.size,
            iconColor = StatusPending,
            onClick = onNavigateToSubmissions,
            testTag = "admin_nav_submissions"
        ),
        AdminMenuItem(
            title = "إدارة جميع الأرقام",
            subtitle = "${stats.totalNumbers} رقم مسجل بالدليل",
            icon = Icons.Default.Phone,
            iconColor = PrimaryBlue,
            onClick = onNavigateToNumbers,
            testTag = "admin_nav_numbers"
        ),
        AdminMenuItem(
            title = "الأرقام المثبتة (المميزة)",
            subtitle = "${stats.featuredNumbers} رقم مثبت ⭐",
            icon = Icons.Default.Star,
            iconColor = GoldenStar,
            onClick = onNavigateToFeatured,
            testTag = "admin_nav_featured"
        ),
        AdminMenuItem(
            title = "إدارة الأقسام والخدمات",
            subtitle = "${stats.totalCategories} قسم نشط",
            icon = Icons.Default.Category,
            iconColor = Color(0xFF059669),
            onClick = onNavigateToCategories,
            testTag = "admin_nav_categories"
        ),
        AdminMenuItem(
            title = "إدارة البنرات الإعلانية",
            subtitle = "العروض والبانرات في الصفحة الرئيسية",
            icon = Icons.Default.PhotoLibrary,
            iconColor = Color(0xFF7C3AED),
            onClick = onNavigateToBanners,
            testTag = "admin_nav_banners"
        ),
        AdminMenuItem(
            title = "إدارة البلاغات والشكاوى",
            subtitle = "$pendingReportsCount بلاغ قيد المعالجة",
            icon = Icons.Default.Report,
            badgeCount = pendingReportsCount,
            iconColor = StatusRejected,
            onClick = onNavigateToReports,
            testTag = "admin_nav_reports"
        ),
        AdminMenuItem(
            title = "إدارة التقييمات والآراء",
            subtitle = "${stats.totalReviews} تقييم من الأهالي",
            icon = Icons.Default.Chat,
            iconColor = Color(0xFF0284C7),
            onClick = onNavigateToReviews,
            testTag = "admin_nav_reviews"
        ),
        AdminMenuItem(
            title = "إعدادات التطبيق",
            subtitle = "الاسم، الهوية، صلاحيات الإضافة",
            icon = Icons.Default.Settings,
            iconColor = Color(0xFF475569),
            onClick = onNavigateToSettings,
            testTag = "admin_nav_settings"
        ),
        AdminMenuItem(
            title = "سجل عمليات الإدارة (Logs)",
            subtitle = "متابعة كافة الإجراءات الإدارية",
            icon = Icons.Default.History,
            iconColor = Color(0xFFD97706),
            onClick = onNavigateToLogs,
            testTag = "admin_nav_logs"
        ),
        AdminMenuItem(
            title = "إرسال إشعار عام للمواطنين",
            subtitle = "بث تنبيه لجميع أهالي المنيا",
            icon = Icons.Default.Campaign,
            iconColor = Color(0xFF8B5CF6),
            onClick = { showBroadcastDialog = true },
            testTag = "admin_nav_broadcast"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لوحة التحكم الشاملة (Admin)", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_dashboard_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Grid Overview
            item {
                Text(
                    text = "إحصائيات المنصة الحية",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminStatCard(
                        title = "إجمالي الأرقام",
                        value = "${stats.totalNumbers}",
                        color = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "طلبات معلقة",
                        value = "${pendingList.size}",
                        color = if (pendingList.isNotEmpty()) StatusPending else Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "أرقام موثقة",
                        value = "${stats.verifiedNumbers}",
                        color = StatusSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminStatCard(
                        title = "أرقام مثبتة",
                        value = "${stats.featuredNumbers}",
                        color = GoldenStar,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "المشاهدات",
                        value = "${stats.totalViews}",
                        color = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "الأقسام",
                        value = "${stats.totalCategories}",
                        color = Color(0xFF059669),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section Divider
            item {
                Text(
                    text = "أقسام الإدارة والتحكم",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Menu Items List
            items(menuItems.size) { index ->
                val item = menuItems[index]
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = item.onClick)
                        .testTag(item.testTag)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(item.iconColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = item.iconColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    )
                                    if (item.badgeCount > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Red
                                        ) {
                                            Text(
                                                text = "${item.badgeCount}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBroadcastDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSendingNotification) showBroadcastDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال إشعار عام للأهالي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "سيتم إرسال هذا الإشعار لجميع مستخدمي دليل أرقام المنيا وحفظه في سجل الإشعارات.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = broadcastTitle,
                        onValueChange = { broadcastTitle = it },
                        label = { Text("عنوان الإشعار (مثال: تنويه هام)") },
                        modifier = Modifier.fillMaxWidth().testTag("broadcast_title_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = broadcastBody,
                        onValueChange = { broadcastBody = it },
                        label = { Text("نص الإشعار") },
                        modifier = Modifier.fillMaxWidth().testTag("broadcast_body_input"),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (broadcastTitle.isNotBlank() && broadcastBody.isNotBlank()) {
                            isSendingNotification = true
                            viewModel.sendBroadcastNotification(broadcastTitle, broadcastBody)
                            isSendingNotification = false
                            showBroadcastDialog = false
                            broadcastTitle = ""
                            broadcastBody = ""
                        }
                    },
                    enabled = broadcastTitle.isNotBlank() && broadcastBody.isNotBlank() && !isSendingNotification,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.testTag("broadcast_send_button")
                ) {
                    if (isSendingNotification) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("إرسال الآن")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
