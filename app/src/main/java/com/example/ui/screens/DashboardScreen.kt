package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CALCULATION_MODE_BAZAAR_RATE
import com.example.data.model.CALCULATION_MODE_FIXED_DAILY
import com.example.data.model.EggSummary
import com.example.data.model.MessBackup
import com.example.data.model.MessSummary
import com.example.ui.Language
import com.example.ui.components.StatCard
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PositiveGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DangerCrimson = Color(0xFFDC2626)

@Composable
fun DashboardScreen(
    summary: MessSummary,
    eggSummary: EggSummary? = null,
    backups: List<MessBackup> = emptyList(),
    currentMonth: String = "",
    messName: String = "",
    language: Language,
    isAdmin: Boolean,
    onNavigateToMealSheet: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToEggStock: (() -> Unit)? = null,
    onNavigateToJomaTk: (() -> Unit)? = null,
    onToggleCalculationMode: ((String) -> Unit)? = null,
    onResetData: ((password: String, label: String, onComplete: (Boolean, String) -> Unit) -> Unit)? = null,
    onRestoreBackup: ((backupId: String, onComplete: (Boolean, String) -> Unit) -> Unit)? = null,
    onCreateBackup: ((label: String, onComplete: (Boolean, String) -> Unit) -> Unit)? = null
) {
    var showUnauthorizedDialog by remember { mutableStateOf(false) }

    // Danger Zone 2-step Reset Dialog States
    var showResetStep1Dialog by remember { mutableStateOf(false) }
    var showResetStep2Dialog by remember { mutableStateOf(false) }
    var resetAdminPasswordInput by remember { mutableStateOf("") }
    var resetPasswordVisible by remember { mutableStateOf(false) }
    var resetBackupLabelInput by remember { mutableStateOf("") }
    var isResettingInProgress by remember { mutableStateOf(false) }

    // Backups & Restore Dialog States
    var showBackupsListDialog by remember { mutableStateOf(false) }
    var backupToRestore by remember { mutableStateOf<MessBackup?>(null) }
    var showCreateManualBackupDialog by remember { mutableStateOf(false) }
    var manualBackupLabelInput by remember { mutableStateOf("") }

    val isFixedDaily = summary.calculationMode == CALCULATION_MODE_FIXED_DAILY

    if (showUnauthorizedDialog) {
        AlertDialog(
            onDismissRequest = { showUnauthorizedDialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = {
                Text(
                    text = if (language == Language.BENGALI) "অননুমোদিত এক্সেস" else "Unauthorized Action",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == Language.BENGALI)
                        "শুধুমাত্র মেস ম্যানেজার/এডমিন হিসাব পদ্ধতি (টগল) পরিবর্তন করতে পারবেন।"
                    else
                        "Only the Mess Manager/Admin is authorized to switch calculation modes."
                )
            },
            confirmButton = {
                TextButton(onClick = { showUnauthorizedDialog = false }) {
                    Text(if (language == Language.BENGALI) "বুঝেছি" else "OK")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Calculation Mode Switcher Card (User Requested Dual Calculation System Toggle)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFixedDaily) Color(0xFFEFF6FF) else Color(0xFFF0FDF4)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = if (isFixedDaily) Color(0xFF3B82F6) else Color(0xFF10B981)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFixedDaily) Color(0xFF3B82F6).copy(alpha = 0.15f)
                                        else Color(0xFF10B981).copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = if (isFixedDaily) Color(0xFF2563EB) else Color(0xFF059669),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == Language.BENGALI) "টাকার হিসাব পদ্ধতি (Toggle)" else "Calculation Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isFixedDaily) {
                                        if (language == Language.BENGALI) "১. ফিক্সড দৈনিক প্যাকেজ (৪০৳/২৫৳)" else "1. Fixed Daily Package (40৳/25৳)"
                                    } else {
                                        if (language == Language.BENGALI) "২. বাজারের হিসাব অনুযায়ী মিল রেট" else "2. Bazaar Dynamic Rate"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isFixedDaily) Color(0xFF1D4ED8) else Color(0xFF047857)
                                )
                            }
                        }

                        // Toggle Switch for Admin
                        if (isAdmin) {
                            Switch(
                                checked = isFixedDaily,
                                onCheckedChange = { checked ->
                                    val newMode = if (checked) CALCULATION_MODE_FIXED_DAILY else CALCULATION_MODE_BAZAAR_RATE
                                    onToggleCalculationMode?.invoke(newMode)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF3B82F6),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFF10B981)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode rule explanation card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (isFixedDaily) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ElectricBolt,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == Language.BENGALI)
                                            "সক্রিয় নিয়ম: ফুল মিল (সকাল+দুপুর+রাত) বা দুপুর+রাত অন থাকলে দৈনিক ৪০৳ কর্তন। সকাল+দুপুর বা রাত+সকাল বা শুধু দুপুর/রাতে অন থাকলে ২৫৳ কর্তন। হিসাব মুহূর্তেই স্বয়ংক্রিয়ভাবে আপডেট হয়।"
                                        else
                                            "Active Rule: Full meal (B+L+D) or Lunch+Dinner = 40৳/day. Breakfast+Lunch, Dinner+Breakfast, or Single meal = 25৳/day. All instant balances auto-update.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF1E293B),
                                        lineHeight = 16.sp
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ElectricBolt,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == Language.BENGALI)
                                            "সক্রিয় নিয়ম: চলতি মাসের মোট বাজার খরচকে মোট খাওয়া মিল দিয়ে ভাগ করে প্রতি মিলের রেট নির্ধারণ করা হয়।"
                                        else
                                            "Active Rule: Dynamic meal rate calculated as Total Bazaar Cost ÷ Total Mess Meals consumed.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF1E293B),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // High-level banner adapting to current mode
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFixedDaily) Color(0xFF1E3A8A) else EmeraldPrimary
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isFixedDaily) {
                            if (language == Language.ENGLISH) "Fixed Daily Package System" else "ফিক্সড দৈনিক প্যাকেজ সিস্টেম"
                        } else {
                            if (language == Language.ENGLISH) "Current Meal Rate" else "বাজার অনুযায়ী বর্তমান মিল রেট"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (isFixedDaily) {
                            Text(
                                text = "৳ ৪০ / ৳ ২৫",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (language == Language.ENGLISH) " (Full / Partial)" else " (ফুল / আংশিক)",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(bottom = 4.dp, start = 6.dp)
                            )
                        } else {
                            Text(
                                text = String.format(Locale.US, "৳ %.2f", summary.mealRate),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (language == Language.ENGLISH) " /meal" else " /প্রতি মিল",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isFixedDaily) {
                            if (language == Language.ENGLISH)
                                "Today's Total Mess Meal Cost: ৳${String.format(Locale.US, "%.0f", summary.todayMealCost)} • Monthly Meals Billed: ৳${String.format(Locale.US, "%.0f", summary.totalMealCost)}"
                            else
                                "⚡ আজকের মেসের মোট মিল খরচ: ৳${String.format(Locale.US, "%.0f", summary.todayMealCost)} • চলতি মাসের মোট: ৳${String.format(Locale.US, "%.0f", summary.totalMealCost)}"
                        } else {
                            if (language == Language.ENGLISH)
                                "Total Bazaar: ৳${String.format(Locale.US, "%.1f", summary.totalBazaarCost)} ÷ Total Meals: ${String.format(Locale.US, "%.1f", summary.totalMeals)}"
                            else
                                "মোট বাজার: ৳${String.format(Locale.US, "%.1f", summary.totalBazaarCost)} ÷ মোট মিল: ${String.format(Locale.US, "%.1f", summary.totalMeals)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }
            }
        }

        // Metrics Grid (2x2)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = if (language == Language.ENGLISH) "Total Meals" else "মোট মিল",
                    value = String.format(Locale.US, "%.1f", summary.totalMeals),
                    subtitle = if (language == Language.ENGLISH) "This month" else "চলতি মাসে",
                    icon = Icons.Default.Restaurant,
                    iconTint = AmberSecondary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = if (language == Language.ENGLISH) "Bazaar Cost" else "বাজার খরচ",
                    value = String.format(Locale.US, "৳%.0f", summary.totalBazaarCost),
                    subtitle = if (language == Language.ENGLISH) "Shared pool" else "শেয়ার্ড বাজার",
                    icon = Icons.Default.ShoppingCart,
                    iconTint = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = if (language == Language.ENGLISH) "Rice In Stock" else "অবশিষ্ট চাউল",
                    value = String.format(Locale.US, "%.1f pots", summary.remainingMessPots),
                    subtitle = if (language == Language.ENGLISH) "Pot balance" else "মোট পটের হিসাব",
                    icon = Icons.Default.Savings,
                    iconTint = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = if (language == Language.ENGLISH) "Utilities" else "ইউটিলিটি বিল",
                    value = String.format(Locale.US, "৳%.0f", summary.totalUtilityCost),
                    subtitle = if (language == Language.ENGLISH) "৳${String.format(Locale.US, "%.0f", summary.perMemberUtility)}/border" else "৳${String.format(Locale.US, "%.0f", summary.perMemberUtility)}/জন",
                    icon = Icons.Default.Receipt,
                    iconTint = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Joma Tk Summary Card
        item {
            val totalCash = summary.memberSummaries.sumOf { it.cashDeposited }
            Card(
                onClick = { onNavigateToJomaTk?.invoke() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == Language.ENGLISH) "Mess Deposit Fund (Joma Tk)" else "মেস ফান্ডের মোট জমা টাকা (Joma Tk)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (language == Language.ENGLISH)
                                    "Tap to view member-wise deposits & balances"
                                else
                                    "সদস্যদের জমা ও ব্যালেন্স দেখতে ট্যাপ করুন",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Text(
                        text = String.format(Locale.US, "৳ %.0f", totalCash),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary
                    )
                }
            }
        }

        // Egg Inventory Summary Item
        if (eggSummary != null) {
            item {
                Card(
                    onClick = { onNavigateToEggStock?.invoke() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AmberSecondary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = AmberSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == Language.ENGLISH) "Egg Inventory Stock" else "ডিমের বর্তমান মজুদ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (language == Language.ENGLISH)
                                        "Bought: ${eggSummary.totalPurchased} • Eaten: ${eggSummary.totalConsumed} • Damaged: ${eggSummary.totalDamaged}"
                                    else
                                        "কেনা: ${eggSummary.totalPurchased} • খাওয়া: ${eggSummary.totalConsumed} • নষ্ট: ${eggSummary.totalDamaged}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Text(
                            text = "${eggSummary.currentStock} ${if (language == Language.ENGLISH) "Eggs" else "টি"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (eggSummary.currentStock <= 5) MaterialTheme.colorScheme.error else AmberSecondary
                        )
                    }
                }
            }
        }

        // Quick Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigateToMealSheet,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = if (language == Language.ENGLISH) "Record Today's Meal" else "আজকের মিল তুলুন",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Button(
                    onClick = onNavigateToReports,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(
                        text = if (language == Language.ENGLISH) "Monthly Statement" else "মাসের হিসাবপত্র",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Section Title: Member Live Standings
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(
                    text = if (language == Language.ENGLISH) "Member Financial Balances" else "সদস্যদের জমা ও খরচ স্ট্যাটাস",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Member Summary List
        items(summary.memberSummaries) { memSummary ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = memSummary.member.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${if (language == Language.ENGLISH) "Meals:" else "মিল:"} ${String.format(Locale.US, "%.1f", memSummary.totalMeals)} • ${if (language == Language.ENGLISH) "Pots Left:" else "অবশিষ্ট পট:"} ${String.format(Locale.US, "%.1f", memSummary.remainingPots)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    // Net status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (memSummary.netBalance >= 0) PositiveGreen.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (memSummary.netBalance >= 0)
                                "+৳${String.format(Locale.US, "%.0f", memSummary.netBalance)} (${if (language == Language.ENGLISH) "Refund" else "ফেরত"})"
                            else
                                "-৳${String.format(Locale.US, "%.0f", -memSummary.netBalance)} (${if (language == Language.ENGLISH) "Due" else "বকেয়া"})",
                            color = if (memSummary.netBalance >= 0) PositiveGreen else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Manager Danger Zone: Secure Reset & Cloud Backups (Admin Only)
        if (isAdmin) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, DangerCrimson.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DangerCrimson.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = DangerCrimson,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == Language.ENGLISH) "Manager Danger Zone & Backups" else "ম্যানেজার ডাটা নিয়ন্ত্রণ ও অটো-ব্যাকআপ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DangerCrimson
                                )
                                Text(
                                    text = if (language == Language.ENGLISH)
                                        "Safe data reset with guaranteed automated cloud snapshot backup"
                                    else
                                        "নিরাপদ ডাটা রিসেট এবং ক্লাউড ব্যাকআপ সংরক্ষণ ও রিস্টোর ব্যবস্থা",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Button 1: View Past Backups & Restore
                        OutlinedButton(
                            onClick = { showBackupsListDialog = true },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == Language.ENGLISH)
                                    "Cloud Backups History & Restore (${backups.size})"
                                else
                                    "📂 ক্লাউড ব্যাকআপ তালিকা ও রিস্টোর (${backups.size}টি)",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Button 2: Secure Reset with 2-step confirmation and admin password
                        Button(
                            onClick = {
                                if (!isAdmin) {
                                    showUnauthorizedDialog = true
                                } else {
                                    showResetStep1Dialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DangerCrimson,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == Language.ENGLISH)
                                    "Reset All Mess Data (with Auto-Backup)"
                                else
                                    "🚨 মেস ডাটা সম্পূর্ণ রিসেট (অটো-ব্যাকআপ সহ)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // --- Danger Zone Step 1: Warning Dialog ---
    if (showResetStep1Dialog) {
        AlertDialog(
            onDismissRequest = { showResetStep1Dialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = DangerCrimson) },
            title = {
                Text(
                    text = if (language == Language.ENGLISH) "Confirm Data Reset (Step 1/2)" else "মেস ডাটা রিসেট নিশ্চিতকরণ (ধাপ ১/২)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (language == Language.ENGLISH)
                            "Warning: This will clear all daily meals, bazaar expenses, utilities, and egg logs. Member deposit balances will be set to 0."
                        else
                            "সতর্কতা: এই অপারেশনের মাধ্যমে বর্তমান মেসের সকল মিল শিট, বাজার খরচ, ইউটিলিটি বিল এবং ডিমের রেকর্ড রিসেট হবে। সদস্যদের চাউল ও টাকা জমা শূন্য (০) করা হবে।",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (language == Language.ENGLISH)
                            "✓ A full cloud snapshot of your current data will be automatically saved before deleting, allowing you to restore it at any time."
                        else
                            "✓ সম্পূর্ণ ডাটার একটি ক্লাউড ব্যাকআপ স্বয়ংক্রিয়ভাবে সংরক্ষিত হবে, যা দিয়ে যে কোনো সময় আবার আগের অবস্থায় রিস্টোর করতে পারবেন।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PositiveGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetStep1Dialog = false
                        resetAdminPasswordInput = ""
                        resetBackupLabelInput = "Auto-Backup before Reset (${SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date())})"
                        showResetStep2Dialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson)
                ) {
                    Text(if (language == Language.ENGLISH) "Proceed to Step 2 (Password)" else "পরবর্তী ধাপ (পাসওয়ার্ড দিন)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStep1Dialog = false }) {
                    Text(if (language == Language.ENGLISH) "Cancel" else "বাতিল")
                }
            }
        )
    }

    // --- Danger Zone Step 2: Password Verification & Backup Label ---
    if (showResetStep2Dialog) {
        AlertDialog(
            onDismissRequest = { if (!isResettingInProgress) showResetStep2Dialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = DangerCrimson) },
            title = {
                Text(
                    text = if (language == Language.ENGLISH) "Admin Password Required (Step 2/2)" else "এডমিন পাসওয়ার্ড যাচাই (ধাপ ২/২)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (language == Language.ENGLISH)
                            "Enter the admin password to confirm reset and auto-backup creation:"
                        else
                            "ভুলবশত ক্লিক রোধ করার জন্য মেস এডমিন পাসওয়ার্ড প্রদান করুন:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = resetAdminPasswordInput,
                        onValueChange = { resetAdminPasswordInput = it },
                        label = { Text(if (language == Language.ENGLISH) "Admin Password" else "এডমিন পাসওয়ার্ড") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (resetPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { resetPasswordVisible = !resetPasswordVisible }) {
                                Icon(
                                    imageVector = if (resetPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = resetBackupLabelInput,
                        onValueChange = { resetBackupLabelInput = it },
                        label = { Text(if (language == Language.ENGLISH) "Backup Snapshot Name" else "ব্যাকআপের নাম (ঐচ্ছিক)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isResettingInProgress = true
                        onResetData?.invoke(resetAdminPasswordInput, resetBackupLabelInput) { success, _ ->
                            isResettingInProgress = false
                            if (success) {
                                showResetStep2Dialog = false
                            }
                        }
                    },
                    enabled = resetAdminPasswordInput.isNotBlank() && !isResettingInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson)
                ) {
                    Text(if (language == Language.ENGLISH) "Confirm Reset & Backup" else "নিশ্চিত রিসেট ও ব্যাকআপ")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetStep2Dialog = false },
                    enabled = !isResettingInProgress
                ) {
                    Text(if (language == Language.ENGLISH) "Cancel" else "বাতিল")
                }
            }
        )
    }

    // --- Backups List & Restore Dialog ---
    if (showBackupsListDialog) {
        val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }
        AlertDialog(
            onDismissRequest = { showBackupsListDialog = false },
            icon = { Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (language == Language.ENGLISH) "Cloud Backups" else "ক্লাউড ব্যাকআপ ও রিস্টোর",
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        manualBackupLabelInput = "Manual Backup (${SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date())})"
                        showCreateManualBackupDialog = true
                    }) {
                        Icon(Icons.Default.Backup, contentDescription = "New Backup", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            text = {
                if (backups.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (language == Language.ENGLISH) "No saved backups found." else "কোনো ব্যাকআপ পাওয়া যায়নি।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = {
                            manualBackupLabelInput = "Manual Backup (${SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date())})"
                            showCreateManualBackupDialog = true
                        }) {
                            Text(if (language == Language.ENGLISH) "Create New Backup Now" else "নতুন ব্যাকআপ তৈরি করুন")
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(backups) { backup ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = backup.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = { backupToRestore = backup },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (language == Language.ENGLISH) "Restore" else "রিস্টোর",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val dateDisplay = backup.timestamp?.toDate()?.let { dateFormat.format(it) } ?: backup.dateString
                                    Text(
                                        text = "${if (language == Language.ENGLISH) "Date:" else "তারিখ:"} $dateDisplay",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${if (language == Language.ENGLISH) "Members:" else "সদস্য:"} ${backup.membersCount} • ${if (language == Language.ENGLISH) "Meals:" else "মিল:"} ${backup.mealsCount} • ${if (language == Language.ENGLISH) "Bazaar:" else "বাজার:"} ${backup.expensesCount} • ${if (language == Language.ENGLISH) "Utilities:" else "ইউটিলিটি:"} ${backup.utilitiesCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupsListDialog = false }) {
                    Text(if (language == Language.ENGLISH) "Close" else "বন্ধ করুন")
                }
            }
        )
    }

    // --- Confirm Restore Backup Dialog ---
    if (backupToRestore != null) {
        val target = backupToRestore!!
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    text = if (language == Language.ENGLISH) "Restore Data Backup?" else "ব্যাকআপ রিস্টোর করবেন?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == Language.ENGLISH)
                        "Are you sure you want to restore '${target.label}'? This will replace current mess data with the records from this backup snapshot."
                    else
                        "আপনি কি নিশ্চিতভাবে '${target.label}' ব্যাকআপটি বর্তমান মেস সিস্টেমে রিস্টোর করতে চান? এটি মেসের সকল তথ্যকে এই ব্যাকআপের অবস্থায় ফিরিয়ে আনবে।"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val backupId = target.id
                        backupToRestore = null
                        showBackupsListDialog = false
                        onRestoreBackup?.invoke(backupId) { _, _ -> }
                    }
                ) {
                    Text(if (language == Language.ENGLISH) "Confirm Restore" else "হ্যাঁ, রিস্টোর করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null }) {
                    Text(if (language == Language.ENGLISH) "Cancel" else "বাতিল")
                }
            }
        )
    }

    // --- Manual Backup Create Dialog ---
    if (showCreateManualBackupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateManualBackupDialog = false },
            icon = { Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    text = if (language == Language.ENGLISH) "Create Manual Backup" else "নতুন ক্লাউড ব্যাকআপ তৈরি করুন",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (language == Language.ENGLISH)
                            "Save a complete snapshot of current members, meals, expenses, and utilities into the cloud database."
                        else
                            "বর্তমান মেসের সকল সদস্য, মিল, বাজার খরচ এবং ইউটিলিটির একটি নিরাপদ ক্লাউড স্ন্যাপশট ব্যাকআপ রাখুন।"
                    )
                    OutlinedTextField(
                        value = manualBackupLabelInput,
                        onValueChange = { manualBackupLabelInput = it },
                        label = { Text(if (language == Language.ENGLISH) "Backup Label" else "ব্যাকআপের নাম") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val label = manualBackupLabelInput
                        showCreateManualBackupDialog = false
                        onCreateBackup?.invoke(label) { _, _ -> }
                    }
                ) {
                    Text(if (language == Language.ENGLISH) "Save Backup" else "ব্যাকআপ সেভ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateManualBackupDialog = false }) {
                    Text(if (language == Language.ENGLISH) "Cancel" else "বাতিল")
                }
            }
        )
    }
}
