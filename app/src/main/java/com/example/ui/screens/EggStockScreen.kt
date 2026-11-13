package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EggRecord
import com.example.data.model.EggSummary
import com.example.ui.Language
import com.example.util.AntiCorruptionPolicy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val VividGreen = Color(0xFF10B981)
private val VividAmber = Color(0xFFF59E0B)
private val VividRed = Color(0xFFEF4444)
private val VividBlue = Color(0xFF3B82F6)

@Composable
fun EggStockScreen(
    eggRecords: List<EggRecord>,
    eggSummary: EggSummary,
    currentMonth: String,
    isAdmin: Boolean,
    language: Language,
    onSaveEggRecord: (EggRecord) -> Unit,
    onDeleteEggRecord: (recordId: String, info: String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<EggRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<EggRecord?>(null) }
    var showLockedDialog by remember { mutableStateOf(false) }
    var lockedDialogMessage by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = {
                        recordToEdit = null
                        showAddDialog = true
                    },
                    containerColor = VividAmber,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_egg_record_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Egg Record")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("egg_stock_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Read-Only Banner for non-admin viewers
            if (!isAdmin) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (language == Language.BENGALI)
                                    "🔒 ইউজার ভিউ মোড: আপনি সাধারণ সদস্য হিসেবে আছেন। ডিমের সকল হিসাব ও মজুদ শুধুমাত্র দেখতে পারবেন। পরিবর্তন করার অনুমতি শুধুমাত্র মেস ম্যানেজারের।"
                                else
                                    "🔒 Member View Mode: You are viewing egg stock as a member (read-only). Only the manager can record egg purchases, consumption, or damage.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 2. High-Impact Stock Dashboard Card (Auto-calculated current stock)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (eggSummary.currentStock <= 5)
                            VividRed.copy(alpha = 0.12f)
                        else
                            VividGreen.copy(alpha = 0.12f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (eggSummary.currentStock <= 5) VividRed.copy(alpha = 0.4f) else VividGreen.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (language == Language.BENGALI) "বর্তমান অবশিষ্ট ডিমের মজুদ" else "Current Egg Stock Available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${eggSummary.currentStock} ${if (language == Language.BENGALI) "টি ডিম" else "Eggs"}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (eggSummary.currentStock <= 5) VividRed else VividGreen
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (eggSummary.currentStock <= 5) VividRed.copy(alpha = 0.2f)
                                        else VividGreen.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (eggSummary.currentStock <= 5) Icons.Default.Warning else Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = if (eggSummary.currentStock <= 5) VividRed else VividGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (language == Language.BENGALI)
                                "স্বয়ংক্রিয় হিসাব: মোট ক্রয় (${eggSummary.totalPurchased}) - খাওয়া (${eggSummary.totalConsumed}) - নষ্ট (${eggSummary.totalDamaged}) = ${eggSummary.currentStock} টি"
                            else
                                "Auto Calculation: Total Bought (${eggSummary.totalPurchased}) - Eaten (${eggSummary.totalConsumed}) - Damaged (${eggSummary.totalDamaged}) = ${eggSummary.currentStock}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 3. Stat Breakdown Cards: মোট ক্রয়, মোট খাওয়া, নষ্ট/ভাঙা, মোট খরচ
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Purchased
                    StatPillCard(
                        title = if (language == Language.BENGALI) "মোট কেনা" else "Purchased",
                        value = "${eggSummary.totalPurchased}",
                        unit = if (language == Language.BENGALI) "টি" else "pcs",
                        color = VividBlue,
                        modifier = Modifier.weight(1f)
                    )

                    // Total Consumed
                    StatPillCard(
                        title = if (language == Language.BENGALI) "মোট খাওয়া" else "Consumed",
                        value = "${eggSummary.totalConsumed}",
                        unit = if (language == Language.BENGALI) "টি" else "pcs",
                        color = VividAmber,
                        modifier = Modifier.weight(1f)
                    )

                    // Total Damaged / Spoiled
                    StatPillCard(
                        title = if (language == Language.BENGALI) "নষ্ট / পচা" else "Damaged",
                        value = "${eggSummary.totalDamaged}",
                        unit = if (language == Language.BENGALI) "টি" else "pcs",
                        color = VividRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Total Egg Expenditure Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = VividAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == Language.BENGALI) "ডিম বাবদ সর্বমোট খরচ" else "Total Egg Expenditure",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "৳ %.0f", eggSummary.totalCost),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = VividAmber
                        )
                    }
                }
            }

            // 5. Header for Records List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (language == Language.BENGALI) "ডিম ক্রয় ও খরচের দৈনিক ইতিহাস" else "Daily Egg Log & History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${eggRecords.size} ${if (language == Language.BENGALI) "টি এন্ট্রি" else "entries"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Empty state if no records
            if (eggRecords.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (language == Language.BENGALI)
                                    "এখনও কোনো ডিমের হিসাব এন্ট্রি করা হয়নি।"
                                else
                                    "No egg records logged yet.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (isAdmin) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        recordToEdit = null
                                        showAddDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VividAmber)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (language == Language.BENGALI) "প্রথম এন্ট্রি দিন" else "Add First Entry")
                                }
                            }
                        }
                    }
                }
            }

            // 6. Record items
            items(eggRecords) { record ->
                val isLocked = AntiCorruptionPolicy.isEggRecordLocked(record)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = record.date,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isLocked) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFEF2F2))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (language == Language.BENGALI) "🔒 ২৪ ঘণ্টা লক" else "🔒 24h Locked",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }
                            }

                            if (record.cost > 0.0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = VividAmber.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "৳ %.0f", record.cost),
                                        color = VividAmber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats pills for the day
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (record.purchasedCount > 0) {
                                EntryBadge(
                                    label = if (language == Language.BENGALI) "কেনা: +${record.purchasedCount} টি" else "Bought: +${record.purchasedCount}",
                                    color = VividBlue
                                )
                            }
                            if (record.consumedCount > 0) {
                                EntryBadge(
                                    label = if (language == Language.BENGALI) "খাওয়া: -${record.consumedCount} টি" else "Eaten: -${record.consumedCount}",
                                    color = VividAmber
                                )
                            }
                            if (record.damagedCount > 0) {
                                EntryBadge(
                                    label = if (language == Language.BENGALI) "নষ্ট: -${record.damagedCount} টি" else "Spoiled: -${record.damagedCount}",
                                    color = VividRed
                                )
                            }
                            if (record.purchasedCount == 0 && record.consumedCount == 0 && record.damagedCount == 0) {
                                EntryBadge(
                                    label = if (language == Language.BENGALI) "কোনো পরিবর্তন নেই" else "No change",
                                    color = Color.Gray
                                )
                            }
                        }

                        if (record.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "মন্তব্য: ${record.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        // Admin Actions (Edit / Delete)
                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isLocked) {
                                            lockedDialogMessage = if (language == Language.BENGALI)
                                                "এই ডিমের হিসাবটি (${record.date}) আপডেট করার পর ২৪ ঘণ্টা পার হয়ে গেছে। দুর্নীতি রোধের স্বার্থে এই রেকর্ডটি স্বয়ংক্রিয়ভাবে ব্লক করা হয়েছে। এটি আর এডিট করা যাবে না।"
                                            else
                                                "This egg record (${record.date}) was updated over 24 hours ago. For anti-corruption protection, it is auto-blocked and cannot be edited."
                                            showLockedDialog = true
                                        } else {
                                            recordToEdit = record
                                            showAddDialog = true
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Edit,
                                        contentDescription = "Edit Egg Log",
                                        tint = if (isLocked) Color.Gray else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        if (isLocked) {
                                            lockedDialogMessage = if (language == Language.BENGALI)
                                                "এই ডিমের হিসাবটি (${record.date}) আপডেট করার পর ২৪ ঘণ্টা পার হয়ে গেছে। দুর্নীতি রোধের স্বার্থে এই রেকর্ডটি স্বয়ংক্রিয়ভাবে ব্লক করা হয়েছে। এটি আর মোছা যাবে না।"
                                            else
                                                "This egg record (${record.date}) was updated over 24 hours ago. For anti-corruption protection, it is auto-blocked and cannot be deleted."
                                            showLockedDialog = true
                                        } else {
                                            recordToDelete = record
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Delete,
                                        contentDescription = "Delete Egg Log",
                                        tint = if (isLocked) Color.Gray else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog) {
        EggRecordDialog(
            record = recordToEdit,
            language = language,
            onDismiss = {
                showAddDialog = false
                recordToEdit = null
            },
            onConfirm = { updated ->
                onSaveEggRecord(updated)
                showAddDialog = false
                recordToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (recordToDelete != null) {
        val toDel = recordToDelete!!
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(if (language == Language.BENGALI) "ডিমের হিসাব মুছে ফেলার নিশ্চিতকরণ" else "Confirm Delete Egg Log")
            },
            text = {
                Text(
                    text = if (language == Language.BENGALI)
                        "আপনি কি নিশ্চিত যে [তারিখ: ${toDel.date}] এর ডিমের হিসাবটি মুছে ফেলতে চান?"
                    else
                        "Are you sure you want to delete the egg log for date ${toDel.date}?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEggRecord(toDel.id, "Date: ${toDel.date}")
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (language == Language.BENGALI) "মুছুন (Delete)" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text(if (language == Language.BENGALI) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Anti-Corruption 24h Lock Dialog
    if (showLockedDialog) {
        AlertDialog(
            onDismissRequest = { showLockedDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (language == Language.BENGALI) "🔒 ২৪ ঘণ্টা অ্যান্টি-করাপশন লক" else "🔒 24-Hour Anti-Corruption Lock",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = lockedDialogMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showLockedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (language == Language.BENGALI) "বুঝেছি" else "Understood")
                }
            }
        )
    }
}

@Composable
private fun StatPillCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun EntryBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EggRecordDialog(
    record: EggRecord?,
    language: Language,
    onDismiss: () -> Unit,
    onConfirm: (EggRecord) -> Unit
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var date by remember { mutableStateOf(record?.date ?: todayStr) }
    var purchasedStr by remember { mutableStateOf(if ((record?.purchasedCount ?: 0) > 0) record!!.purchasedCount.toString() else "") }
    var consumedStr by remember { mutableStateOf(if ((record?.consumedCount ?: 0) > 0) record!!.consumedCount.toString() else "") }
    var damagedStr by remember { mutableStateOf(if ((record?.damagedCount ?: 0) > 0) record!!.damagedCount.toString() else "") }
    var costStr by remember { mutableStateOf(if ((record?.cost ?: 0.0) > 0.0) String.format(Locale.US, "%.0f", record!!.cost) else "") }
    var notes by remember { mutableStateOf(record?.notes ?: "") }

    var showConfirmSaveAlert by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (record == null)
                    (if (language == Language.BENGALI) "নতুন ডিমের হিসাব যোগ করুন" else "Add Egg Record")
                else
                    (if (language == Language.BENGALI) "ডিমের হিসাব পরিবর্তন" else "Edit Egg Record"),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text(if (language == Language.BENGALI) "তারিখ (YYYY-MM-DD)" else "Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Purchased Count (কয়টি ডিম কেনা হলো)
                OutlinedTextField(
                    value = purchasedStr,
                    onValueChange = { purchasedStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (language == Language.BENGALI) "কয়টি ডিম কেনা হলো (Purchased)" else "Eggs Bought (Purchased)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Consumed Count (কয়টি ডিম খাওয়া হলো)
                OutlinedTextField(
                    value = consumedStr,
                    onValueChange = { consumedStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (language == Language.BENGALI) "কয়টি ডিম খাওয়া হলো (Consumed)" else "Eggs Eaten (Consumed)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Damaged/Spoiled Count (কয়টি ডিম নষ্ট বা ভেঙে গেল)
                OutlinedTextField(
                    value = damagedStr,
                    onValueChange = { damagedStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (language == Language.BENGALI) "কয়টি ডিম নষ্ট বা পচা (Damaged/Spoiled)" else "Eggs Damaged/Spoiled") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Cost in Taka (টাকা খরচ)
                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(if (language == Language.BENGALI) "ক্রয়ের টাকা খরচ (৳) [যদি কেনা হয়]" else "Purchase Cost (৳) [If bought]") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (language == Language.BENGALI) "মন্তব্য / খাবারের নাম" else "Notes / Meal Details") },
                    placeholder = { Text(if (language == Language.BENGALI) "উদা: দুপুরের খাবারে ডিম ভুনা" else "e.g. Boiled eggs for lunch") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    showConfirmSaveAlert = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = VividAmber)
            ) {
                Text(if (language == Language.BENGALI) "সংরক্ষণ করুন" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == Language.BENGALI) "বাতিল" else "Cancel")
            }
        }
    )

    // Interactive Notification / Confirmation alert before final save
    if (showConfirmSaveAlert) {
        val p = purchasedStr.toIntOrNull() ?: 0
        val c = consumedStr.toIntOrNull() ?: 0
        val d = damagedStr.toIntOrNull() ?: 0
        val cost = costStr.toDoubleOrNull() ?: 0.0

        AlertDialog(
            onDismissRequest = { showConfirmSaveAlert = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = VividAmber,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (language == Language.BENGALI) "তথ্য সংরক্ষণ নিশ্চিতকরণ" else "Confirm Save",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == Language.BENGALI)
                        "আপনি কি নিশ্চিত যে [তারিখ: $date] এর ডিমের হিসাব (কেনা: $p, খাওয়া: $c, নষ্ট: $d, খরচ: ৳$cost) সংরক্ষণ করতে চান?"
                    else
                        "Are you sure you want to save this egg log for $date (Bought: $p, Eaten: $c, Spoiled: $d, Cost: ৳$cost)?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = (record ?: EggRecord()).copy(
                            date = date,
                            purchasedCount = p,
                            consumedCount = c,
                            damagedCount = d,
                            cost = cost,
                            notes = notes
                        )
                        showConfirmSaveAlert = false
                        onConfirm(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VividGreen)
                ) {
                    Text(if (language == Language.BENGALI) "হ্যাঁ, সংরক্ষণ করুন" else "Yes, Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSaveAlert = false }) {
                    Text(if (language == Language.BENGALI) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
