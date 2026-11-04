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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
fun RiceStockScreen(
    summary: MessSummary,
    isAdmin: Boolean,
    language: Language,
    onUpdateMemberDeposit: (Member) -> Unit
) {
    var memberToEditDeposit by remember { mutableStateOf<Member?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("rice_stock_screen"),
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
                                "🔒 ইউজার ভিউ মোড: সাধারণ সদস্য হিসেবে আপনি চাউলের হিসাব শুধুমাত্র দেখতে পারবেন। সদস্যদের চাউলের পট জমা আপডেট করার ক্ষমতা শুধুমাত্র অ্যাডমিনের।"
                            else
                                "🔒 Viewer Mode: As a member, rice deposits are read-only. Only the mess manager can update pot deposits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // High-level Pot Summary Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (language == Language.ENGLISH) "Total Mess Rice Inventory" else "মেসের সার্বিক চাউলের হিসাব",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (language == Language.ENGLISH) "Measured in standard pots (কৌটা / পট)" else "স্ট্যান্ডার্ড পট বা কৌটা ভিত্তিক হিসাব",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AmberSecondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = AmberSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PotMetric(
                            label = if (language == Language.ENGLISH) "Deposited" else "মোট জমা",
                            value = "${String.format(Locale.US, "%.1f", summary.totalPotsDeposited)} pots"
                        )
                        PotMetric(
                            label = if (language == Language.ENGLISH) "Consumed" else "মোট খাওয়া",
                            value = "${String.format(Locale.US, "%.1f", summary.totalPotsConsumed)} pots"
                        )
                        PotMetric(
                            label = if (language == Language.ENGLISH) "In Stock" else "মজুদ চাউল",
                            value = "${String.format(Locale.US, "%.1f", summary.remainingMessPots)} pots",
                            highlight = true
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = if (language == Language.ENGLISH) "Member Pot Balances (জমা ও অবশিষ্ট)" else "সদস্যভিত্তিক চাউলের স্টক ও ব্যালেন্স",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Member breakdown
        items(summary.memberSummaries) { memSummary ->
            val member = memSummary.member
            val remaining = memSummary.remainingPots
            val isDeficit = remaining < 0
            val isOutOfRice = remaining <= 0.0
            val isOutOfMoney = memSummary.netBalance < 0
            val isDepleted = isOutOfRice || isOutOfMoney

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDepleted) Color(0xFFFFF5F5) else MaterialTheme.colorScheme.surface
                ),
                border = if (isDepleted) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.7f)) else null,
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
                                    isOutOfRice && isOutOfMoney -> "⚠️ চাউল ও টাকা শেষ (চাউল পট ও টাকা জমা লাগবে)"
                                    isOutOfRice -> "⚠️ চাউল শেষ (চাউল পট জমা লাগবে)"
                                    else -> "⚠️ টাকা শেষ (ম্যানেজারকে টাকা দেওয়া লাগবে)"
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${if (language == Language.ENGLISH) "Deposited:" else "জমা:"} ${String.format(Locale.US, "%.1f", member.potDeposits)} pots • ${if (language == Language.ENGLISH) "Eaten:" else "খাওয়া:"} ${String.format(Locale.US, "%.1f", memSummary.totalPotsConsumed)} pots",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Stock badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isDeficit) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                    else PositiveGreen.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isDeficit)
                                    "${String.format(Locale.US, "%.1f", remaining)} (${if (language == Language.ENGLISH) "Deficit" else "ঘাটতি"})"
                                else
                                    "+${String.format(Locale.US, "%.1f", remaining)} (${if (language == Language.ENGLISH) "Pots Left" else "অবশিষ্ট"})",
                                color = if (isDeficit) MaterialTheme.colorScheme.error else PositiveGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        if (isAdmin) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { memberToEditDeposit = member },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Pot Deposit",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Pot Deposit Dialog
    if (memberToEditDeposit != null) {
        var newDepositStr by remember { mutableStateOf(memberToEditDeposit!!.potDeposits.toString()) }

        AlertDialog(
            onDismissRequest = { memberToEditDeposit = null },
            title = {
                Text(
                    if (language == Language.ENGLISH) "Update Rice Deposit for ${memberToEditDeposit!!.name}"
                    else "${memberToEditDeposit!!.name} এর চাউল জমা আপডেট করুন"
                )
            },
            text = {
                Column {
                    Text(
                        text = if (language == Language.ENGLISH)
                            "Enter the total rice pots brought or deposited by this member:"
                        else
                            "সদস্যের জমা দেয়া মোট চাউলের পট লিখুন:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newDepositStr,
                        onValueChange = { newDepositStr = it },
                        label = { Text(if (language == Language.ENGLISH) "Pots (কৌটা / পট)" else "পট সংখ্যা") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pots = newDepositStr.toDoubleOrNull() ?: memberToEditDeposit!!.potDeposits
                        onUpdateMemberDeposit(memberToEditDeposit!!.copy(potDeposits = pots))
                        memberToEditDeposit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (language == Language.ENGLISH) "Save Deposit" else "জমা আপডেট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToEditDeposit = null }) {
                    Text(if (language == Language.ENGLISH) "Cancel" else "বাতিল")
                }
            }
        )
    }
}

@Composable
private fun PotMetric(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (highlight) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}
