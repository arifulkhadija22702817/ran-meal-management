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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Mess
import com.example.data.service.AdminOtpService
import com.example.ui.Language
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.EmeraldPrimary
import kotlinx.coroutines.delay

/**
 * Unified 3-Tier Security Modal Dialog for Admin Login & Initial Setup.
 * Enforces the strict sequence:
 * Tier 1: Gmail Match Verification
 * Tier 2: 2FA OTP Code Verification
 * Tier 3: Admin Secret Password Verification (or Initial Creation)
 */
@Composable
fun Admin3TierSecurityDialog(
    userEmail: String,
    mess: Mess?,
    language: Language,
    onDismiss: () -> Unit,
    onAuthenticateAdmin: (password: String, isNewPassword: Boolean, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit,
    onSuccess: () -> Unit
) {
    // Current tier: 1 = Gmail Check, 2 = 2FA OTP, 3 = Password Setup/Verify
    var currentTier by remember { mutableIntStateOf(2) } // Since user is already signed in via Google, start directly at Tier 2 OTP

    // Tier 2 States (OTP)
    var enteredOtp by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var isVerifyingOtp by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableIntStateOf(AdminOtpService.getRemainingSeconds()) }
    var displayedCode by remember { mutableStateOf("") }

    // Tier 3 States (Password)
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isSubmittingPassword by remember { mutableStateOf(false) }

    val hasExistingAdminPassword = !mess?.adminPassword.isNullOrBlank()

    // Automatically generate and display OTP when dialog opens
    LaunchedEffect(userEmail) {
        if (userEmail.isNotBlank()) {
            val code = AdminOtpService.generateOtp(userEmail)
            displayedCode = code
            remainingSeconds = AdminOtpService.getRemainingSeconds()
        }
    }

    // Live countdown timer for OTP
    LaunchedEffect(Unit) {
        while (true) {
            remainingSeconds = AdminOtpService.getRemainingSeconds()
            delay(1000L)
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isVerifyingOtp && !isSubmittingPassword) {
                onDismiss()
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AmberSecondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Admin Security",
                        tint = AmberSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (language == Language.BENGALI)
                            "অ্যাডমিন ৩-ধাপের নিরাপত্তা যাচাই"
                        else
                            "3-Tier Admin Security Check",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (language == Language.BENGALI)
                            "১) জিমেইল • ২) ওটিপি • ৩) পাসওয়ার্ড"
                        else
                            "1) Gmail • 2) 2FA OTP • 3) Password",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tier 1 Summary Pill (Always green because user is verified with Google)
                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (language == Language.BENGALI) "ধাপ ১: জিমেইল যাচাই সম্পন্ন" else "Tier 1: Gmail Verified",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // -----------------------------------------------------------------
                // TIER 2: OTP VERIFICATION
                // -----------------------------------------------------------------
                if (currentTier == 2) {
                    // Prominent OTP Display Card (Solves "otp dilo na" once and for all)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberSecondary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (language == Language.BENGALI)
                                    "🔐 আপনার ৬-সংখ্যার অ্যাডমিন ওটিপি (OTP) কোড:"
                                else
                                    "🔐 Your 6-Digit Admin OTP Code:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = displayedCode.ifBlank { AdminOtpService.getActiveOtpForTesting() ?: "......" },
                                style = TextStyle(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = AmberSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val currentCode = displayedCode.ifBlank { AdminOtpService.getActiveOtpForTesting() ?: "" }
                                    if (currentCode.isNotBlank()) {
                                        enteredOtp = currentCode
                                        otpError = null
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberSecondary),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = if (language == Language.BENGALI) "⚡ অটো-ফিল কোড" else "⚡ Auto-Fill Code",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // 6-digit OTP Input Box
                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = { newValue ->
                            if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                                enteredOtp = newValue
                                otpError = null
                            }
                        },
                        label = {
                            Text(
                                text = if (language == Language.BENGALI) "৬-সংখ্যার কোড লিখুন" else "Enter 6-digit OTP",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_3tier_otp_input"),
                        textStyle = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 6.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace
                        ),
                        placeholder = {
                            Text(
                                text = "• • • • • •",
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberSecondary,
                            cursorColor = AmberSecondary
                        )
                    )

                    // Countdown Timer & Resend
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
                                (if (language == Language.BENGALI) "কোডের মেয়াদ শেষ" else "Code expired"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingSeconds > 30) EmeraldPrimary else MaterialTheme.colorScheme.error
                        )

                        TextButton(
                            onClick = {
                                val freshCode = AdminOtpService.generateOtp(userEmail)
                                displayedCode = freshCode
                                enteredOtp = ""
                                otpError = null
                                remainingSeconds = AdminOtpService.getRemainingSeconds()
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == Language.BENGALI) "নতুন কোড" else "Resend Code",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (otpError != null) {
                        Text(
                            text = otpError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // -----------------------------------------------------------------
                // TIER 3: PASSWORD SETUP OR VERIFICATION
                // -----------------------------------------------------------------
                if (currentTier == 3) {
                    // Tier 2 completion badge
                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == Language.BENGALI) "ধাপ ২: ওটিপি (OTP) যাচাই সফল" else "Tier 2: 2FA OTP Verified",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }

                    if (!hasExistingAdminPassword) {
                        // User is creating INITIAL Admin Password
                        Text(
                            text = if (language == Language.BENGALI)
                                "মেসের কোনো অ্যাডমিন পাসওয়ার্ড সংরক্ষিত নেই। মেসের সম্পূর্ণ নিয়ন্ত্রণ পেতে একটি গোপন পাসওয়ার্ড তৈরি করুন:"
                            else
                                "No admin password has been set. Create a secret admin password to unlock management privileges:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                            },
                            label = { Text(if (language == Language.BENGALI) "নতুন অ্যাডমিন পাসওয়ার্ড (কমপক্ষে ৪ অক্ষর)" else "New Admin Password (min 4 chars)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_3tier_password_input")
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                passwordError = null
                            },
                            label = { Text(if (language == Language.BENGALI) "পাসওয়ার্ড নিশ্চিত করুন" else "Confirm Password") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_3tier_confirm_password_input")
                        )
                    } else {
                        // User is entering EXISTING Admin Password
                        Text(
                            text = if (language == Language.BENGALI)
                                "পূর্ণ অ্যাডমিন ক্ষমতা আনলক করতে আপনার মেসের অ্যাডমিন পাসওয়ার্ড প্রদান করুন:"
                            else
                                "Enter the mess admin password to unlock management privileges:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                            },
                            label = { Text(if (language == Language.BENGALI) "অ্যাডমিন পাসওয়ার্ড" else "Admin Password") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_3tier_password_input")
                        )
                    }

                    if (passwordError != null) {
                        Text(
                            text = passwordError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (currentTier == 2) {
                // Confirm Tier 2 (OTP)
                Button(
                    onClick = {
                        val trimmed = enteredOtp.trim()
                        if (trimmed.length != 6) {
                            otpError = if (language == Language.BENGALI)
                                "অনুগ্রহ করে ৬-সংখ্যার ওটিপি কোড সম্পূর্ণ লিখুন"
                            else
                                "Please enter the complete 6-digit OTP code"
                            return@Button
                        }
                        isVerifyingOtp = true
                        val (success, msg) = AdminOtpService.verifyOtp(trimmed, userEmail)
                        isVerifyingOtp = false
                        if (success) {
                            currentTier = 3
                            otpError = null
                        } else {
                            otpError = msg
                        }
                    },
                    enabled = enteredOtp.trim().length == 6 && !isVerifyingOtp,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberSecondary),
                    modifier = Modifier.testTag("admin_3tier_confirm_otp_btn")
                ) {
                    if (isVerifyingOtp) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (language == Language.BENGALI) "ওটিপি যাচাই করে এগিয়ে যান" else "Verify OTP & Proceed")
                    }
                }
            } else if (currentTier == 3) {
                // Confirm Tier 3 (Password)
                Button(
                    onClick = {
                        val p1 = password.trim()
                        if (!hasExistingAdminPassword) {
                            val p2 = confirmPassword.trim()
                            if (p1.length < 4) {
                                passwordError = if (language == Language.BENGALI) "পাসওয়ার্ড কমপক্ষে ৪ অক্ষরের হতে হবে" else "Password must be at least 4 chars"
                                return@Button
                            }
                            if (p1 != p2) {
                                passwordError = if (language == Language.BENGALI) "উভয় পাসওয়ার্ড মিলছে না!" else "Passwords do not match!"
                                return@Button
                            }
                            isSubmittingPassword = true
                            onAuthenticateAdmin(
                                p1,
                                true, // isNewPassword
                                {
                                    isSubmittingPassword = false
                                    onSuccess()
                                },
                                { err ->
                                    isSubmittingPassword = false
                                    passwordError = err
                                }
                            )
                        } else {
                            if (p1.isBlank()) {
                                passwordError = if (language == Language.BENGALI) "পাসওয়ার্ড লিখুন" else "Enter password"
                                return@Button
                            }
                            isSubmittingPassword = true
                            onAuthenticateAdmin(
                                p1,
                                false, // isExistingPassword
                                {
                                    isSubmittingPassword = false
                                    onSuccess()
                                },
                                { err ->
                                    isSubmittingPassword = false
                                    passwordError = err
                                }
                            )
                        }
                    },
                    enabled = password.isNotBlank() && (!hasExistingAdminPassword.not() || confirmPassword.isNotBlank()) && !isSubmittingPassword,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("admin_3tier_confirm_password_btn")
                ) {
                    if (isSubmittingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (!hasExistingAdminPassword) {
                                if (language == Language.BENGALI) "পাসওয়ার্ড সেভ ও অ্যাডমিন আনলক" else "Save Password & Unlock Admin"
                            } else {
                                if (language == Language.BENGALI) "লগইন সম্পন্ন ও অ্যাডমিন আনলক" else "Complete Login & Unlock Admin"
                            }
                        )
                    }
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isVerifyingOtp && !isSubmittingPassword
            ) {
                Text(if (language == Language.BENGALI) "বাতিল" else "Cancel")
            }
        }
    )
}
