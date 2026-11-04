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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.data.model.EggSummary
import com.example.data.model.Member
import com.example.data.model.MemberSummary
import com.example.data.model.MessSummary
import com.example.data.model.Utility
import com.example.ui.Language
import com.example.ui.components.BulkWhatsAppDispatchDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PositiveGreen
import com.example.util.PdfReportGenerator
import com.example.util.WhatsAppShareHelper
import java.util.Locale

private val WhatsAppBrandGreen = Color(0xFF25D366)

@Composable
fun ReportsScreen(
    messName: String,
    currentMonth: String,
    summary: MessSummary,
    utilities: List<Utility>,
    eggSummary: EggSummary? = null,
    isAdmin: Boolean = false,
    language: Language,
    onUpdateMember: ((Member) -> Unit)? = null
) {
    val context = LocalContext.current
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // Dialog state for updating a member's phone number
    var memberToEditPhone by remember { mutableStateOf<Pair<MemberSummary, String>?>(null) }
    var showBulkDispatchDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // PDF & Monthly Audit Card (Visible to Everyone)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == Language.ENGLISH) "Monthly Mess Audit & Reports" else "মাসিক মেস অডিট ও রিপোর্ট",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (language == Language.ENGLISH)
                                    "Billing Month: $currentMonth • Meal Rate: ৳${String.format(Locale.US, "%.2f", summary.mealRate)}"
                                else
                                    "চলতি মাস: $currentMonth • বর্তমান মিল রেট: ৳${String.format(Locale.US, "%.2f", summary.mealRate)} / মিল",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ACTION: Generate & Share PDF Report
                    OutlinedButton(
                        onClick = {
                            isGeneratingPdf = true
                            try {
                                val pdfFile = PdfReportGenerator.generateMonthlyReport(
                                    context = context,
                                    messName = messName,
                                    month = currentMonth,
                                    summary = summary,
                                    utilities = utilities
                                )
                                WhatsAppShareHelper.sharePdfFile(context, pdfFile, "$messName Statement $currentMonth")
                            } catch (e: Exception) {
                                Toast.makeText(context, "PDF generation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isGeneratingPdf = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isGeneratingPdf
                    ) {
                        if (isGeneratingPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == Language.ENGLISH) "Export & Download PDF Audit" else "অডিট স্টেটমেন্টের PDF তৈরি করুন",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // WhatsApp Messaging & Dispatch Hub (ONLY FOR ADMIN - HIDDEN FOR MEMBERS)
        if (isAdmin) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, WhatsAppBrandGreen.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        tint = WhatsAppBrandGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (language == Language.ENGLISH)
                                            "WhatsApp Messaging Hub (Admin Only)"
                                        else
                                            "হোয়াটসঅ্যাপ মেসেজিং হাব (অ্যাডমিন)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (language == Language.ENGLISH)
                                        "Broadcast reports to group or 1-click personal statement dispatch to members."
                                    else
                                        "মেস গ্রুপে মাসিক রিপোর্ট পাঠান বা ১ ক্লিকে সবার পার্সোনাল হোয়াটসঅ্যাপে হিসাব পাঠান।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ACTION 1: Share Monthly Report directly to Mess WhatsApp Group
                        Button(
                            onClick = {
                                WhatsAppShareHelper.shareMessMonthlyStatusToGroup(
                                    context = context,
                                    month = currentMonth,
                                    messName = messName,
                                    summary = summary,
                                    utilities = utilities,
                                    eggSummary = eggSummary
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppBrandGreen,
                                contentColor = Color.White
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == Language.ENGLISH)
                                        "Share Monthly Report to Mess WhatsApp Group"
                                    else
                                        "📢 হোয়াটসঅ্যাপ গ্রুপে পুরো মেসের মাসিক রিপোর্ট পাঠান",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ACTION 2: 1-Click Send All Member Statements to Personal WhatsApp Inboxes
                        Button(
                            onClick = { showBulkDispatchDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == Language.ENGLISH)
                                        "⚡ 1-Click Send All to Personal WhatsApp Inboxes"
                                    else
                                        "📱 ১ ক্লিকে সবার পার্সোনাল হোয়াটসঅ্যাপে হিসাব পাঠান",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Title for Individual Member Statements
        item {
            Column {
                Text(
                    text = if (language == Language.ENGLISH)
                        "Individual Member Statements"
                    else
                        "সদস্যভিত্তিক ব্যক্তিগত হিসাব বিবরণী",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (language == Language.ENGLISH)
                        "Itemized statement starting from rice pots deposit down to meal costs and net balance."
                    else
                        "চাউল জমা থেকে শুরু করে প্রতিটি সদস্যের সম্পূর্ণ মিল ও খরচের অডিট বিবরণী।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Member Individual Vouchers
        items(summary.memberSummaries) { memSummary ->
            val member = memSummary.member
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with name & net status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (member.phone.isNotBlank())
                                        "${if (language == Language.ENGLISH) "Phone:" else "ফোন:"} ${member.phone}"
                                    else
                                        (if (language == Language.ENGLISH) "No phone (tap to add)" else "ফোন নম্বর নেই (ট্যাপ করে লিখুন)"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (member.phone.isNotBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        memberToEditPhone = Pair(memSummary, member.phone)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit phone",
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            if (member.roomNumber.isNotBlank()) {
                                Text(
                                    text = "Room: ${member.roomNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Net Due / Refund Pill
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
                                    "+৳${String.format(Locale.US, "%.0f", memSummary.netBalance)} (${if (language == Language.ENGLISH) "Refund" else "ফেরত পাবে"})"
                                else
                                    "-৳${String.format(Locale.US, "%.0f", -memSummary.netBalance)} (${if (language == Language.ENGLISH) "Due" else "বকেয়া জমা"})",
                                color = if (memSummary.netBalance >= 0) PositiveGreen else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Detail Metrics Grid: Starts with Rice pot accounting as specifically requested
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(12.dp)
                    ) {
                        // 1. Rice Pots Accounting
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (language == Language.ENGLISH) "🍚 Rice Pots (Dep / Eat / Rem):" else "🍚 চাউল (জমা / খাওয়া / স্থিতি):",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", member.potDeposits)} / ${String.format(Locale.US, "%.1f", memSummary.totalPotsConsumed)} / ${
                                    if (memSummary.remainingPots >= 0) "+${String.format(Locale.US, "%.1f", memSummary.remainingPots)}"
                                    else "${String.format(Locale.US, "%.1f", memSummary.remainingPots)}"
                                } pots",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (memSummary.remainingPots >= 0) PositiveGreen else MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 2. Meals breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (language == Language.ENGLISH) "🍲 Meals (B / L / D = Total):" else "🍲 মিল (সকাল / দুপুর / রাত = মোট):",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", memSummary.totalBreakfast)} + ${String.format(Locale.US, "%.1f", memSummary.totalLunch)} + ${String.format(Locale.US, "%.1f", memSummary.totalDinner)} = ${String.format(Locale.US, "%.1f", memSummary.totalMeals)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 3. Costs & Cash Deposits
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (language == Language.ENGLISH) "💰 Total Cost (Meal + Utility):" else "💰 সর্বমোট খরচ (মিল + বিল):",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "৳${String.format(Locale.US, "%.1f", memSummary.totalCost)} (বিল: ৳${String.format(Locale.US, "%.1f", memSummary.utilityShare)})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (language == Language.ENGLISH) "💵 Cash Deposited:" else "💵 জমা টাকা:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "৳${String.format(Locale.US, "%.1f", memSummary.cashDeposited)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog for entering / updating member phone number
    if (memberToEditPhone != null) {
        val (memSummary, initialPhone) = memberToEditPhone!!
        var phoneInput by remember { mutableStateOf(initialPhone) }

        AlertDialog(
            onDismissRequest = { memberToEditPhone = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (language == Language.ENGLISH)
                        "Phone Number for ${memSummary.member.name}"
                    else
                        "${memSummary.member.name} এর ফোন নম্বর"
                )
            },
            text = {
                Column {
                    Text(
                        text = if (language == Language.ENGLISH)
                            "Enter contact mobile number for this member:"
                        else
                            "সদস্যের যোগাযোগের মোবাইল নম্বর লিখুন (যেমন: 017xxxxxxxx):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text(if (language == Language.ENGLISH) "Mobile Number" else "মোবাইল নম্বর") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedMember = memSummary.member.copy(phone = phoneInput.trim())
                        onUpdateMember?.invoke(updatedMember)
                        memberToEditPhone = null
                        Toast.makeText(
                            context,
                            if (language == Language.ENGLISH) "Phone number saved!" else "ফোন নম্বর সংরক্ষিত হয়েছে!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (language == Language.ENGLISH) "Save" else "সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToEditPhone = null }) {
                    Text(if (language == Language.ENGLISH) "Cancel" else "বাতিল")
                }
            }
        )
    }

    if (showBulkDispatchDialog) {
        BulkWhatsAppDispatchDialog(
            summary = summary,
            currentMonth = currentMonth,
            messName = messName,
            language = language,
            onDismiss = { showBulkDispatchDialog = false }
        )
    }
}
