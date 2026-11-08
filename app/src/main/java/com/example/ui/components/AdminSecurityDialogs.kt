package com.example.ui.components

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.Language
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.util.AdminPasswordValidator

/**
 * Live Password Criteria Checklist:
 * Visual indicators showing compliance with:
 * - 8 to 12 characters
 * - At least one letter (a-zA-Z)
 * - At least one number (0-9)
 * - At least one special character (!@#$%^&*...)
 */
@Composable
fun PasswordRequirementChecklist(
    password: String,
    language: Language
) {
    val isLengthOk = AdminPasswordValidator.isLengthValid(password)
    val hasLetter = AdminPasswordValidator.hasLetter(password)
    val hasDigit = AdminPasswordValidator.hasDigit(password)
    val hasSpecial = AdminPasswordValidator.hasSpecial(password)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (language == Language.BENGALI) "পাসওয়ার্ডের শর্তাবলী (বাধ্যতামূলক):" else "Password Requirements (Mandatory):",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CriterionItem(label = if (language == Language.BENGALI) "৮-১২ অক্ষর" else "8-12 chars", isValid = isLengthOk)
                CriterionItem(label = if (language == Language.BENGALI) "বর্ণ (A-Z)" else "Letter (A-Z)", isValid = hasLetter)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CriterionItem(label = if (language == Language.BENGALI) "সংখ্যা (0-9)" else "Number (0-9)", isValid = hasDigit)
                CriterionItem(label = if (language == Language.BENGALI) "চিহ্ন (!@#$)" else "Special (!@#$)", isValid = hasSpecial)
            }
        }
    }
}

@Composable
private fun CriterionItem(label: String, isValid: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isValid) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isValid) EmeraldPrimary else MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isValid) FontWeight.Bold else FontWeight.Normal,
            color = if (isValid) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AdminDirectLoginDialog(
    initialEmail: String = "",
    currentConfiguredEmail: String = "",
    language: Language,
    onDismiss: () -> Unit,
    onVerifyAdminCredentials: (email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit,
    onSuccess: () -> Unit,
    onForgotPassword: (() -> Unit)? = null
) {
    var emailInput by remember { mutableStateOf(initialEmail.ifBlank { currentConfiguredEmail }) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AmberSecondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = AmberSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (language == Language.BENGALI) "অ্যাডমিন লগইন" else "Admin Login",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = if (language == Language.BENGALI) "জিমেইল ও পাসওয়ার্ড যাচাইকরণ" else "Gmail & Password Verification",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (language == Language.BENGALI)
                        "মেস অ্যাডমিন অ্যাক্সেস পেতে আপনার অনুমোদিত জিমেইল ও পাসওয়ার্ড দিন:"
                    else
                        "Enter your authorized Admin Gmail and secret password to unlock privileges:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = {
                        emailInput = it
                        errorMessage = null
                    },
                    label = { Text(if (language == Language.BENGALI) "অ্যাডমিন জিমেইল" else "Admin Gmail") },
                    placeholder = { Text("arifulislamsaju.sdp@gmail.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_email_input")
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        errorMessage = null
                    },
                    label = { Text(if (language == Language.BENGALI) "অ্যাডমিন পাসওয়ার্ড" else "Admin Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_password_input")
                )

                if (onForgotPassword != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onForgotPassword,
                            enabled = !isVerifying
                        ) {
                            Text(
                                text = if (language == Language.BENGALI) "🔑 পাসওয়ার্ড ভুলে গেছেন? (রিকভারি)" else "Forgot Password?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberSecondary
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanEmail = emailInput.trim().lowercase()
                    val cleanPass = passwordInput.trim()
                    if (cleanEmail.isBlank()) {
                        errorMessage = if (language == Language.BENGALI) "জিমেইল এড্রেস লিখুন" else "Please enter Gmail address"
                        return@Button
                    }
                    if (cleanPass.isBlank()) {
                        errorMessage = if (language == Language.BENGALI) "পাসওয়ার্ড লিখুন" else "Please enter password"
                        return@Button
                    }
                    isVerifying = true
                    onVerifyAdminCredentials(
                        cleanEmail,
                        cleanPass,
                        {
                            isVerifying = false
                            onSuccess()
                        },
                        { err ->
                            isVerifying = false
                            errorMessage = err
                        }
                    )
                },
                enabled = emailInput.isNotBlank() && passwordInput.isNotBlank() && !isVerifying,
                colors = ButtonDefaults.buttonColors(containerColor = AmberSecondary),
                modifier = Modifier.testTag("admin_direct_login_btn")
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language == Language.BENGALI) "লগইন করুন" else "Unlock Admin")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isVerifying) {
                Text(if (language == Language.BENGALI) "বাতিল" else "Cancel")
            }
        }
    )
}

/**
 * Admin Password Verification Dialog:
 * Checks whether the registered admin Gmail + secret password match.
 * If both match, grants full admin access.
 */
@Composable
fun AdminPasswordVerificationDialog(
    targetEmail: String,
    language: Language,
    onDismiss: () -> Unit,
    onVerifyPassword: (password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit,
    onSuccess: () -> Unit,
    onForgotPassword: (() -> Unit)? = null
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AmberSecondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = AmberSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (language == Language.BENGALI) "অ্যাডমিন পাসওয়ার্ড যাচাই" else "Admin Password Check",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = if (language == Language.BENGALI) "নিরাপদ জিমেইল ও পাসওয়ার্ড অনুমোদন" else "Secure Gmail & Password Match",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Gmail match confirmation pill
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
                                text = if (language == Language.BENGALI) "মেস অ্যাডমিন জিমেইল:" else "Admin Gmail:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = targetEmail,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Text(
                    text = if (language == Language.BENGALI)
                        "পূর্ণ অ্যাডমিন এক্সেস পেতে আপনার গোপন পাসওয়ার্ডটি লিখুন:"
                    else
                        "Enter your secret password to unlock mess admin privileges:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text(if (language == Language.BENGALI) "অ্যাডমিন পাসওয়ার্ড" else "Admin Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_password_input")
                )

                if (onForgotPassword != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onForgotPassword,
                            enabled = !isVerifying
                        ) {
                            Text(
                                text = if (language == Language.BENGALI) "🔑 পাসওয়ার্ড ভুলে গেছেন? (রিকভারি)" else "Forgot Password?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberSecondary
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = password.trim()
                    if (clean.isBlank()) {
                        errorMessage = if (language == Language.BENGALI) "পাসওয়ার্ড লিখুন" else "Please enter password"
                        return@Button
                    }
                    isVerifying = true
                    onVerifyPassword(
                        clean,
                        {
                            isVerifying = false
                            onSuccess()
                        },
                        { err ->
                            isVerifying = false
                            errorMessage = err
                        }
                    )
                },
                enabled = password.isNotBlank() && !isVerifying,
                colors = ButtonDefaults.buttonColors(containerColor = AmberSecondary),
                modifier = Modifier.testTag("admin_password_confirm_btn")
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language == Language.BENGALI) "লগইন সম্পন্ন করুন" else "Unlock Admin")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isVerifying) {
                Text(if (language == Language.BENGALI) "সাধারণ ইউজার হিসেবে প্রবেশ করুন" else "Continue as Member")
            }
        }
    )
}

/**
 * Professional Admin Password Recovery Dialog:
 * Allows verified admin Gmail to receive a 6-digit OTP code,
 * verify it, and reset their Admin Password in Firebase Firestore.
 */
@Composable
fun AdminPasswordRecoveryDialog(
    targetEmail: String,
    language: Language,
    onDismiss: () -> Unit,
    onRequestOtp: (email: String, onSent: () -> Unit) -> Unit,
    onVerifyOtp: (enteredCode: String, email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit,
    onRecoverPassword: (newPassword: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit,
    onSuccess: () -> Unit
) {
    var otpCode by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var isOtpVerified by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AmberSecondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AmberSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (language == Language.BENGALI) "অ্যাডমিন পাসওয়ার্ড রিকভারি" else "Password Recovery",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = if (language == Language.BENGALI) "সিকিউরিটি ওটিপি ও নতুন পাসওয়ার্ড" else "Security OTP & Password Reset",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (language == Language.BENGALI) "অ্যাডমিন জিমেইল:" else "Admin Gmail:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = targetEmail,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!isOtpVerified) {
                    // Step 1: Request & Verify 6-digit OTP
                    Text(
                        text = if (language == Language.BENGALI)
                            "পাসওয়ার্ড রিকভার করতে আপনার জিমেইলে পাঠানো ৬-সংখ্যার সিকিউরিটি কোড দিন:"
                        else
                            "Enter the 6-digit security code sent to your Gmail:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = {
                                if (it.length <= 6) {
                                    otpCode = it
                                    errorMessage = null
                                }
                            },
                            label = { Text(if (language == Language.BENGALI) "৬-ডিজিট কোড" else "6-digit OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onRequestOtp(targetEmail) {
                                    isOtpSent = true
                                    errorMessage = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text(if (!isOtpSent) (if (language == Language.BENGALI) "কোড পাঠান" else "Send") else (if (language == Language.BENGALI) "পুনরায় পাঠান" else "Resend"))
                        }
                    }

                    if (isOtpSent) {
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (language == Language.BENGALI)
                                            "✓ সিকিউরিটি কোড পাঠানো হয়েছে!"
                                        else
                                            "✓ Security Code Sent!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = EmeraldPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (language == Language.BENGALI)
                                            "আপনার অ্যাডমিন জিমেইলে ($targetEmail) ৬-সংখ্যার সিকিউরিটি কোড পাঠানো হয়েছে। অনুগ্রহ করে আপনার জিমেইল ইনবক্স বা স্প্যাম ফোল্ডার চেক করুন এবং কোডটি নিচে লিখে 'কোড যাচাই করুন' বাটনে চাপ দিন।"
                                        else
                                            "A 6-digit security code has been sent to your Admin Gmail ($targetEmail). Please check your Inbox or Spam folder and enter the code below.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (otpCode.length != 6) {
                                errorMessage = if (language == Language.BENGALI) "৬-সংখ্যার কোড দিন" else "Enter 6-digit code"
                                return@Button
                            }
                            isSubmitting = true
                            onVerifyOtp(
                                otpCode,
                                targetEmail,
                                {
                                    isSubmitting = false
                                    isOtpVerified = true
                                    errorMessage = null
                                },
                                { err ->
                                    isSubmitting = false
                                    errorMessage = err
                                }
                            )
                        },
                        enabled = otpCode.length == 6 && !isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberSecondary)
                    ) {
                        Text(if (language == Language.BENGALI) "কোড যাচাই করুন" else "Verify Code")
                    }
                } else {
                    // Step 2: Set New Password
                    Text(
                        text = if (language == Language.BENGALI)
                            "কোড যাচাই সফল! এবার আপনার মেসের জন্য নতুন ৮-১২ অক্ষরের পাসওয়ার্ড নির্ধারণ করুন:"
                        else
                            "Code verified! Set your new 8-12 character admin password:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldPrimary
                    )

                    PasswordRequirementChecklist(password = newPassword, language = language)

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            errorMessage = null
                        },
                        label = { Text(if (language == Language.BENGALI) "নতুন অ্যাডমিন পাসওয়ার্ড (৮-১২ অক্ষর)" else "New Admin Password (8-12 chars)") },
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
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = null
                        },
                        label = { Text(if (language == Language.BENGALI) "পাসওয়ার্ড নিশ্চিত করুন" else "Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            if (isOtpVerified) {
                Button(
                    onClick = {
                        val p1 = newPassword.trim()
                        val p2 = confirmPassword.trim()
                        val (valid, error) = AdminPasswordValidator.validate(p1)
                        if (!valid) {
                            errorMessage = error
                            return@Button
                        }
                        if (p1 != p2) {
                            errorMessage = if (language == Language.BENGALI) "উভয় পাসওয়ার্ড মিলছে না!" else "Passwords do not match!"
                            return@Button
                        }
                        isSubmitting = true
                        onRecoverPassword(
                            p1,
                            {
                                isSubmitting = false
                                onSuccess()
                            },
                            { err ->
                                isSubmitting = false
                                errorMessage = err
                            }
                        )
                    },
                    enabled = newPassword.isNotBlank() && confirmPassword.isNotBlank() && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberSecondary)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(if (language == Language.BENGALI) "পাসওয়ার্ড আপডেট ও মেস আনলক" else "Update Password & Unlock")
                    }
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(if (language == Language.BENGALI) "বাতিল" else "Cancel")
            }
        }
    )
}

/**
 * Initial Admin Claim Dialog:
 * When no admin/manager Gmail is registered in Firebase, this allows the user to
 * designate their signed-in Gmail as the initial Admin and create their initial Admin Password
 * (enforcing 8-12 characters with letter + number + special character).
 */
@Composable
fun InitialAdminClaimDialog(
    userEmail: String,
    language: Language,
    onDismiss: () -> Unit,
    onClaimAdmin: (initialPassword: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit,
    onSuccess: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (language == Language.BENGALI) "মেসের প্রথম অ্যাডমিন তৈরি" else "Create Initial Mess Admin",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = if (language == Language.BENGALI) "জিমেইল ও পাসওয়ার্ড নির্ধারণ" else "Assign Gmail & Password",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (language == Language.BENGALI)
                            "এই মেসে কোনো অ্যাডমিন নির্ধারিত নেই। আপনার জিমেইল ($userEmail)-কে মেসের একমাত্র অ্যাডমিন করতে একটি নিরাপদ পাসওয়ার্ড তৈরি করুন।"
                        else
                            "No admin is assigned yet. Set a password to register your Gmail ($userEmail) as the mess manager.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                PasswordRequirementChecklist(password = password, language = language)

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text(if (language == Language.BENGALI) "নতুন অ্যাডমিন পাসওয়ার্ড (৮-১২ অক্ষর)" else "Admin Password (8-12 chars)") },
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
                        .testTag("initial_admin_password_input")
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text(if (language == Language.BENGALI) "পাসওয়ার্ড নিশ্চিত করুন" else "Confirm Password") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("initial_admin_confirm_password_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p1 = password.trim()
                    val p2 = confirmPassword.trim()
                    val (valid, error) = AdminPasswordValidator.validate(p1)
                    if (!valid) {
                        errorMessage = error
                        return@Button
                    }
                    if (p1 != p2) {
                        errorMessage = if (language == Language.BENGALI) "উভয় পাসওয়ার্ড মিলছে না!" else "Passwords do not match!"
                        return@Button
                    }
                    isSubmitting = true
                    onClaimAdmin(
                        p1,
                        {
                            isSubmitting = false
                            onSuccess()
                        },
                        { err ->
                            isSubmitting = false
                            errorMessage = err
                        }
                    )
                },
                enabled = password.isNotBlank() && confirmPassword.isNotBlank() && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("initial_admin_submit_btn")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language == Language.BENGALI) "অ্যাডমিন নিশ্চিত করুন" else "Set as Admin")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(if (language == Language.BENGALI) "বাতিল" else "Cancel")
            }
        }
    )
}

/**
 * Change Admin Password Dialog:
 * Allows the active admin to update their secret password.
 */
@Composable
fun ChangeAdminPasswordDialog(
    hasExistingPassword: Boolean,
    language: Language,
    onDismiss: () -> Unit,
    onChangePassword: (oldPass: String?, newPass: String, onDone: (Boolean, String) -> Unit) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null, tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == Language.BENGALI) "অ্যাডমিন পাসওয়ার্ড পরিবর্তন / আপডেট" else "Change Admin Password",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hasExistingPassword) {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it; errorMessage = null },
                        label = { Text(if (language == Language.BENGALI) "বর্তমান পাসওয়ার্ড" else "Current Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PasswordRequirementChecklist(password = newPassword, language = language)

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMessage = null },
                    label = { Text(if (language == Language.BENGALI) "নতুন পাসওয়ার্ড (৮-১২ অক্ষর)" else "New Password (8-12 chars)") },
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
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text(if (language == Language.BENGALI) "নতুন পাসওয়ার্ড নিশ্চিত করুন" else "Confirm New Password") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val n1 = newPassword.trim()
                    val n2 = confirmPassword.trim()
                    val (valid, error) = AdminPasswordValidator.validate(n1)
                    if (!valid) {
                        errorMessage = error
                        return@Button
                    }
                    if (n1 != n2) {
                        errorMessage = if (language == Language.BENGALI) "নতুন পাসওয়ার্ড মিলছে না!" else "Passwords do not match!"
                        return@Button
                    }
                    if (hasExistingPassword && oldPassword.isBlank()) {
                        errorMessage = if (language == Language.BENGALI) "বর্তমান পাসওয়ার্ড লিখুন" else "Enter current password"
                        return@Button
                    }
                    isSubmitting = true
                    val oldPassParam = if (hasExistingPassword) oldPassword.trim() else null
                    onChangePassword(oldPassParam, n1) { success, msg ->
                        isSubmitting = false
                        if (success) {
                            onDismiss()
                        } else {
                            errorMessage = msg
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(if (language == Language.BENGALI) "সংরক্ষণ করুন" else "Save Password")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(if (language == Language.BENGALI) "বাতিল" else "Cancel")
            }
        }
    )
}
