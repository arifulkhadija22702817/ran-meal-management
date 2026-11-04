package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Member
import com.example.data.model.MemberSummary
import com.example.data.model.MessSummary
import com.example.ui.Language
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PositiveGreen
import java.util.Locale

@Composable
fun JomaTkScreen(
    summary: MessSummary,
    currentMonth: String,
    messName: String,
    isAdmin: Boolean,
    language: Language,
    onUpdateMemberDeposit: (member: Member) -> Unit
) {
    val context = LocalContext.current

    // Dialog state for adding/editing deposit
    var memberToUpdateDeposit by remember { mutableStateOf<MemberSummary?>(null) }
    var pendingConfirmation by remember { mutableStateOf<Pair<Member, Double>?>(null) }
    var showUnauthorizedDialog by remember { mutableStateOf(false) }
    var memberToEditPhone by remember { mutableStateOf<Member?>(null) }

    // Aggregates for all members in the current month
    val totalCashDeposited = summary.memberSummaries.sumOf { it.cashDeposited }
    val totalCostIncurred = summary.memberSummaries.sumOf { it.totalCost }
    val totalRefundable = summary.memberSummaries.filter { it.netBalance > 0 }.sumOf { it.netBalance }
    val totalDueCollectable = summary.memberSummaries.filter { it.netBalance < 0 }.sumOf { -it.netBalance }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("joma_tk_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Prominent Read-Only Banner for non-admin members
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
                                "🔒 ইউজার ভিউ মোড: সাধারণ সদস্য হিসেবে আপনি জমা টাকা ও ব্যালেন্স শুধুমাত্র দেখতে পারবেন। জমা টাকা যোগ বা আপডেট করার ক্ষমতা শুধুমাত্র মেস ম্যানেজারের।"
                            else
                                "🔒 Viewer Mode: As a member, cash deposits and balances are read-only. Only the mess manager can record or update deposits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // High-level Financial Fund Summary Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (language == Language.BENGALI) "মেস ফান্ডের মোট জমা টাকা (Joma Tk)" else "Total Mess Deposit Fund",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.US, "৳ %.2f", totalCashDeposited),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3-way breakdown grid inside header card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (language == Language.BENGALI) "মোট খাওয়া খরচ" else "Total Cost",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = String.format(Locale.US, "৳ %.0f", totalCostIncurred),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column {
                            Text(
                                text = if (language == Language.BENGALI) "মেস ফেরত দিবে" else "Total Refund",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = String.format(Locale.US, "+৳ %.0f", totalRefundable),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                        Column {
                            Text(
                                text = if (language == Language.BENGALI) "বকেয়া আদায়যোগ্য" else "Total Due",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = String.format(Locale.US, "-৳ %.0f", totalDueCollectable),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCA5A5)
                            )
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (language == Language.BENGALI) "সদস্যদের জমা টাকা ও খরচের লাইভ হিসাব" else "Members Deposit & Balance Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (language == Language.BENGALI)
                            "মিল রেট ও বিলের সাথে স্বয়ংক্রিয়ভাবে হিসাব সমন্বয় হয়"
                        else
                            "Auto-calculated with meal rate & shared utilities",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Members List with Detailed Joma Tk Card
        items(summary.memberSummaries) { memSummary ->
            val member = memSummary.member
            val isRefund = memSummary.netBalance > 0
            val isDue = memSummary.netBalance < 0
            val isOutOfMoney = memSummary.netBalance < 0
            val isOutOfRice = memSummary.remainingPots <= 0
            val isDepleted = isOutOfMoney || isOutOfRice

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDepleted) Color(0xFFFFF5F5) else MaterialTheme.colorScheme.surface
                ),
                border = if (isDepleted) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.7f)) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Member Info & Balance Badge Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isDepleted) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = if (isDepleted) Color(0xFFB91C1C) else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                if (isDepleted) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFEE2E2))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFB91C1C)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when {
                                            isOutOfMoney && isOutOfRice -> "⚠️ টাকা ও চাউল শেষ (টাকা ও চাউল জমা দেওয়া লাগবে)"
                                            isOutOfMoney -> "⚠️ টাকা শেষ (ম্যানেজারকে টাকা দেওয়া লাগবে)"
                                            else -> "⚠️ চাউল শেষ (চাউল পট জমা লাগবে)"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                } else {
                                    Text(
                                        text = member.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${if (member.phone.isNotBlank()) member.phone else "নম্বর নেই"} ${if (member.roomNumber.isNotBlank()) "• রুম: ${member.roomNumber}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isAdmin) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { memberToEditPhone = member },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Phone",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Final Result Badge (Back Pabe / Manager k Dibe)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isRefund -> PositiveGreen.copy(alpha = 0.15f)
                                        isDue -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when {
                                    isRefund -> if (language == Language.BENGALI)
                                        "ফেরত পাবে: +৳${String.format(Locale.US, "%.0f", memSummary.netBalance)}"
                                    else
                                        "Refund: +৳${String.format(Locale.US, "%.0f", memSummary.netBalance)}"
                                    isDue -> if (language == Language.BENGALI)
                                        "ম্যানেজারকে দিবে: -৳${String.format(Locale.US, "%.0f", -memSummary.netBalance)}"
                                    else
                                        "Due: -৳${String.format(Locale.US, "%.0f", -memSummary.netBalance)}"
                                    else -> if (language == Language.BENGALI) "হিসাব সমান (০)" else "Nil (0)"
                                },
                                color = when {
                                    isRefund -> PositiveGreen
                                    isDue -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2-Column Clean Breakdown Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Cash Deposited
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == Language.BENGALI) "💵 জমা দেওয়া টাকা (Joma)" else "💵 Cash Deposited",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.US, "৳ %.2f", member.cashDeposits),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                        }

                        // Right: Consumed Cost
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (language == Language.BENGALI) "🍲 মোট খাওয়া খরচ (Cost)" else "🍲 Consumed Cost",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.US, "৳ %.2f", memSummary.totalCost),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (language == Language.BENGALI)
                                    "(${String.format(Locale.US, "%.1f", memSummary.totalMeals)} মিল + বিল)"
                                else
                                    "(${String.format(Locale.US, "%.1f", memSummary.totalMeals)} meals + bill)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Admin-only: Add / Edit Joma Tk
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { memberToUpdateDeposit = memSummary },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == Language.BENGALI) "টাকা জমা যোগ / এডিট" else "Update Deposit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Interactive Dialog: Add / Update Cash Deposit
    if (memberToUpdateDeposit != null) {
        val memSummary = memberToUpdateDeposit!!
        val member = memSummary.member
        var mode by remember { mutableStateOf("add") } // "add" = add to existing, "set" = set total
        var amountInput by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { memberToUpdateDeposit = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (language == Language.BENGALI)
                        "${member.name} এর জমা টাকা"
                    else
                        "Cash Deposit for ${member.name}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (language == Language.BENGALI)
                            "বর্তমানে জমা আছে: ৳${String.format(Locale.US, "%.2f", member.cashDeposits)}"
                        else
                            "Current Deposit: ৳${String.format(Locale.US, "%.2f", member.cashDeposits)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode Selector: Add to existing vs Set total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { mode = "add" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = if (mode == "add") ButtonDefaults.outlinedButtonColors(containerColor = EmeraldPrimary.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(
                                text = if (language == Language.BENGALI) "নতুন টাকা যোগ (+)" else "Add (+)",
                                fontWeight = if (mode == "add") FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                        OutlinedButton(
                            onClick = { mode = "set" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = if (mode == "set") ButtonDefaults.outlinedButtonColors(containerColor = EmeraldPrimary.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(
                                text = if (language == Language.BENGALI) "মোট জমা পরিবর্তন" else "Set Total",
                                fontWeight = if (mode == "set") FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = {
                            amountInput = it
                            inputError = null
                        },
                        label = {
                            Text(
                                if (mode == "add") {
                                    if (language == Language.BENGALI) "নতুন জমা দেওয়ার পরিমাণ (৳)" else "Amount to Add (৳)"
                                } else {
                                    if (language == Language.BENGALI) "সংশোধিত মোট জমা টাকা (৳)" else "New Total Deposit (৳)"
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = inputError != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (inputError != null) {
                        Text(text = inputError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    // Calculation preview
                    val parsed = amountInput.toDoubleOrNull() ?: 0.0
                    val projectedTotal = if (mode == "add") member.cashDeposits + parsed else parsed
                    val projectedBalance = projectedTotal - memSummary.totalCost

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (language == Language.BENGALI)
                                    "আপডেটের পর মোট জমা হবে: ৳${String.format(Locale.US, "%.0f", projectedTotal)}"
                                else
                                    "Projected Total Deposit: ৳${String.format(Locale.US, "%.0f", projectedTotal)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (projectedBalance >= 0) {
                                    if (language == Language.BENGALI) "👉 সদস্য ফেরত পাবে (Back Pabe): +৳${String.format(Locale.US, "%.0f", projectedBalance)}"
                                    else "Refund: +৳${String.format(Locale.US, "%.0f", projectedBalance)}"
                                } else {
                                    if (language == Language.BENGALI) "👉 ম্যানেজারকে দিতে হবে (Due): -৳${String.format(Locale.US, "%.0f", -projectedBalance)}"
                                    else "Due to Manager: -৳${String.format(Locale.US, "%.0f", -projectedBalance)}"
                                },
                                fontSize = 12.sp,
                                color = if (projectedBalance >= 0) PositiveGreen else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = amountInput.toDoubleOrNull()
                        if (parsed == null || parsed < 0) {
                            inputError = if (language == Language.BENGALI) "সঠিক টাকার পরিমাণ লিখুন" else "Enter valid amount"
                        } else {
                            val finalDeposit = if (mode == "add") member.cashDeposits + parsed else parsed
                            pendingConfirmation = Pair(member, finalDeposit)
                            memberToUpdateDeposit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (language == Language.BENGALI) "এগিয়ে যান" else "Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToUpdateDeposit = null }) {
                    Text(if (language == Language.BENGALI) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Confirmation Alert before saving deposit
    if (pendingConfirmation != null) {
        val (member, newDeposit) = pendingConfirmation!!
        AlertDialog(
            onDismissRequest = { pendingConfirmation = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (language == Language.BENGALI) "জমা টাকা নিশ্চিতকরণ" else "Confirm Deposit",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == Language.BENGALI)
                        "আপনি কি নিশ্চিত যে '${member.name}' এর মোট জমা টাকা ৳${String.format(Locale.US, "%.0f", newDeposit)} হিসেবে সংরক্ষণ করতে চান?"
                    else
                        "Are you sure you want to save total deposit for '${member.name}' as ৳${String.format(Locale.US, "%.0f", newDeposit)}?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = member.copy(cashDeposits = newDeposit)
                        onUpdateMemberDeposit(updated)
                        pendingConfirmation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (language == Language.BENGALI) "হ্যাঁ, সংরক্ষণ করুন" else "Yes, Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmation = null }) {
                    Text(if (language == Language.BENGALI) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Phone Edit Dialog
    if (memberToEditPhone != null) {
        val targetMember = memberToEditPhone!!
        var phoneInput by remember { mutableStateOf(targetMember.phone) }

        AlertDialog(
            onDismissRequest = { memberToEditPhone = null },
            icon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldPrimary) },
            title = {
                Text(
                    text = if (language == Language.BENGALI)
                        "${targetMember.name} এর ফোন নম্বর"
                    else
                        "Phone Number for ${targetMember.name}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (language == Language.BENGALI)
                            "সদস্যের যোগাযোগের মোবাইল নম্বর প্রদান করুন:"
                        else
                            "Enter the member's contact mobile number:"
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text(if (language == Language.BENGALI) "ফোন নম্বর (যেমন: 017xxxxxxxx)" else "Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = targetMember.copy(phone = phoneInput.trim())
                        onUpdateMemberDeposit(updated)
                        memberToEditPhone = null
                        Toast.makeText(
                            context,
                            if (language == Language.BENGALI) "ফোন নম্বর সংরক্ষিত হয়েছে!" else "Phone number saved!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (language == Language.BENGALI) "সংরক্ষণ করুন" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToEditPhone = null }) {
                    Text(if (language == Language.BENGALI) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
