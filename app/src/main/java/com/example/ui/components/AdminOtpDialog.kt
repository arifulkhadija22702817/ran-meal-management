package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.service.AdminOtpService
import com.example.ui.Language
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.EmeraldPrimary
import kotlinx.coroutines.delay

@Composable
fun AdminOtpDialog(
    targetEmail: String,
    language: Language,
    onDismiss: () -> Unit,
    onVerifySuccess: () -> Unit,
    onResendOtp: () -> Unit
) {
    var enteredOtp by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableIntStateOf(AdminOtpService.getRemainingSeconds()) }
    var showTestCode by remember { mutableStateOf(false) }

    // Live countdown timer
    LaunchedEffect(Unit) {
        while (true) {
            remainingSeconds = AdminOtpService.getRemainingSeconds()
            delay(1000L)
        }
    }

    val maskedEmail = remember(targetEmail) {
        val parts = targetEmail.split("@")
        if (parts.size == 2 && parts[0].length > 3) {
            val name = parts[0]
            val maskedName = name.take(2) + "***" + name.takeLast(2)
            "$maskedName@${parts[1]}"
        } else {
            targetEmail
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(AmberSecondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin 2FA",
                        tint = AmberSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (language == Language.BENGALI) "অ্যাডমিন ২-স্টেপ যাচাইকরণ" else "Admin 2FA Verification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (language == Language.BENGALI) "নিরাপত্তা কোড (OTP) আবশ্যক" else "Security Code (OTP) Required",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (language == Language.BENGALI)
                        "আপনার মেসের নিবন্ধিত অ্যাডমিনের জিমেইল ($maskedEmail)-এ একটি ৬-সংখ্যার নিরাপত্তা কোড পাঠানো হয়েছে।"
                    else
                        "A 6-digit verification code has been dispatched to your registered admin Gmail ($maskedEmail).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 6-digit OTP Input Field
                OutlinedTextField(
                    value = enteredOtp,
                    onValueChange = { newValue ->
                        if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                            enteredOtp = newValue
                            errorMessage = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_otp_input"),
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 8.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    placeholder = {
                        Text(
                            text = "• • • • • •",
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberSecondary,
                        cursorColor = AmberSecondary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Countdown Timer & Resend Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    val timeStr = String.format("%02d:%02d", minutes, seconds)

                    Text(
                        text = if (remainingSeconds > 0)
                            "${if (language == Language.BENGALI) "মেয়াদ বাকি:" else "Expires in:"} $timeStr"
                        else
                            if (language == Language.BENGALI) "কোডের মেয়াদ শেষ" else "Code expired",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingSeconds > 30) EmeraldPrimary else MaterialTheme.colorScheme.error
                    )

                    TextButton(
                        onClick = {
                            enteredOtp = ""
                            errorMessage = null
                            onResendOtp()
                            remainingSeconds = AdminOtpService.getRemainingSeconds()
                        },
                        modifier = Modifier.testTag("resend_otp_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Resend",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == Language.BENGALI) "পুনরায় পাঠান" else "Resend Code",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Error Message
                AnimatedVisibility(visible = errorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Development / Emulator Quick Verification Assistance
                val activeTestOtp = AdminOtpService.getActiveOtpForTesting()
                if (activeTestOtp != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AmberSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (showTestCode) "নিরাপত্তা কোড: $activeTestOtp" else "টেস্ট মোড: কোড দেখুন",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            TextButton(
                                onClick = {
                                    if (!showTestCode) {
                                        showTestCode = true
                                        enteredOtp = activeTestOtp
                                    } else {
                                        showTestCode = false
                                    }
                                }
                            ) {
                                Text(
                                    text = if (showTestCode) "গোপন করুন" else "অটো-ফিল কোড",
                                    fontSize = 11.sp,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (enteredOtp.length != 6) {
                        errorMessage = if (language == Language.BENGALI)
                            "অনুগ্রহ করে ৬-সংখ্যার কোড সম্পূর্ণ লিখুন"
                        else
                            "Please enter the complete 6-digit code"
                        return@Button
                    }
                    isVerifying = true
                    val (success, msg) = AdminOtpService.verifyOtp(enteredOtp, targetEmail)
                    isVerifying = false
                    if (success) {
                        onVerifySuccess()
                    } else {
                        errorMessage = msg
                    }
                },
                enabled = enteredOtp.length == 6 && !isVerifying,
                colors = ButtonDefaults.buttonColors(containerColor = AmberSecondary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("verify_otp_confirm_btn")
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == Language.BENGALI) "যাচাই করুন" else "Verify",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = if (language == Language.BENGALI) "বাতিল" else "Cancel")
            }
        }
    )
}
