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
import androidx.compose.material.icons.filled.ShoppingCart
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
import com.example.data.model.Expense
import com.example.data.model.Member
import com.example.ui.Language
import com.example.ui.theme.EmeraldPrimary
import com.example.util.AntiCorruptionPolicy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BazaarScreen(
    expenses: List<Expense>,
    members: List<Member>,
    currentMonth: String,
    isAdmin: Boolean,
    language: Language,
    onSaveExpense: (Expense) -> Unit,
    onDeleteExpense: (expenseId: String, title: String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showLockedDialog by remember { mutableStateOf(false) }
    var lockedDialogMessage by remember { mutableStateOf("") }

    val monthExpenses = expenses.filter { it.date.startsWith(currentMonth) }
    val totalMonthBazaar = monthExpenses.sumOf { it.amount }

    Scaffold(
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_bazaar_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Bazaar Item")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("bazaar_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    "🔒 ইউজার ভিউ মোড: সাধারণ সদস্য হিসেবে আপনি বাজার খরচের তালিকা শুধুমাত্র দেখতে পারবেন। বাজার খরচ যোগ বা ডিলিট করার ক্ষমতা শুধুমাত্র অ্যাডমিনের।"
                                else
                                    "🔒 Viewer Mode: As a member, bazaar records are read-only. Only the admin can add or delete expenses.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (language == Language.ENGLISH) "Total Bazaar Expenditure" else "চলতি মাসের মোট বাজার খরচ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.US, "৳ %.2f", totalMonthBazaar),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
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
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Section Header
            item {
                Text(
                    text = if (language == Language.ENGLISH) "Bazaar Purchases ($currentMonth)" else "বাজার ক্রয়ের বিবরণী ($currentMonth)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (monthExpenses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (language == Language.ENGLISH) "No bazaar expenses recorded this month." else "এই মাসে এখনও কোনো বাজার খরচ তোলা হয়নি।",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // List of Expenses
            items(monthExpenses) { expense ->
                val payer = members.firstOrNull { it.id == expense.payerMemberId }?.name ?: "Mess Pool"
                val isLocked = AntiCorruptionPolicy.isExpenseLocked(expense)

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
                                    text = expense.title,
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
                                        text = expense.category,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (isLocked) {
                                    Spacer(modifier = Modifier.width(6.dp))
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
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${expense.date} • ${if (language == Language.ENGLISH) "Shopped by:" else "বাজার করেছেন:"} $payer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format(Locale.US, "৳ %.0f", expense.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                            if (isAdmin) {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (isLocked) {
                                            lockedDialogMessage = if (language == Language.BENGALI)
                                                "এই বাজার খরচটি (${expense.title} - ৳${expense.amount.toInt()}) ২৪ ঘণ্টার বেশি আগে দাখিল করা হয়েছিল। দুর্নীতি ও হিসাব পরিবর্তন রোধে ২৪ ঘণ্টা পর এই রেকর্ড স্থায়ীভাবে লক হয়ে গেছে। এটি আর মোছা যাবে না।"
                                            else
                                                "This bazaar expense (${expense.title}) was recorded over 24 hours ago. For anti-corruption protection, it is permanently locked and cannot be deleted."
                                            showLockedDialog = true
                                        } else {
                                            expenseToDelete = expense
                                        }
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Delete,
                                        contentDescription = if (isLocked) "Locked Expense" else "Delete",
                                        tint = if (isLocked) Color.Gray else MaterialTheme.colorScheme.error,
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

    // Add Expense Dialog
    if (showAddDialog) {
        AddExpenseDialog(
            members = members,
            language = language,
            onDismiss = { showAddDialog = false },
            onConfirm = { exp ->
                onSaveExpense(exp)
                showAddDialog = false
            }
        )
    }

    // Delete Expense Confirmation
    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text(if (language == Language.ENGLISH) "Delete Bazaar Entry" else "বাজার খরচ মুছুন") },
            text = {
                Text(
                    if (language == Language.ENGLISH)
                        "Are you sure you want to delete '${expenseToDelete!!.title}'?"
                    else
                        "আপনি কি নিশ্চিত যে '${expenseToDelete!!.title}' মুছে ফেলবেন?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense(expenseToDelete!!.id, expenseToDelete!!.title)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (language == Language.ENGLISH) "Delete" else "মুছুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text(if (language == Language.ENGLISH) "Cancel" else "বাতিল")
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
fun AddExpenseDialog(
    members: List<Member>,
    language: Language,
    onDismiss: () -> Unit,
    onConfirm: (Expense) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var selectedPayerId by remember { mutableStateOf(members.firstOrNull()?.id ?: "") }
    var category by remember { mutableStateOf("BAZAAR") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (language == Language.ENGLISH) "Add Bazaar Expense" else "নতুন বাজার খরচ যুক্ত করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (language == Language.ENGLISH) "Items Description (e.g. Fish, Veg, Oil)" else "বাজারের বিবরণ (মাছ, মাংস, সবজি ইত্যাদি)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(if (language == Language.ENGLISH) "Amount in ৳ (Taka)" else "খরচের পরিমাণ (৳)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text(if (language == Language.ENGLISH) "Date (YYYY-MM-DD)" else "তারিখ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("BAZAAR", "SPICES", "EXTRA").forEach { cat ->
                        val isSelected = (category == cat)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { category = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                            Expense(
                                title = title,
                                amount = amt,
                                date = date,
                                payerMemberId = selectedPayerId,
                                category = category
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(if (language == Language.ENGLISH) "Save Expense" else "যুক্ত করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == Language.ENGLISH) "Cancel" else "বাতিল")
            }
        }
    )
}
