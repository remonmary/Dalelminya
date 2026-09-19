package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity

fun getCategoryIcon(name: String): ImageVector {
    return when (name.lowercase()) {
        "phone_android", "mobile" -> Icons.Default.PhoneAndroid
        "build", "maintenance" -> Icons.Default.Build
        "restaurant", "food" -> Icons.Default.Restaurant
        "store", "shop" -> Icons.Default.Store
        "medical_services", "doctor" -> Icons.Default.MedicalServices
        "local_pharmacy", "pharmacy" -> Icons.Default.LocalPharmacy
        "school", "teacher" -> Icons.Default.School
        "directions_car", "transport" -> Icons.Default.DirectionsCar
        "home", "real_estate" -> Icons.Default.Home
        "gavel", "lawyer" -> Icons.Default.Gavel
        "handyman", "services" -> Icons.Default.Handyman
        "business", "company" -> Icons.Default.Business
        else -> Icons.Default.Category
    }
}

fun getCategoryColor(index: Int): Pair<Color, Color> {
    val palettes = listOf(
        Pair(Color(0xFF2563EB), Color(0xFFEFF6FF)), // Blue
        Pair(Color(0xFFD97706), Color(0xFFFEF3C7)), // Amber
        Pair(Color(0xFFDC2626), Color(0xFFFEE2E2)), // Red
        Pair(Color(0xFF059669), Color(0xFFD1FAE5)), // Emerald
        Pair(Color(0xFF7C3AED), Color(0xFFEDE9FE)), // Purple
        Pair(Color(0xFF0891B2), Color(0xFFCFFAFE)), // Cyan
        Pair(Color(0xFFE11D48), Color(0xFFFFE4E6)), // Rose
        Pair(Color(0xFF4F46E5), Color(0xFFEEF2FF)), // Indigo
        Pair(Color(0xFFCA8A04), Color(0xFFFEF9C3)), // Yellow
        Pair(Color(0xFF0D9488), Color(0xFFCCFBF1)), // Teal
        Pair(Color(0xFF9333EA), Color(0xFFF3E8FF)), // Violet
        Pair(Color(0xFF475569), Color(0xFFF1F5F9))  // Slate
    )
    return palettes[index % palettes.size]
}

@Composable
fun CategoryGridItem(
    category: CategoryEntity,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (primaryColor, bgColor) = getCategoryColor(index)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("category_card_${category.slug}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(category.iconName),
                    contentDescription = category.name,
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = category.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
