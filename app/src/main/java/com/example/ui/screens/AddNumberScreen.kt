package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.DirectoryViewModel

private val MINYA_CENTERS = listOf(
    "مدينة المنيا",
    "المنيا الجديدة",
    "نزلة عبيد",
    "ملوي",
    "سمالوط",
    "بني مزار",
    "مغاغة",
    "أبو قرقاص",
    "مطاي",
    "ديرمواس",
    "العدوة"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNumberScreen(
    viewModel: DirectoryViewModel,
    onBack: () -> Unit,
    onSuccessSubmitted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories by viewModel.visibleCategories.collectAsState()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("مدينة المنيا") }
    var village by remember { mutableStateOf("مدينة المنيا") }
    var description by remember { mutableStateOf("") }
    var workingHours by remember { mutableStateOf("9:00 ص - 10:00 م") }
    var website by remember { mutableStateOf("") }
    var facebook by remember { mutableStateOf("") }
    var instagram by remember { mutableStateOf("") }
    var tiktok by remember { mutableStateOf("") }

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var isLocating by remember { mutableStateOf(false) }

    val selectedCategoryIds = remember { mutableStateListOf<Long>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        isLocating = true
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gpsLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLocation: Location? = gpsLocation ?: networkLocation

            if (bestLocation != null) {
                latitude = bestLocation.latitude
                longitude = bestLocation.longitude
            } else {
                // Fallback default coordinates for Minya center if GPS has no cached fix
                latitude = 28.1099
                longitude = 30.7503
            }
        } catch (e: Exception) {
            latitude = 28.1099
            longitude = 30.7503
        } finally {
            isLocating = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchCurrentLocation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إضافة رقم أو نشاط جديد", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("add_number_back")) {
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
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Notice Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryBlue.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ملاحظة هامة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "سيتم مراجعة وتدقيق بيانات الرقم والتأكد من صحتها من قبل إدارة الدليل قبل نشره ليظهر لأهالي المنيا.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Name
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم النشاط / المحل / الشخص *") },
                    placeholder = { Text("مثال: صيدلية النيل أو هايبر ماركت الأهرام") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_input_name"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Phone
            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف الأساسي *") },
                    placeholder = { Text("مثال: 01012345678") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_input_phone"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // WhatsApp
            item {
                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text("رقم الواتساب (اختياري)") },
                    placeholder = { Text("اتركه فارغاً إذا كان مطابقاً للهاتف") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_input_whatsapp"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Categories Selection
            item {
                Text(
                    text = "اختر القسم أو الأقسام المناسبة للنشاط: *",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val isSelected = cat.id in selectedCategoryIds
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedCategoryIds.remove(cat.id)
                                else selectedCategoryIds.add(cat.id)
                            },
                            label = { Text(cat.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f),
                                selectedLabelColor = PrimaryBlue
                            )
                        )
                    }
                }
            }

            // Minya Center / Village Selection
            item {
                Text(
                    text = "اختر المركز أو المنطقة بمحافظة المنيا: *",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MINYA_CENTERS) { center ->
                        val isSelected = (village == center)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                village = center
                                if (address.isBlank() || address in MINYA_CENTERS) {
                                    address = center
                                }
                            },
                            label = { Text(center) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f),
                                selectedLabelColor = PrimaryBlue
                            )
                        )
                    }
                }
            }

            // Detailed Address
            item {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان التفصيلي *") },
                    placeholder = { Text("مثال: شارع طه حسين - برج الجامعة - المنيا") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // GPS Location Acquisition
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (latitude != null) StatusSuccess.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (latitude != null) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (latitude != null) StatusSuccess else PrimaryBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (latitude != null) "تم تحديد إحداثيات الموقع بنجاح" else "تحديد الموقع الجغرافي GPS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (latitude != null) StatusSuccess else MaterialTheme.colorScheme.onSurface
                                )
                                if (latitude != null && longitude != null) {
                                    Text(
                                        text = "${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasFine || hasCoarse) {
                                    fetchCurrentLocation()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isLocating
                        ) {
                            if (isLocating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (latitude != null) "تحديث GPS" else "تحديد الآن", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Working Hours
            item {
                OutlinedTextField(
                    value = workingHours,
                    onValueChange = { workingHours = it },
                    label = { Text("مواعيد وساعات العمل") },
                    placeholder = { Text("مثال: 10:00 ص - 11:00 م") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Description
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("نبذة عن الخدمات والمنتجات التي تقدمها") },
                    placeholder = { Text("اكتب تفاصيل تساعد العملاء في التعرف على نشاطك...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 5
                )
            }

            // Social links
            item {
                Text("روابط إضافية (اختياري)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = facebook,
                    onValueChange = { facebook = it },
                    label = { Text("صفحة فيسبوك") },
                    placeholder = { Text("facebook.com/...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("الموقع الإلكتروني") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Error display with full details
            if (errorMessage != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تنبيه خطأ في الإرسال:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Submit Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            errorMessage = "يرجى كتابة اسم النشاط أو الشخص"
                            return@Button
                        }
                        if (phone.isBlank()) {
                            errorMessage = "يرجى كتابة رقم الهاتف"
                            return@Button
                        }
                        if (selectedCategoryIds.isEmpty()) {
                            errorMessage = "يرجى اختيار قسم واحد على الأقل"
                            return@Button
                        }
                        errorMessage = null
                        isSubmitting = true

                        viewModel.submitNumber(
                            name = name,
                            phone = phone,
                            whatsapp = whatsapp,
                            categoryIds = selectedCategoryIds.toList(),
                            address = address,
                            village = village,
                            description = description,
                            workingHours = workingHours,
                            website = website,
                            facebook = facebook,
                            instagram = instagram,
                            tiktok = tiktok,
                            latitude = latitude,
                            longitude = longitude,
                            onSuccess = {
                                isSubmitting = false
                                onSuccessSubmitted()
                            },
                            onError = { err ->
                                isSubmitting = false
                                errorMessage = err
                            }
                        )
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_add_number_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "جارٍ حفظ وإرسال البيانات...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "إرسال طلب الإضافة للمراجعة 🚀",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
