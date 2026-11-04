package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemberSummary
import com.example.data.model.MessSummary
import com.example.ui.Language
import com.example.util.WhatsAppShareHelper
import java.util.Locale

private val WhatsAppBrandColor = Color(0xFF25D366)
private val SentGreen = Color(0xFF10B981)

@Composable
fun BulkWhatsAppDispatchDialog(
    summary: MessSummary,
    currentMonth: String,
    messName: String,
    language: Language,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val memberSummaries = summary.memberSummaries

    // Track which members have been dispatched in this session
    val sentMap = remember { mutableStateMapOf<String, Boolean>() }

    val totalCount = memberSummaries.size
    val sentCount = sentMap.values.count { it }
    val progress = if (totalCount > 0) sentCount.toFloat() / totalCount else 0f

    // Find next pending member with a phone number
    val nextPending = memberSummaries.firstOrNull {
        it.member.phone.isNotBlank() && sentMap[it.member.id] != true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(WhatsAppBrandColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = WhatsAppBrandColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (language == Language.BENGALI)
                            "সবার ইনবক্সে ব্যক্তিগত হিসাব প্রেরণ"
                        else
                            "One-Click All Member Dispatcher",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (language == Language.BENGALI)
                            "এক ক্লিকে প্রতি সদস্যের হোয়াটসঅ্যাপে রিপোর্ট যাবে"
                        else
                            "Directly send personal statement to each member's inbox",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Progress Bar Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == Language.BENGALI)
                                    "অগ্রগতি (Progress): $sentCount / $totalCount জন পাঠানো হয়েছে"
                                else
                                    "Progress: $sentCount / $totalCount sent",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (sentCount == totalCount && totalCount > 0) {
                                Text(
                                    text = "🎉 সম্পন্ন!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SentGreen
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = WhatsAppBrandColor,
                            trackColor = Color(0xFFE2E8F0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fast Quick Action: Send to Next Pending Member
                if (nextPending != null) {
                    Button(
                        onClick = {
                            val mem = nextPending
                            sentMap[mem.member.id] = true
                            WhatsAppShareHelper.shareMemberStatementToWhatsApp(
                                context = context,
                                summary = mem,
                                month = currentMonth,
                                mealRate = summary.mealRate,
                                messName = messName,
                                calculationMode = summary.calculationMode
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppBrandColor,
                            contentColor = Color.White
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == Language.BENGALI)
                                    "⚡ পরবর্তী জন (${nextPending.member.name}) কে পাঠান"
                                else
                                    "⚡ Send Next (${nextPending.member.name})",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Member List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(memberSummaries) { memSummary ->
                        val member = memSummary.member
                        val isSent = sentMap[member.id] == true
                        val hasPhone = member.phone.isNotBlank()

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSent) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSent) SentGreen else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSent) SentGreen.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSent) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = SentGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (hasPhone) member.phone else "ফোন নম্বর নেই",
                                            fontSize = 11.sp,
                                            color = if (hasPhone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = if (memSummary.netBalance >= 0)
                                                "ফেরত: +৳${String.format(Locale.US, "%.0f", memSummary.netBalance)}"
                                            else
                                                "বকেয়া: -৳${String.format(Locale.US, "%.0f", -memSummary.netBalance)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (memSummary.netBalance >= 0) SentGreen else Color(0xFFDC2626)
                                        )
                                    }
                                }

                                if (hasPhone) {
                                    Button(
                                        onClick = {
                                            sentMap[member.id] = true
                                            WhatsAppShareHelper.shareMemberStatementToWhatsApp(
                                                context = context,
                                                summary = memSummary,
                                                month = currentMonth,
                                                mealRate = summary.mealRate,
                                                messName = messName,
                                                calculationMode = summary.calculationMode
                                            )
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSent) SentGreen else WhatsAppBrandColor,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSent) Icons.Default.Check else Icons.Default.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isSent) "পাঠানো শেষ" else "পাঠান",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "⚠️ নম্বর নেই",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (language == Language.BENGALI) "শেষ করুন" else "Done")
            }
        }
    )
}
