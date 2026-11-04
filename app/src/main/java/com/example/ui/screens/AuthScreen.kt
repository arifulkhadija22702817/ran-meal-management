package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import com.example.ui.components.AdminOtpDialog
import com.example.ui.components.AdminPasswordVerificationDialog
import com.example.ui.components.InitialAdminClaimDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.Mess
import com.example.ui.Language
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.EmeraldPrimary
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    mess: Mess?,
    language: Language,
    onEnterAsGuest: () -> Unit = {},
    onVerifyAdminPassword: (password: String) -> Boolean = { true },
    onVerifyAdminPasswordAsync: (password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit = { _, s, _ -> s() },
    onClaimInitialAdmin: (initialPassword: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit = { _, s, _ -> s() },
    onChangePassword: (newPass: String) -> Unit = {},
    onResetPasswordWithEmail: (email: String, newPass: String) -> Boolean = { _, _ -> true },
    onMemberAuthSuccess: () -> Unit = {},
    onAdminAuthSuccess: () -> Unit = {},
    onRequestAdminOtp: (String, () -> Unit) -> Unit = { _, cb -> cb() },
    onVerifyAdminOtp: (String, String, () -> Unit, (String) -> Unit) -> Unit = { _, _, s, _ -> s() },
    onAuthSuccess: () -> Unit = onMemberAuthSuccess
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAdminOtpDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showInitialAdminClaimDialog by remember { mutableStateOf(false) }
    var adminTargetEmail by remember { mutableStateOf("") }
    var notManagerAlertEmail by remember { mutableStateOf<String?>(null) }

    // Initial Admin Setup Dialog (when no admin is registered yet in Firebase)
    if (showInitialAdminClaimDialog) {
        InitialAdminClaimDialog(
            userEmail = adminTargetEmail,
            language = language,
            onDismiss = {
                showInitialAdminClaimDialog = false
            },
            onClaimAdmin = { initialPassword, onSuccess, onFailure ->
                onClaimInitialAdmin(initialPassword, onSuccess, onFailure)
            },
            onSuccess = {
                showInitialAdminClaimDialog = false
                onAdminAuthSuccess()
            }
        )
    }

    // Tier 2: 2FA OTP Dialog (Step 2 of 3-Tier Security)
    if (showAdminOtpDialog) {
        AdminOtpDialog(
            targetEmail = adminTargetEmail,
            language = language,
            onDismiss = {
                showAdminOtpDialog = false
            },
            onVerifySuccess = {
                // Tier 2 (OTP) Confirmed! Move to Tier 3: Admin Secret Password
                showAdminOtpDialog = false
                showPasswordDialog = true
            },
            onResendOtp = {
                onRequestAdminOtp(adminTargetEmail) {}
            }
        )
    }

    // Tier 3: Admin Secret Password Dialog (Step 3 of 3-Tier Security)
    if (showPasswordDialog) {
        AdminPasswordVerificationDialog(
            targetEmail = adminTargetEmail,
            language = language,
            onDismiss = {
                showPasswordDialog = false
            },
            onVerifyPassword = { password, onSuccess, onFailure ->
                onVerifyAdminPasswordAsync(password, onSuccess, onFailure)
            },
            onSuccess = {
                showPasswordDialog = false
                onAdminAuthSuccess()
            }
        )
    }

    // If User signed in with an account that is not the registered manager
    if (notManagerAlertEmail != null) {
        AlertDialog(
            onDismissRequest = { notManagerAlertEmail = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (language == Language.BENGALI) "অ্যাডমিন অনুমতি নেই" else "Not Authorized as Admin",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == Language.BENGALI)
                        "আপনার সাইন-ইন করা জিমেইল ($notManagerAlertEmail) এই মেসের নিবন্ধিত অ্যাডমিনের (${mess?.managerEmail ?: "নির্ধারিত জিমেইল"}) সাথে মেলেনি।\n\nআপনি সাধারণ সদস্য হিসেবে প্রবেশ করে মিল, বাজার ও ব্যালেন্স দেখতে পারবেন।"
                    else
                        "Your signed-in Gmail ($notManagerAlertEmail) does not match the designated mess manager (${mess?.managerEmail ?: "admin"}). You may enter as a member with read-only access.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        notManagerAlertEmail = null
                        onMemberAuthSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(text = if (language == Language.BENGALI) "সদস্য হিসেবে প্রবেশ করুন" else "Enter as Member")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        notManagerAlertEmail = null
                        performSignOut(context, credentialManager, coroutineScope) {}
                    }
                ) {
                    Text(text = if (language == Language.BENGALI) "অন্য একাউন্ট দিয়ে চেষ্টা" else "Try Another Account")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo Icon
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(EmeraldPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = "Meal Management Logo",
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Meal Management",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = EmeraldPrimary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = if (language == Language.BENGALI)
                        "মেস মিল ও বাজার ব্যবস্থাপনা • নিরাপদ ক্লাউড এক্সেস"
                    else
                        "Mess Meal & Expense Management • Secure Cloud Access",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Dedicated Admin / Manager Portal Login Card
                Card(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        performGoogleSignIn(
                            context = context,
                            credentialManager = credentialManager,
                            coroutineScope = coroutineScope,
                            onSuccess = {
                                isLoading = false
                                val user = Firebase.auth.currentUser
                                val userEmail = user?.email?.trim()?.lowercase() ?: ""
                                val managerEmail = mess?.managerEmail?.trim()?.lowercase() ?: ""

                                val isManagerUnassigned = managerEmail.isBlank() ||
                                        managerEmail == "manager_placeholder" ||
                                        managerEmail.contains("placeholder")

                                if (isManagerUnassigned) {
                                    // No registered manager yet! Initiate Initial Admin Claim Flow
                                    adminTargetEmail = userEmail
                                    showInitialAdminClaimDialog = true
                                } else if (userEmail.isNotBlank() && userEmail.equals(managerEmail, ignoreCase = true)) {
                                    // Step 1 Passed: Gmail matches registered manager -> Initiate Step 2: 2FA OTP
                                    adminTargetEmail = userEmail
                                    onRequestAdminOtp(userEmail) {
                                        showAdminOtpDialog = true
                                    }
                                } else {
                                    // Step 1 Failed: Not the registered manager
                                    notManagerAlertEmail = userEmail.ifBlank { "unknown" }
                                }
                            },
                            onError = { err ->
                                isLoading = false
                                errorMessage = err
                            },
                            onCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_login_portal_btn"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AmberSecondary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
                    enabled = !isLoading
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Login",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (language == Language.BENGALI)
                                        "মেস অ্যাডমিন / ম্যানেজার লগইন"
                                    else
                                        "Mess Admin / Manager Login",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (language == Language.BENGALI)
                                    "৩-ধাপের নিশ্ছিদ্র নিরাপত্তা: ১) জিমেইল ২) ওটিপি ৩) পাসওয়ার্ড"
                                else
                                    "3-Tier Security: 1) Gmail 2) 2FA OTP 3) Admin Password",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.95f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 2. Member Sign-In Card
                Card(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        performGoogleSignIn(
                            context = context,
                            credentialManager = credentialManager,
                            coroutineScope = coroutineScope,
                            onSuccess = {
                                isLoading = false
                                onMemberAuthSuccess()
                            },
                            onError = { err ->
                                isLoading = false
                                errorMessage = err
                            },
                            onCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_login_portal_btn"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = EmeraldPrimary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
                    enabled = !isLoading
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Member Login",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == Language.BENGALI)
                                    "সদস্য হিসেবে প্রবেশ করুন"
                                else
                                    "Enter as Mess Member",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (language == Language.BENGALI)
                                    "যেকোনো জিমেইল দিয়ে রিয়েলটাইম মিল, বাজার ও ব্যালেন্স দর্শন"
                                else
                                    "Real-time read-only access to meals, bazaar and ledger",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Security & Governance Info Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = AmberSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == Language.BENGALI) "টু-ফ্যাক্টর নিরাপত্তা নীতি (2FA)" else "2-Factor Security Policy",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (language == Language.BENGALI)
                                "• মেস ম্যানেজার (${mess?.managerEmail ?: "নির্ধারিত জিমেইল"}): জিমেইল লগইন এবং ৬-সংখ্যার ওটিপি কোড মেলানোর পর পূর্ণ অ্যাডমিন নিয়ন্ত্রণ সক্রিয় হবে।\n" +
                                "• মেস সদস্য: যেকোনো জিমেইল দিয়ে সাইন-ইন করে রিয়েলটাইমে সকল মিল, বাজার ও ব্যালেন্স রিড-অনলি দেখতে পারবেন।\n" +
                                "• সম্পূর্ণ ফায়ারবেস ক্লাউড সিকিউরিটি রুলস দ্বারা সুরক্ষিত; কোনো পাসওয়ার্ড ফাঁসের ভয় নেই।"
                            else
                                "• Mess Manager (${mess?.managerEmail ?: "designated email"}): Full admin authority unlocked only after Google Sign-In and 6-digit email OTP verification.\n" +
                                "• Mess Members: Signed-in members receive authenticated real-time read-only access.\n" +
                                "• End-to-end zero-trust architecture backed by Firebase security rules.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

fun performGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    coroutineScope: CoroutineScope,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onError("Google Sign-In configuration missing: default_web_client_id")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    coroutineScope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onSuccess()
            } else {
                onError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled: ${e.message}")
            onCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onError(e.localizedMessage ?: "Sign-in failed")
        }
    }
}

fun performSignOut(
    context: Context,
    credentialManager: CredentialManager,
    coroutineScope: CoroutineScope,
    onSignedOut: () -> Unit
) {
    Firebase.auth.signOut()
    coroutineScope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Clear credentials failed", e)
        } finally {
            onSignedOut()
        }
    }
}
