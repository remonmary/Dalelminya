package com.example.ui.screens.admin

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PhoneNumberEntity
import com.example.ui.theme.GoldenStar
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusRejected
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuspended
import com.example.ui.viewmodel.DirectoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNumbersScreen(
    viewModel: DirectoryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allNumbers by viewModel.allPhoneNumbers.collectAsState()
    val featuredList by viewModel.featuredNumbers.collectAsState()

    var filterStatus by remember { mutableStateOf<String?>(null) }

    val filteredList = if (filterStatus == null) allNumbers else {
        allNumbers.filter { it.status == filterStatus }
    }

    val pinnedIds = featuredList.filter { it.status == "active" }.map { it.phoneId }.toSet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة الأرقام (${allNumbers.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_numbers_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = (filterStatus == null),
                        onClick = { filterStatus = null },
                        label = { Text("الكل (${allNumbers.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = (filterStatus == "approved"),
                        onClick = { filterStatus = "approved" },
                        label = { Text("معتمد (${allNumbers.count { it.status == "approved" }})") }
                    )
                }
                item {
                    FilterChip(
                        selected = (filterStatus == "pending"),
                        onClick = { filterStatus = "pending" },
                        label = { Text("معلق (${allNumbers.count { it.status == "pending" }})") }
                    )
                }
                item {
                    FilterChip(
                        selected = (filterStatus == "rejected"),
                        onClick = { filterStatus = "rejected" },
                        label = { Text("مرفوض (${allNumbers.count { it.status == "rejected" }})") }
                    )
                }
                item {
                    FilterChip(
                        selected = (filterStatus == "suspended"),
                        onClick = { filterStatus = "suspended" },
                        label = { Text("موقوف (${allNumbers.count { it.status == "suspended" }})") }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList) { phone ->
                    AdminNumberCard(
                        phone = phone,
                        isPinned = phone.id in pinnedIds,
                        onToggleVerified = { viewModel.adminToggleVerified(phone.id, phone.isVerified) },
                        onTogglePinned = {
                            if (phone.id in pinnedIds) {
                                viewModel.adminUnpinNumber(phone.id)
                            } else {
                                viewModel.adminPinNumber(phone.id, null, isGlobal = true, sortOrder = 1)
                            }
                        },
                        onToggleStatus = {
                            val newStatus = if (phone.status == "suspended") "approved" else "suspended"
                            viewModel.adminToggleStatus(phone.id, newStatus)
                        },
                        onDelete = { viewModel.adminDeletePhone(phone.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminNumberCard(
    phone: PhoneNumberEntity,
    isPinned: Boolean,
    onToggleVerified: () -> Unit,
    onTogglePinned: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = phone.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                val (badgeText, badgeColor) = when (phone.status) {
                    "approved" -> Pair("معتمد", StatusSuccess)
                    "pending" -> Pair("معلق", StatusPending)
                    "rejected" -> Pair("مرفوض", StatusRejected)
                    else -> Pair("موقوف", StatusSuspended)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("الهاتف: ${phone.phoneNumber}", fontSize = 13.sp, color = PrimaryBlue)
            Text("العنوان: ${phone.address}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(10.dp))

            // Action Toggles
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Verified Toggle
                IconButton(onClick = onToggleVerified) {
                    Icon(
                        imageVector = if (phone.isVerified) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "توثيق",
                        tint = if (phone.isVerified) StatusSuccess else Color.Gray
                    )
                }

                // Pinned Toggle ⭐
                IconButton(onClick = onTogglePinned) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "تثبيت",
                        tint = if (isPinned) GoldenStar else Color.Gray
                    )
                }

                // Suspend / Activate Toggle
                IconButton(onClick = onToggleStatus) {
                    Icon(
                        imageVector = if (phone.status == "suspended") Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                        contentDescription = "إيقاف/تفعيل",
                        tint = if (phone.status == "suspended") StatusSuccess else StatusSuspended
                    )
                }

                // Delete
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
