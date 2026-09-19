package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BannerCarousel
import com.example.ui.components.CategoryGridItem
import com.example.ui.components.PhoneNumberCard
import com.example.ui.theme.AccentGold
import com.example.ui.theme.GoldenStar
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.DirectoryViewModel

@Composable
fun HomeScreen(
    viewModel: DirectoryViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToCategory: (Long) -> Unit,
    onNavigateToNumber: (Long) -> Unit,
    onNavigateToAddNumber: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.visibleCategories.collectAsState()
    val approvedNumbers by viewModel.approvedPhoneNumbers.collectAsState()
    val featuredNumbers by viewModel.featuredNumbers.collectAsState()
    val banners by viewModel.activeBanners.collectAsState()
    val favoriteIds by viewModel.favoritePhoneIds.collectAsState()
    val firestoreStatus by viewModel.firestoreConnectionStatus.collectAsState()

    // Find pinned numbers
    val pinnedPhoneIds = featuredNumbers.filter { it.status == "active" }.map { it.phoneId }.toSet()
    val pinnedNumbers = approvedNumbers.filter { it.id in pinnedPhoneIds }
    val regularNumbers = approvedNumbers.filter { it.id !in pinnedPhoneIds }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. Search Bar Shortcut Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onNavigateToSearch)
                        .testTag("home_search_bar")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ابحث عن اسم، نشاط، محل، أو رقم هاتف بالمنيا...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Active Banners Carousel
            if (banners.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    BannerCarousel(
                        banners = banners,
                        onBannerClick = { banner ->
                            if (banner.buttonUrl == "add-number") {
                                onNavigateToAddNumber()
                            } else if (banner.buttonUrl.startsWith("category/")) {
                                val slug = banner.buttonUrl.removePrefix("category/")
                                val found = categories.find { it.slug == slug }
                                if (found != null) onNavigateToCategory(found.id)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // 3. Categories Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "الأقسام والخدمات",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    TextButton(onClick = onNavigateToCategories) {
                        Text(
                            text = "عرض الكل (${categories.size})",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Horizontal row of top categories
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(categories.take(8)) { index, cat ->
                        Box(modifier = Modifier.width(96.dp)) {
                            CategoryGridItem(
                                category = cat,
                                index = index,
                                onClick = { onNavigateToCategory(cat.id) }
                            )
                        }
                    }
                }
            }

            // 4. Featured / Pinned Numbers Section ⭐
            if (pinnedNumbers.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldenStar,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "الأرقام والأنشطة المميزة (المثبتة)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                items(pinnedNumbers) { phone ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        PhoneNumberCard(
                            phone = phone,
                            isFeatured = true,
                            isFavorite = phone.id in favoriteIds,
                            onCardClick = {
                                viewModel.recordView(phone.id)
                                onNavigateToNumber(phone.id)
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(phone.id) },
                            onCallClick = { viewModel.recordCall(phone.id) },
                            onWhatsAppClick = { viewModel.recordWhatsapp(phone.id) },
                            onShareClick = { viewModel.recordShare(phone.id) }
                        )
                    }
                }
            }

            // 5. Recent Verified Listings Section
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "دليل الأرقام المعتمدة",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    )
                    Text(
                        text = "${regularNumbers.size} رقم متاح",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (regularNumbers.isEmpty() && pinnedNumbers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "لا توجد أرقام مضافة بعد",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "كن أول من يضيف نشاطه أو محله في دليل أرقام المنيا",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(regularNumbers) { phone ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        PhoneNumberCard(
                            phone = phone,
                            isFeatured = false,
                            isFavorite = phone.id in favoriteIds,
                            onCardClick = {
                                viewModel.recordView(phone.id)
                                onNavigateToNumber(phone.id)
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(phone.id) },
                            onCallClick = { viewModel.recordCall(phone.id) },
                            onWhatsAppClick = { viewModel.recordWhatsapp(phone.id) },
                            onShareClick = { viewModel.recordShare(phone.id) }
                        )
                    }
                }
            }
        }

        // Floating Action Button (أضف رقمك)
        ExtendedFloatingActionButton(
            onClick = onNavigateToAddNumber,
            icon = { Icon(Icons.Default.Add, contentDescription = "أضف رقمك") },
            text = { Text("أضف رقمك مجانًا", fontWeight = FontWeight.Bold) },
            containerColor = PrimaryBlue,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("home_fab_add_number")
        )
    }
}
