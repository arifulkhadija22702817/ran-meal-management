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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import com.example.ui.components.ChangeAdminPasswordDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminHandoverRecord
import com.example.data.model.AuditLog
import com.example.data.model.Mess
import com.example.ui.Language
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.EmeraldPrimary
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AdminAuditScreen(
    mess: Mess?,
    auditLogs: List<AuditLog>,
    adminHistory: List<AdminHandoverRecord> = emptyList(),
    isAdmin: Boolean,
    language: Language,
    onTransferManager: (newEmail: String, note: String, onDone: (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    onTransferAdmin: (newEmail: String, newUid: String) -> Unit = { _, _ -> },
    onTransferAdminWithPassword: (currentPassword: String, newEmail: String, newPassword: String?, onDone: (Boolean) -> Unit) -> Unit = { _, _, _, _ -> },
    onChangePassword: (oldPass: String?, newPass: String, onDone: (Boolean, String) -> Unit) -> Unit = { _, _, _ -> }
) {
    var showTransferDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_audit_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Admin Governance & Monthly Manager Transition Card
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == Language.ENGLISH) "Mess Manager Governance" else "মেস ম্যানেজার ও অ্যাডমিন নিয়ন্ত্রণ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (language == Language.ENGLISH) "Single Active Admin Policy (Monthly Handover)" else "একক সক্রিয় ম্যানেজার সিকিউরিটি ও মাসিক হস্তান্তর",
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
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = AmberSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (language == Language.ENGLISH) "Current Designated Manager Gmail:" else "বর্তমান দায়িত্বপ্রাপ্ত ম্যানেজারের জিমেইল:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = mess?.managerEmail ?: "Not set",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (language == Language.ENGLISH)
                            "Every month when a new manager takes charge, the current manager can verify their password and securely set the new manager's Gmail."
                        else
                            "প্রতি মাসে নতুন ম্যানেজার দায়িত্ব নেওয়ার সময় বর্তমান অ্যাডমিন পাসওয়ার্ড দিয়ে নতুন ম্যানেজারের জিমেইল সেট করে সহজে দায়িত্ব হস্তান্তর করতে পারবেন। পূর্বের ম্যানেজারের তথ্য স্বয়ংক্রিয়ভাবে ক্লাউড থেকে আপডেট হয়ে যাবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isAdmin) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { showChangePasswordDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberSecondary)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (language == Language.ENGLISH) "🔑 Change / Update Admin Password" else "🔑 মেস অ্যাডমিন পাসওয়ার্ড পরিবর্তন / আপডেট",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Button(
                                onClick = { showTransferDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (language == Language.ENGLISH) "Monthly Manager Handover (Gmail Change)" else "মাসিক ম্যানেজার পরিবর্তন ও জিমেইল হস্তান্তর",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = if (language == Language.ENGLISH)
                                "Logged in as border/member. Only the active manager can transfer roles or modify settings."
                            else
                                "আপনি সদস্য হিসেবে লগইন আছেন। শুধুমাত্র সক্রিয় ম্যানেজার দায়িত্ব হস্তান্তর করতে পারবেন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Online Real-time Cloud Sync & Sharing Information Card (Answers user question about GitHub / Vercel / Live Updates)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == Language.ENGLISH) "🌐 Online Real-Time Cloud Sync Guide" else "🌐 অনলাইন লাইভ ক্লাউড সিঙ্ক ও শেয়ারিং গাইড",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (language == Language.ENGLISH) "Google Firebase Firestore Live Sync" else "ভার্সেল ছাড়াই সবার ফোনে স্বয়ংক্রিয় লাইভ আপডেট",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (language == Language.ENGLISH)
                            "1. Live Cloud Database: This app is directly connected to Google Firebase Firestore. Vercel is for websites; this is a native Android APK app.\n" +
                            "2. Real-Time Sync on All Phones: Whenever the manager saves or updates meals, bazaar, rice, eggs, or deposits, the Firestore cloud snapshot automatically updates every border's phone instantly!\n" +
                            "3. How to Share with Borders: Export the APK from the top-right AI Studio settings and share via WhatsApp/Google Drive. Once installed on Android phones, everyone sees live updates.\n" +
                            "4. GitHub: You can export your source code to GitHub anytime via AI Studio export options for version control."
                        else
                            "১. লাইভ ক্লাউড ডাটাবেস: আপনার এই মেস অ্যাপটি সরাসরি গুগল ক্লাউডের Firebase Firestore-এর সাথে সংযুক্ত। ভার্সেল (Vercel) সাধারণত ওয়েবসাইট হোস্ট করার জন্য লাগে, কিন্তু এটি একটি পূর্ণাঙ্গ অ্যান্ড্রয়েড (APK) অ্যাপ। তাই ভার্সেলের কোনো প্রয়োজন নেই।\n\n" +
                            "২. সবার ফোনে রিয়েলটাইম আপডেট: ম্যানেজার যখনই মিল, বাজার, চাল, ডিম বা জমা টাকা এন্ট্রি/আপডেট করবেন, সাথে সাথে ক্লাউড লিসেনারের মাধ্যমে মেসের সকল বর্ডারের ফোনে স্বয়ংক্রিয়ভাবে লাইভ আপডেট হয়ে যাবে!\n\n" +
                            "৩. মেসের বর্ডারদের মাঝে অ্যাপ শেয়ার করার উপায়: AI Studio-র উপরের ডানপাশের সেটিংস / মেনু থেকে 'Export APK' অথবা প্রোজেক্ট ডাউনলোড করে হোয়াটসঅ্যাপ (WhatsApp) বা গুগল ড্রাইভে APK ফাইলটি শেয়ার করে দিন। সবার ফোনে ইনস্টল করলেই মেসের লাইভ ডাটা দেখতে পারবে।\n\n" +
                            "৪. গিটহাব (GitHub): ব্যাকআপ ও সোর্স কোড সংরক্ষণের জন্য AI Studio থেকে সরাসরি 'Export to GitHub' করতে পারবেন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Section Title: Admin Handover History (মাসিক ম্যানেজার পরিবর্তনের ইতিহাস)
        if (adminHistory.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = AmberSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == Language.ENGLISH) "Monthly Admin Handover History" else "মাসিক ম্যানেজার পরিবর্তনের ইতিহাস",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(adminHistory) { record ->
                val handoverDate = record.transferredAt?.toDate()?.let {
                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(it)
                } ?: "Recently"

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (language == Language.ENGLISH) "Admin Transferred" else "ম্যানেজার হস্তান্তর",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = EmeraldPrimary
                                )
                            }
                            Text(
                                text = handoverDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${if (language == Language.ENGLISH) "From:" else "পূর্বের:"} ${record.previousAdminEmail.ifBlank { "Bootstrap" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${if (language == Language.ENGLISH) "To (Active):" else "নতুন ম্যানেজার:"} ${record.newAdminEmail}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }

                        if (record.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${if (language == Language.ENGLISH) "Note:" else "বিবরণ:"} ${record.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }

                        if (record.handoverMonth.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${if (language == Language.ENGLISH) "Month:" else "মাস:"} ${record.handoverMonth}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Section Title: Audit Trail
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (language == Language.ENGLISH) "Cloud Change History & Audit Logs" else "ক্লাউড পরিবর্তন হিস্ট্রি ও অডিট ট্রেইল",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (auditLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (language == Language.ENGLISH) "No audit actions logged yet." else "এখনও কোনো অডিট লগ নেই।",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // List of audit items
        items(auditLogs) { log ->
            val dateFormatted = log.timestamp?.toDate()?.let {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(it)
            } ?: "Just now"

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AmberSecondary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = log.action,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = AmberSecondary
                            )
                        }
                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = log.details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${if (language == Language.ENGLISH) "Executed by:" else "সম্পাদন করেছেন:"} ${log.performedBy}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    // Change Admin Password Dialog
    if (showChangePasswordDialog) {
        ChangeAdminPasswordDialog(
            hasExistingPassword = (mess?.adminPassword ?: "").isNotBlank(),
            language = language,
            onDismiss = { showChangePasswordDialog = false },
            onChangePassword = onChangePassword
        )
    }

    // Monthly Transfer Admin Dialog
    if (showTransferDialog) {
        MonthlyTransferAdminDialog(
            currentManagerEmail = mess?.managerEmail ?: "",
            hasExistingPassword = (mess?.adminPassword ?: "").isNotBlank(),
            language = language,
            onDismiss = { showTransferDialog = false },
            onConfirmTransfer = { currentPass, newEmail, newPass, note, onDone ->
                onTransferAdminWithPassword(currentPass, newEmail, newPass, onDone)
            }
        )
    }
}

@Composable
fun MonthlyTransferAdminDialog(
    currentManagerEmail: String,
    hasExistingPassword: Boolean,
    language: Language,
    onDismiss: () -> Unit,
    onConfirmTransfer: (currentPass: String, newEmail: String, newPassword: String?, note: String, onDone: (Boolean) -> Unit) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (language == Language.ENGLISH) "Monthly Manager Handover" else "মাসিক ম্যানেজার হস্তান্তর ও জিমেইল পরিবর্তন")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (language == Language.ENGLISH)
                        "You are authenticated as the active mess manager ($currentManagerEmail). Entering the new manager's Gmail will atomically transfer admin privileges. Your admin write access will be revoked immediately on the backend."
                    else
                        "আপনি বর্তমানে সক্রিয় মেস ম্যানেজার ($currentManagerEmail) হিসেবে লগইন আছেন। পরবর্তী মাসের নতুন ম্যানেজারের Gmail এড্রেস দিলে তাৎক্ষণিকভাবে ক্লাউডে পূর্ণ এডমিন ক্ষমতা হস্তান্তর হবে এবং আপনার এডমিন পারমিশন বাতিল হবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (hasExistingPassword) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it; errorMessage = null },
                        label = { Text(if (language == Language.ENGLISH) "Your Current Admin Password *" else "আপনার বর্তমান অ্যাডমিন পাসওয়ার্ড *") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it; errorMessage = null },
                    label = { Text(if (language == Language.ENGLISH) "New Manager Gmail *" else "নতুন ম্যানেজারের Gmail এড্রেস *") },
                    placeholder = { Text("newmanager@gmail.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMessage = null },
                    label = { Text(if (language == Language.ENGLISH) "New Manager Password (Optional)" else "নতুন ম্যানেজারের পাসওয়ার্ড (ঐচ্ছিক)") },
                    placeholder = { Text(if (language == Language.ENGLISH) "Leave blank for new manager to set" else "খালি রাখলে নতুন ম্যানেজার নিজে সেট করবেন") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (language == Language.ENGLISH) "Handover Note / Month" else "হস্তান্তর বিবরণ / মাস") },
                    placeholder = { Text(if (language == Language.ENGLISH) "e.g. November 2026 Manager" else "যেমন: নভেম্বর ২০২৬ মেস ম্যানেজার") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanEmail = newEmail.trim().lowercase()
                    if (hasExistingPassword && currentPassword.isBlank()) {
                        errorMessage = if (language == Language.ENGLISH) "Please enter your current admin password" else "আপনার বর্তমান অ্যাডমিন পাসওয়ার্ড লিখুন"
                        return@Button
                    }
                    if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                        errorMessage = if (language == Language.ENGLISH) "Please enter a valid Gmail" else "সঠিক জিমেইল এড্রেস লিখুন"
                        return@Button
                    }
                    if (cleanEmail == currentManagerEmail.lowercase()) {
                        errorMessage = if (language == Language.ENGLISH) "New manager email cannot be the same as current" else "নতুন ম্যানেজারের ইমেইল বর্তমান ইমেইল থেকে আলাদা হতে হবে"
                        return@Button
                    }
                    isSubmitting = true
                    val handoverNote = note.trim().ifBlank { "Monthly Manager Handover" }
                    val cleanNewPass = newPassword.trim().ifBlank { null }
                    onConfirmTransfer(currentPassword.trim(), cleanEmail, cleanNewPass, handoverNote) { success ->
                        isSubmitting = false
                        if (success) {
                            onDismiss()
                        } else {
                            errorMessage = if (language == Language.ENGLISH) "Handover failed! Check password or network." else "হস্তান্তর ব্যর্থ হয়েছে! বর্তমান পাসওয়ার্ড বা নেটওয়ার্ক পরীক্ষা করুন।"
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (language == Language.ENGLISH) "Confirm Handover" else "হস্তান্তর নিশ্চিত করুন")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text(if (language == Language.ENGLISH) "Cancel" else "বাতিল")
            }
        }
    )
}
