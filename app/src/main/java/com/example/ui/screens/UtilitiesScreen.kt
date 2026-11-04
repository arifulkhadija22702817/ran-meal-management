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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
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
import com.example.data.model.Utility
import com.example.ui.Language
import com.example.ui.theme.EmeraldPrimary
import java.util.Locale

@Composable
fun UtilitiesScreen(
    utilities: List<Utility>,
    currentMonth: String,
    activeMemberCount: Int,
    isAdmin: Boolean,
    language: Language,
    onSaveUtility: (Utility) -> Unit,
    onDeleteUtility: (utilityId: String, title: String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var utilityToDelete by remember { mutableStateOf<Utility?>(null) }

    val monthUtilities = utilities.filter { it.month == currentMonth }
    val totalCost = monthUtilities.sumOf { it.amount }
    val membersCount = activeMemberCount.coerceAtLeast(1)
    val perMemberCost = totalCost / membersCount

    Scaffold(
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_utility_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Shared Utility Bill")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("utilities_screen"),
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
                                    "🔒 ইউজার ভিউ মোড: সাধারণ সদস্য হিসেবে আপনি ইউটিলিটি বিলের হিসাব শুধুমাত্র দেখতে পারবেন। বিল যোগ বা ডিলিট করার ক্ষমতা শুধুমাত্র অ্যাডমিনের।"
                                else
                                    "🔒 Viewer Mode: As a member, utility bills are read-only. Only the mess manager can add or delete bills.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Split Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (language == Language.ENGLISH) "Shared Utility Bill Splitting" else "শেয়ার্ড ইউটিলিটি বিল বণ্টন",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${if (language == Language.ENGLISH) "Active Borders:" else "সক্রিয় সদস্য সংখ্যা:"} $membersCount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (language == Language.ENGLISH) "Total Shared Bills" else "মোট বিল খরচ",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = String.format(Locale.US, "৳ %.2f", totalCost),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (language == Language.ENGLISH) "Per Member Share" else "জনপ্রতি বণ্টন",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = String.format(Locale.US, "৳ %.2f", perMemberCost),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (language == Language.ENGLISH) "Bill Breakdown ($currentMonth)" else "বিলের বিস্তারিত তালিকা ($currentMonth)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (monthUtilities.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (language == Language.ENGLISH) "No shared utilities added for this month." else "এই মাসে কোনো ইউটিলিটি বিল যোগ করা হয়নি।",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            items(monthUtilities) { utility ->
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = utility.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = utility.category,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Split equally among $membersCount members",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format(Locale.US, "৳ %.0f", utility.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            if (isAdmin) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { utilityToDelete = utility },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Utility",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Utility Dialog
    if (showAddDialog) {
        AddUtilityDialog(
            currentMonth = currentMonth,
            language = language,
            onDismiss = { showAddDialog = false },
            onConfirm = { ut ->
                onSaveUtility(ut)
                showAddDialog = false
            }
        )
    }

    // Delete Utility Confirmation
    if (utilityToDelete != null) {
        AlertDialog(
            onDismissRequest = { utilityToDelete = null },
            title = { Text(if (language == Language.ENGLISH) "Delete Utility Bill" else "ইউটিলিটি বিল মুছুন") },
            text = {
                Text(
                    if (language == Language.ENGLISH)
                        "Are you sure you want to remove '${utilityToDelete!!.title}'?"
                    else
                        "আপনি কি '${utilityToDelete!!.title}' মুছে ফেলতে চান?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUtility(utilityToDelete!!.id, utilityToDelete!!.title)
                        utilityToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (language == Language.ENGLISH) "Delete" else "মুছুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { utilityToDelete = null }) {
                    Text(if (language == Language.ENGLISH) "Cancel" else "বাতিল")
                }
            }
        )
    }
}

@Composable
fun AddUtilityDialog(
    currentMonth: String,
    language: Language,
    onDismiss: () -> Unit,
    onConfirm: (Utility) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("WIFI") }

    val categories = listOf("RENT", "ELECTRICITY", "GAS", "WIFI", "MAID", "OTHER")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (language == Language.ENGLISH) "Add Shared Utility Bill" else "নতুন শেয়ার্ড বিল যুক্ত করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (language == Language.ENGLISH) "Bill Title (e.g. Wi-Fi bill, Cook salary)" else "বিলের নাম (ওয়াইফাই, খালা, গ্যাস বিল ইত্যাদি)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(if (language == Language.ENGLISH) "Total Amount (৳)" else "মোট টাকার পরিমাণ (৳)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (language == Language.ENGLISH) "Category" else "ক্যাটাগরি",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        CategoryChip(
                            cat = cat,
                            isSelected = category == cat,
                            onSelect = { category = cat },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.drop(3).forEach { cat ->
                        CategoryChip(
                            cat = cat,
                            isSelected = category == cat,
                            onSelect = { category = cat },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0.0) {
                        onConfirm(
                            Utility(
                                title = title,
                                amount = amt,
                                month = currentMonth,
                                category = category
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(if (language == Language.ENGLISH) "Save Bill" else "যুক্ত করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == Language.ENGLISH) "Cancel" else "বাতিল")
            }
        }
    )
}

@Composable
private fun CategoryChip(
    cat: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onSelect() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cat,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
