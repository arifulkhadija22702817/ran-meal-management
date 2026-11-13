package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import com.example.data.model.EggRecord
import com.example.data.model.EggSummary
import com.example.data.model.Expense
import com.example.data.model.MessSummary
import com.example.util.AntiCorruptionPolicy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyMeal
import com.example.data.model.Member
import com.example.ui.Language
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.EmeraldPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Bright, vibrant palette for high-visibility active/on states
private val VividGreenActive = Color(0xFF10B981) // Radiant emerald green
private val VividGreenBorder = Color(0xFF059669)
private val VividAmberActive = Color(0xFFF59E0B) // Radiant warm gold
private val VividAmberBorder = Color(0xFFD97706)
private val VividRedOff = Color(0xFFEF4444)     // Distinct red for closed/off state
private val VividRedBorder = Color(0xFFDC2626)
private val InactiveButtonBg = Color(0xFFF1F5F9)
private val InactiveButtonBorder = Color(0xFFCBD5E1)
private val InactiveTextColor = Color(0xFF64748B)

@Composable
fun MealSheetScreen(
    selectedDate: String,
    available45Dates: List<String>,
    onDateSelected: (String) -> Unit,
    members: List<Member>,
    dailyMeals: List<DailyMeal>,
    isAdmin: Boolean,
    language: Language,
    messName: String = "Meal Management",
    expenses: List<Expense> = emptyList(),
    eggRecords: List<EggRecord> = emptyList(),
    eggSummary: EggSummary? = null,
    calculationMode: String = com.example.data.model.CALCULATION_MODE_FIXED_DAILY,
    fullMealRate: Double = 40.0,
    partialMealRate: Double = 25.0,
    summary: MessSummary? = null,
    onUpdateMeal: (date: String, memberId: String, mealType: String, count: Double) -> Unit,
    onToggleFullDayMeal: (date: String, memberId: String, turnOn: Boolean) -> Unit,
    onSaveMember: (Member) -> Unit,
    onDeleteMember: (memberId: String, memberName: String) -> Unit,
    onCopyPreviousDayMeals: (date: String) -> Unit = {}
) {
    var showMemberDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<Member?>(null) }
    var memberToDelete by remember { mutableStateOf<Member?>(null) }
    var show45DatesPicker by remember { mutableStateOf(false) }

    // Unauthorized alert when a non-admin clicks on any meal or attendance button
    var showUnauthorizedDialog by remember { mutableStateOf(false) }
    var showLockedDialog by remember { mutableStateOf(false) }

    val currentDailyMeal = dailyMeals.firstOrNull { it.date == selectedDate }
    val isMealSheetLocked = AntiCorruptionPolicy.isMealLocked(currentDailyMeal, selectedDate)

    // Aggregate totals for selected date
    val totalB = currentDailyMeal?.breakfastCount ?: 0.0
    val totalL = currentDailyMeal?.lunchCount ?: 0.0
    val totalD = currentDailyMeal?.dinnerCount ?: 0.0
    val totalDayMeals = totalB + totalL + totalD

    Scaffold(
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = {
                        editingMember = null
                        showMemberDialog = true
                    },
                    containerColor = VividGreenActive,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_member_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Member")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("meal_sheet_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // 1. Sticky Save Status Header Card (Visible in both User and Manager mode)
            item {
                val updateTs = currentDailyMeal?.updatedAt ?: currentDailyMeal?.createdAt
                val formattedTime = if (updateTs != null) {
                    val sdf = SimpleDateFormat("hh:mm a, dd MMM", Locale.US)
                    sdf.format(updateTs.toDate())
                } else {
                    if (language == Language.BENGALI) "আজকের কোনো এন্ট্রি আপডেট নেই" else "No entries updated today"
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Cloud Save Status",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (language == Language.BENGALI) "💾 রিয়েলটাইম ক্লাউড সেভ" else "💾 Realtime Cloud Sync",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF15803D)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF16A34A)
                                    ) {
                                        Text(
                                            text = "LIVE",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (language == Language.BENGALI)
                                        "সর্বশেষ সেভ: $formattedTime"
                                    else
                                        "Last Saved: $formattedTime",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }
                }
            }

            // 2. 1-Click Copy Previous Day Meals Card (For Manager)
            if (isAdmin && !isMealSheetLocked) {
                item {
                    val prevDateStr = shiftDate(selectedDate, -1)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (language == Language.BENGALI)
                                            "পূর্বের দিনের মিল আজকের তারিখে আনতে চান?"
                                        else
                                            "Copy previous day's meals?",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = if (language == Language.BENGALI)
                                            "১ ক্লিকেই পূর্বের দিনের ($prevDateStr) সব মিল সিলেক্ট হবে"
                                        else
                                            "1-click copies all meals from $prevDateStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onCopyPreviousDayMeals(selectedDate) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text(
                                    text = if (language == Language.BENGALI) "১ ক্লিকে কপি" else "1-Click Copy",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Date Picker Header with Next/Prev navigation and 45-day Selector Button
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { onDateSelected(shiftDate(selectedDate, -1)) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = "Previous Day",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (language == Language.BENGALI) "পূর্বের দিন" else "Prev",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatReadableDate(selectedDate),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = selectedDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            Button(
                                onClick = { onDateSelected(shiftDate(selectedDate, 1)) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(
                                    text = if (language == Language.BENGALI) "পরের দিন" else "Next",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Next Day",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick date shortcut chips & 45-day picker trigger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            val yesterdayStr = shiftDate(todayStr, -1)
                            val tomorrowStr = shiftDate(todayStr, 1)

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = selectedDate == yesterdayStr,
                                    onClick = { onDateSelected(yesterdayStr) },
                                    label = { Text(if (language == Language.BENGALI) "গতকাল" else "Yesterday", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = selectedDate == todayStr,
                                    onClick = { onDateSelected(todayStr) },
                                    label = { Text(if (language == Language.BENGALI) "আজকে" else "Today", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = selectedDate == tomorrowStr,
                                    onClick = { onDateSelected(tomorrowStr) },
                                    label = { Text(if (language == Language.BENGALI) "আগামীকাল" else "Tomorrow", fontSize = 11.sp) }
                                )
                            }

                            Button(
                                onClick = { show45DatesPicker = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VividGreenActive.copy(alpha = 0.15f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = VividGreenActive,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == Language.BENGALI) "৪৫ দিন" else "45 Days",
                                    color = VividGreenActive,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 24-Hour Anti-Corruption Security Lock Banner
            item {
                if (isMealSheetLocked) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == Language.BENGALI)
                                        "🔒 ২৪ ঘণ্টা অতিক্রান্ত: এই মিল শিটটি স্থায়ীভাবে লক করা হয়েছে"
                                    else
                                        "🔒 24 Hours Passed: This meal sheet is permanently locked",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF991B1B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (language == Language.BENGALI)
                                        "দুর্নীতি রোধ ও নিরপেক্ষ হিসাবের স্বার্থে ২৪ ঘণ্টা পর ম্যানেজার সহ কারো আর মিল এডিট করার অনুমতি নেই।"
                                    else
                                        "For anti-corruption security, no edits are permitted after 24h.",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = VividGreenBorder, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = AntiCorruptionPolicy.getLockStatusMessage(
                                    currentDailyMeal?.updatedAt ?: currentDailyMeal?.createdAt,
                                    selectedDate,
                                    language == Language.BENGALI
                                ),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }
            }

            // Daily Meal Total Stats Card (Vibrant highlights)
            item {
                val isFixedDaily = calculationMode == com.example.data.model.CALCULATION_MODE_FIXED_DAILY
                val todayTotalMealExpense = members.sumOf { member ->
                    val mMeals = currentDailyMeal?.memberMeals?.get(member.id) ?: emptyMap()
                    val b = mMeals["b"] ?: 0.0
                    val l = mMeals["l"] ?: 0.0
                    val d = mMeals["d"] ?: 0.0
                    com.example.util.MessCalculator.calculateDailyMemberCost(b, l, d, fullMealRate, partialMealRate)
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            MealStatItem(
                                label = if (language == Language.BENGALI) "সকাল মোট" else "Breakfast",
                                count = totalB
                            )
                            MealStatItem(
                                label = if (language == Language.BENGALI) "দুপুর মোট" else "Lunch",
                                count = totalL
                            )
                            MealStatItem(
                                label = if (language == Language.BENGALI) "রাত মোট" else "Dinner",
                                count = totalD
                            )
                            MealStatItem(
                                label = if (language == Language.BENGALI) "সর্বমোট মিল" else "Total Meals",
                                count = totalDayMeals,
                                isHighlighted = true
                            )
                        }

                        if (isFixedDaily) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Restaurant,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (language == Language.BENGALI)
                                                "⚡ আজকের মোট মিল খরচ (প্যাকেজ রেট):"
                                            else
                                                "⚡ Today's Total Meal Cost:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = Color(0xFF1E3A8A)
                                        )
                                    }
                                    Text(
                                        text = "৳ ${todayTotalMealExpense.toInt()}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1D4ED8)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Member list with meal toggles
            items(members) { member ->
                val memberMeals = currentDailyMeal?.memberMeals?.get(member.id) ?: emptyMap()
                val bVal = memberMeals["b"] ?: 0.0
                val lVal = memberMeals["l"] ?: 0.0
                val dVal = memberMeals["d"] ?: 0.0
                val memberDayTotal = bVal + lVal + dVal
                val isMemberPresent = memberDayTotal > 0.0

                // Financial & Rice Depletion Check:
                // User requirement: "jader tk othoba chaul ses hoye jabe, tar nam red bg hobe jate manager bujhte pare j, ei member er tk ba chaul aro lagbe"
                val memSummary = summary?.memberSummaries?.firstOrNull { it.member.id == member.id }
                val isOutOfMoney = (memSummary != null && memSummary.netBalance < 0.0) || member.cashDeposits <= 0.0
                val isOutOfRice = (memSummary != null && memSummary.remainingPots <= 0.0) || member.potDeposits <= 0.0
                val isDepleted = isOutOfMoney || isOutOfRice

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDepleted) Color(0xFFFFF5F5) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isDepleted) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.8f)) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // 1. TOP HEADER: Member info + Prominent PRESENT / ABSENT Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDepleted) Color(0xFFFEE2E2)
                                            else if (isMemberPresent) VividGreenActive.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isDepleted) Color(0xFFDC2626)
                                        else if (isMemberPresent) VividGreenActive
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    // Member Name with distinct RED background when money or rice is depleted
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isDepleted) Color(0xFFFEE2E2) else Color.Transparent,
                                        border = if (isDepleted) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)) else null
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(
                                                horizontal = if (isDepleted) 6.dp else 0.dp,
                                                vertical = if (isDepleted) 2.dp else 0.dp
                                            )
                                        ) {
                                            Text(
                                                text = member.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDepleted) Color(0xFFB91C1C) else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isDepleted) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFDC2626)
                                                ) {
                                                    Text(
                                                        text = when {
                                                            isOutOfMoney && isOutOfRice -> if (language == Language.BENGALI) "⚠️ টাকা ও চাউল শেষ" else "⚠️ Tk & Rice Out"
                                                            isOutOfMoney -> if (language == Language.BENGALI) "⚠️ টাকা শেষ" else "⚠️ Tk Out"
                                                            else -> if (language == Language.BENGALI) "⚠️ চাউল শেষ" else "⚠️ Rice Out"
                                                        },
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (member.roomNumber.isNotBlank()) "Room: ${member.roomNumber}" else member.phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        if (calculationMode == com.example.data.model.CALCULATION_MODE_FIXED_DAILY) {
                                            val memberDayCost = com.example.util.MessCalculator.calculateDailyMemberCost(
                                                bVal, lVal, dVal, fullMealRate, partialMealRate
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (memberDayCost > 0) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)
                                            ) {
                                                Text(
                                                    text = "খরচ: ৳${memberDayCost.toInt()}",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (memberDayCost > 0) Color(0xFF1D4ED8) else Color(0xFF64748B),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Manager prompt showing what is required from this member
                                    if (isDepleted) {
                                        Text(
                                            text = when {
                                                isOutOfMoney && isOutOfRice -> if (language == Language.BENGALI) "👉 এই সদস্যের টাকা ও চাউল জমা লাগবে" else "👉 Needs cash and rice deposit"
                                                isOutOfMoney -> if (language == Language.BENGALI) "👉 এই সদস্যের টাকা জমা লাগবে" else "👉 Needs cash deposit"
                                                else -> if (language == Language.BENGALI) "👉 এই সদস্যের চাউল জমা লাগবে" else "👉 Needs rice deposit"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }
                            }

                            // Attendance Toggle Button (Click to toggle full-day meal presence)
                            Surface(
                                onClick = {
                                    if (!isAdmin) {
                                        showUnauthorizedDialog = true
                                    } else if (isMealSheetLocked) {
                                        showLockedDialog = true
                                    } else {
                                        onToggleFullDayMeal(selectedDate, member.id, !isMemberPresent)
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isMemberPresent) VividGreenActive.copy(alpha = 0.18f) else InactiveButtonBg,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.2.dp,
                                    if (isMemberPresent) VividGreenBorder else InactiveButtonBorder
                                ),
                                modifier = Modifier.testTag("attendance_badge_${member.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMemberPresent) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (isMemberPresent) VividGreenBorder else InactiveTextColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isMemberPresent) {
                                            if (language == Language.BENGALI) "মিল চালু (${String.format(Locale.US, "%.1f", memberDayTotal)})"
                                            else "Present (${String.format(Locale.US, "%.1f", memberDayTotal)})"
                                        } else {
                                            if (language == Language.BENGALI) "মিল বন্ধ (০)"
                                            else "Absent (0)"
                                        },
                                        color = if (isMemberPresent) VividGreenBorder else InactiveTextColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }

                            // Admin Edit / Delete (Manager Only)
                            if (isAdmin) {
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingMember = member
                                            showMemberDialog = true
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Member",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { memberToDelete = member },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Member",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. MEAL TOGGLES: Strictly Admin / Manager Only (Disabled for regular users)
                        // Breakfast: 0 / 0.5 (Half Pot) / 1.0 (Full Pot)
                        VibrantMealSelectorRow(
                            mealName = if (language == Language.BENGALI) "🌅 সকাল (Breakfast)" else "🌅 Breakfast",
                            currentValue = bVal,
                            language = language,
                            allowHalfPot = true,
                            isAdmin = isAdmin,
                            onOptionClicked = { count ->
                                if (!isAdmin) {
                                    showUnauthorizedDialog = true
                                } else if (isMealSheetLocked) {
                                    showLockedDialog = true
                                } else {
                                    onUpdateMeal(selectedDate, member.id, "b", count)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Lunch: strictly ONLY 0 (বন্ধ) or 1 (ফুল পট) - NO half pot!
                        VibrantMealSelectorRow(
                            mealName = if (language == Language.BENGALI) "☀️ দুপুর (Lunch - শুধু ১ পট বা বন্ধ)" else "☀️ Lunch (1 Pot or OFF)",
                            currentValue = lVal,
                            language = language,
                            allowHalfPot = false,
                            isAdmin = isAdmin,
                            onOptionClicked = { count ->
                                if (!isAdmin) {
                                    showUnauthorizedDialog = true
                                } else if (isMealSheetLocked) {
                                    showLockedDialog = true
                                } else {
                                    onUpdateMeal(selectedDate, member.id, "l", count)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dinner: strictly ONLY 0 (বন্ধ) or 1 (ফুল পট) - NO half pot!
                        VibrantMealSelectorRow(
                            mealName = if (language == Language.BENGALI) "🌙 রাত (Dinner - শুধু ১ পট বা বন্ধ)" else "🌙 Dinner (1 Pot or OFF)",
                            currentValue = dVal,
                            language = language,
                            allowHalfPot = false,
                            isAdmin = isAdmin,
                            onOptionClicked = { count ->
                                if (!isAdmin) {
                                    showUnauthorizedDialog = true
                                } else if (isMealSheetLocked) {
                                    showLockedDialog = true
                                } else {
                                    onUpdateMeal(selectedDate, member.id, "d", count)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 24-Hour Anti-Corruption Security Lock Alert Dialog
    if (showLockedDialog) {
        AlertDialog(
            onDismissRequest = { showLockedDialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(36.dp)) },
            title = {
                Text(
                    text = if (language == Language.BENGALI) "🔒 দুর্নীতি রোধ: ২৪ ঘণ্টা পর অটো-ব্লক" else "🔒 Auto-Blocked: 24h Passed",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == Language.BENGALI)
                        "অ্যাডমিন ডাটা আপডেট করার পর ২৪ ঘণ্টা অতিক্রান্ত হয়ে গেছে। দুর্নীতি রোধ ও নিরপেক্ষ হিসাবের স্বার্থে এই দিনের ডাটা স্বয়ংক্রিয়ভাবে ব্লক করা হয়েছে। এটি আর পরিবর্তন করা যাবে না।"
                    else
                        "24 hours have passed since this day's data was updated. For anti-corruption transparency, it is auto-blocked and cannot be modified.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showLockedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(if (language == Language.BENGALI) "বুঝেছি (OK)" else "OK")
                }
            }
        )
    }

    // Unauthorized Action Alert for regular members / guests
    if (showUnauthorizedDialog) {
        AlertDialog(
            onDismissRequest = { showUnauthorizedDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (language == Language.BENGALI) "শুধুমাত্র অ্যাডমিনের অনুমতি আছে" else "Admin Permission Required",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == Language.BENGALI)
                        "আপনি বর্তমানে মেম্বার/ইউজার মোডে আছেন। মেসের মিল চালু বা বন্ধ করা কিংবা সকাল, দুপুর বা রাতের মিল পরিবর্তন করার ক্ষমতা শুধুমাত্র মেস ম্যানেজারের (অ্যাডমিন) রয়েছে। সাধারণ সদস্যরা শুধুমাত্র তথ্য দেখতে পারবেন।"
                    else
                        "You are currently viewing as a member. Only the Mess Manager (Admin) has permission to turn on/off meals or change meal counts. Members have read-only access.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showUnauthorizedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (language == Language.BENGALI) "বুঝেছি (OK)" else "OK")
                }
            }
        )
    }

    // 45 Days Date Selection Dialog
    if (show45DatesPicker) {
        AlertDialog(
            onDismissRequest = { show45DatesPicker = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = VividGreenActive)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (language == Language.BENGALI) "৪৫ দিনের তারিখ নির্বাচন" else "Select from 45 Days")
                }
            },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(available45Dates) { dateStr ->
                        val isCurrent = (dateStr == selectedDate)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) VividGreenActive.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    onDateSelected(dateStr)
                                    show45DatesPicker = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${formatReadableDate(dateStr)} ($dateStr)",
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) VividGreenActive else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                            if (isCurrent) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = VividGreenActive, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { show45DatesPicker = false }) {
                    Text(if (language == Language.BENGALI) "বন্ধ করুন" else "Close")
                }
            }
        )
    }

    // Add / Edit Member Dialog with confirmation
    if (showMemberDialog) {
        MemberEditDialog(
            member = editingMember,
            language = language,
            onDismiss = { showMemberDialog = false },
            onConfirm = { m ->
                onSaveMember(m)
                showMemberDialog = false
            }
        )
    }

    // Confirm Delete Dialog
    if (memberToDelete != null) {
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text(if (language == Language.BENGALI) "সদস্য মুছে ফেলুন" else "Delete Member") },
            text = {
                Text(
                    if (language == Language.BENGALI)
                        "আপনি কি নিশ্চিত যে '${memberToDelete!!.name}' কে মেস থেকে মুছে ফেলতে চান?"
                    else
                        "Are you sure you want to remove '${memberToDelete!!.name}' from the mess?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMember(memberToDelete!!.id, memberToDelete!!.name)
                        memberToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (language == Language.BENGALI) "মুছুন (Delete)" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) {
                    Text(if (language == Language.BENGALI) "বাতিল (Cancel)" else "Cancel")
                }
            }
        )
    }
}

/**
 * Prominent Present / Absent toggle button
 * Glowing Vivid Green when active/ON, Distinct Slate when inactive/OFF.
 */
@Composable
private fun PresentMealToggleButton(
    isPresent: Boolean,
    totalCount: Double,
    language: Language,
    isAdmin: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isPresent) VividGreenActive else InactiveButtonBg,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isPresent) VividGreenBorder else InactiveButtonBorder
        ),
        shadowElevation = if (isPresent) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isPresent) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isPresent) Color.White else InactiveTextColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isPresent) {
                    if (language == Language.BENGALI) "মিল চালু (${String.format(Locale.US, "%.1f", totalCount)})"
                    else "Present (${String.format(Locale.US, "%.1f", totalCount)})"
                } else {
                    if (language == Language.BENGALI) "মিল বন্ধ (০)"
                    else "Absent (0)"
                },
                color = if (isPresent) Color.White else InactiveTextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            if (!isAdmin) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Read only",
                    tint = if (isPresent) Color.White.copy(alpha = 0.8f) else InactiveTextColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * 3-button selector for a single meal with bright, distinct active colors.
 * - OFF (0.0): Distinct Crimson Red when selected
 * - HALF (0.5): Glowing Radiant Amber Gold when selected
 * - FULL (1.0): Radiant Bright Emerald Green when selected
 */
@Composable
private fun VibrantMealSelectorRow(
    mealName: String,
    currentValue: Double,
    language: Language,
    allowHalfPot: Boolean = true,
    isAdmin: Boolean = true,
    onOptionClicked: (count: Double) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mealName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Current status indicator pill
            val statusColor = when {
                currentValue == 0.0 -> InactiveTextColor
                currentValue == 0.5 && allowHalfPot -> VividAmberActive
                else -> VividGreenActive
            }
            val statusLabel = when {
                currentValue == 0.0 -> if (language == Language.BENGALI) "বন্ধ (০)" else "OFF (0)"
                currentValue == 0.5 && allowHalfPot -> if (language == Language.BENGALI) "হাফ পট (০.৫)" else "Half (0.5)"
                else -> if (language == Language.BENGALI) "ফুল পট (১)" else "Full (1)"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Option 1: OFF (0.0)
            VibrantOptionButton(
                label = if (language == Language.BENGALI) "বন্ধ (০)" else "OFF (0)",
                isSelected = (currentValue == 0.0),
                activeColor = VividRedOff,
                activeBorderColor = VividRedBorder,
                enabled = isAdmin,
                modifier = Modifier.weight(1f),
                onClick = { onOptionClicked(0.0) }
            )

            // Option 2: HALF (0.5) - ONLY if allowHalfPot is true (Breakfast only)
            if (allowHalfPot) {
                VibrantOptionButton(
                    label = if (language == Language.BENGALI) "হাফ (০.৫)" else "Half (0.5)",
                    isSelected = (currentValue == 0.5),
                    activeColor = VividAmberActive,
                    activeBorderColor = VividAmberBorder,
                    enabled = isAdmin,
                    modifier = Modifier.weight(1f),
                    onClick = { onOptionClicked(0.5) }
                )
            }

            // Option 3: FULL POT (1.0)
            VibrantOptionButton(
                label = if (language == Language.BENGALI) "ফুল পট (১)" else "Full (1)",
                isSelected = (currentValue >= 1.0),
                activeColor = VividGreenActive,
                activeBorderColor = VividGreenBorder,
                enabled = isAdmin,
                modifier = Modifier.weight(1f),
                onClick = { onOptionClicked(1.0) }
            )
        }
    }
}

@Composable
private fun VibrantOptionButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    activeBorderColor: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) {
        if (enabled) activeColor else activeColor.copy(alpha = 0.5f)
    } else InactiveButtonBg

    val borderColor = if (isSelected) {
        if (enabled) activeBorderColor else activeBorderColor.copy(alpha = 0.5f)
    } else InactiveButtonBorder

    val textColor = if (isSelected) Color.White else InactiveTextColor
    val elevation = if (isSelected && enabled) 3.dp else 0.dp

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        shadowElevation = elevation,
        modifier = modifier.height(42.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MealStatItem(
    label: String,
    count: Double,
    isHighlighted: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = String.format(Locale.US, "%.1f", count),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) VividGreenActive else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MealStatusChip(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) VividGreenActive.copy(alpha = 0.12f) else InactiveButtonBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) VividGreenActive.copy(alpha = 0.5f) else InactiveButtonBorder
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) VividGreenActive else InactiveTextColor
            )
        }
    }
}

@Composable
fun MemberEditDialog(
    member: Member?,
    language: Language,
    onDismiss: () -> Unit,
    onConfirm: (Member) -> Unit
) {
    var name by remember { mutableStateOf(member?.name ?: "") }
    var phone by remember { mutableStateOf(member?.phone ?: "+8801") }
    var roomNumber by remember { mutableStateOf(member?.roomNumber ?: "") }
    var potDeposits by remember { mutableStateOf(member?.potDeposits?.toString() ?: "20") }
    var cashDeposits by remember { mutableStateOf(member?.cashDeposits?.toString() ?: "2000") }
    var active by remember { mutableStateOf(member?.active ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (member == null)
                    (if (language == Language.BENGALI) "নতুন সদস্য যোগ করুন" else "Add New Member")
                else
                    (if (language == Language.BENGALI) "সদস্য তথ্য পরিবর্তন" else "Edit Member")
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (language == Language.BENGALI) "পূর্ণ নাম" else "Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (language == Language.BENGALI) "ফোন নম্বর" else "Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text(if (language == Language.BENGALI) "রুম বা সিট" else "Room / Seat") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = potDeposits,
                    onValueChange = { potDeposits = it },
                    label = { Text(if (language == Language.BENGALI) "জমা চাউল (পট)" else "Rice Pot Deposit") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cashDeposits,
                    onValueChange = { cashDeposits = it },
                    label = { Text(if (language == Language.BENGALI) "জমা টাকা (৳)" else "Cash Bazaar Deposit (৳)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (language == Language.BENGALI) "বর্তমানে সক্রিয় সদস্য" else "Active Resident")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val updated = (member ?: Member()).copy(
                            name = name,
                            phone = phone,
                            roomNumber = roomNumber,
                            potDeposits = potDeposits.toDoubleOrNull() ?: 0.0,
                            cashDeposits = cashDeposits.toDoubleOrNull() ?: 0.0,
                            active = active
                        )
                        onConfirm(updated)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VividGreenActive)
            ) {
                Text(if (language == Language.BENGALI) "সংরক্ষণ করুন (Save)" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == Language.BENGALI) "বাতিল" else "Cancel")
            }
        }
    )
}

fun shiftDate(dateStr: String, days: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(dateStr) ?: Date()
        cal.add(Calendar.DAY_OF_YEAR, days)
        sdf.format(cal.time)
    } catch (e: Exception) {
        dateStr
    }
}

fun formatReadableDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = SimpleDateFormat("EEEE, MMM d", Locale.US)
        formatter.format(parser.parse(dateStr) ?: Date())
    } catch (e: Exception) {
        dateStr
    }
}
