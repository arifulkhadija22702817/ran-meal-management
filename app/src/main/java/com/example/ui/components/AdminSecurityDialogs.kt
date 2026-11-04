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
import androidx.compose.material.icons.filled.CheckCircle
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

/**
 * Tier 3 of 3-Tier Security Authentication:
 * Step 1 (Gmail matched) -> Step 2 (OTP code matched) -> Step 3 (Admin Secret Password).
 * If all 3 match, grants full admin access.
 */
@Composable
fun AdminPasswordVerificationDialog(
    targetEmail: String,
    language: Language,
    onDismiss: () -> Unit,
    onVerifyPassword: (password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit,
    onSuccess: () -> Unit
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
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AmberSecondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = AmberSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (language == Language.BENGALI) "৩য় ধাপ: অ্যাডমিন পাসওয়ার্ড" else "Tier 3: Admin Password",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = if (language == Language.BENGALI) "চূড়ান্ত নিরাপত্তা অনুমোদন" else "Final Security Verification",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Tier 1 & 2 Completed Indicator
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == Language.BENGALI) "১ম ধাপ: জিমেইল যাচাইকরণ সফল ($targetEmail)" else "Step 1: Gmail Match Confirmed ($targetEmail)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == Language.BENGALI) "২য় ধাপ: ওটিপি কোড যাচাইকরণ সফল" else "Step 2: 2FA OTP Verified Successfully",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }

                Text(
                    text = if (language == Language.BENGALI)
                        "পূর্ণ অ্যাডমিন এক্সেস পেতে মেসের গোপন অ্যাডমিন পাসওয়ার্ডটি লিখুন:"
                    else
                        "Enter the secret mess admin password to unlock full management rights:",
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
                    Text(if (language == Language.BENGALI) "লগইন সম্পন্ন করুন" else "Complete Login")
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
 * Initial Admin Claim Dialog:
 * When no admin/manager Gmail is registered in Firebase, this allows the user to
 * designate their signed-in Gmail as the initial Admin and create their initial Admin Password.
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
                        text = if (language == Language.BENGALI) "মেসের প্রথম অ্যাডমিন রেজিস্ট্রেশন" else "Initial Mess Admin Setup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = if (language == Language.BENGALI) "অ্যাডমিন একাউন্ট ও পাসওয়ার্ড তৈরি" else "Create Admin Account & Password",
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
                            "এই মেসে বর্তমানে কোনো অ্যাডমিন বা ম্যানেজার নির্ধারিত নেই। আপনার জিমেইল ($userEmail)-কে মেসের প্রধান অ্যাডমিন হিসেবে রেজিস্টার করতে একটি নিরাপদ পাসওয়ার্ড তৈরি করুন।"
                        else
                            "No manager is currently assigned to this mess. Set up an admin password to register your Gmail ($userEmail) as the mess manager.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
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
                    if (p1.length < 4) {
                        errorMessage = if (language == Language.BENGALI) "পাসওয়ার্ড কমপক্ষে ৪ অক্ষরের হতে হবে" else "Password must be at least 4 chars"
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
                modifier = Modifier.testTag("claim_initial_admin_btn")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language == Language.BENGALI) "অ্যাডমিন হিসেবে নিশ্চিত করুন" else "Confirm as Admin")
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
 * Change / Update Admin Password Dialog:
 * Accessible from Admin Settings / Governance card.
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

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMessage = null },
                    label = { Text(if (language == Language.BENGALI) "নতুন পাসওয়ার্ড (কমপক্ষে ৪ অক্ষর)" else "New Password (min 4 chars)") },
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
                    if (hasExistingPassword && oldPassword.isBlank()) {
                        errorMessage = if (language == Language.BENGALI) "বর্তমান পাসওয়ার্ড লিখুন" else "Enter current password"
                        return@Button
                    }
                    if (n1.length < 4) {
                        errorMessage = if (language == Language.BENGALI) "নতুন পাসওয়ার্ড কমপক্ষে ৪ অক্ষরের হতে হবে" else "New password must be at least 4 chars"
                        return@Button
                    }
                    if (n1 != n2) {
                        errorMessage = if (language == Language.BENGALI) "উভয় পাসওয়ার্ড মিলছে না!" else "Passwords do not match!"
                        return@Button
                    }
                    isSubmitting = true
                    onChangePassword(if (hasExistingPassword) oldPassword.trim() else null, n1) { success, msg ->
                        isSubmitting = false
                        if (success) {
                            onDismiss()
                        } else {
                            errorMessage = msg
                        }
                    }
                },
                enabled = !isSubmitting && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(if (language == Language.BENGALI) "পাসওয়ার্ড আপডেট করুন" else "Update Password")
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
