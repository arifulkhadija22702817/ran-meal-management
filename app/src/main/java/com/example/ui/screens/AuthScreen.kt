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
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.components.AdminPasswordRecoveryDialog
import com.example.ui.components.AdminPasswordVerificationDialog
import com.example.ui.components.InitialAdminClaimDialog
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
    onRecoverAdminPassword: (newPassword: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit = { _, s, _ -> s() },
    onChangePassword: (newPass: String) -> Unit = {},
    onResetPasswordWithEmail: (email: String, newPass: String) -> Boolean = { _, _ -> true },
    onMemberAuthSuccess: () -> Unit = {},
    onAdminAuthRequested: () -> Unit = {},
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
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showInitialAdminDialog by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var adminTargetEmail by remember { mutableStateOf("") }
    var signedInUserEmailForInitialClaim by remember { mutableStateOf("") }

    val managerEmail = mess?.managerEmail?.trim()?.lowercase() ?: ""
    val hasAdminConfigured = !mess?.managerEmail.isNullOrBlank() &&
            !mess?.adminPassword.isNullOrBlank() &&
            !mess?.managerEmail?.contains("placeholder")!!

    // Initial Admin Claim Dialog (When no admin exists yet in Firebase)
    if (showInitialAdminDialog && signedInUserEmailForInitialClaim.isNotBlank()) {
        InitialAdminClaimDialog(
            userEmail = signedInUserEmailForInitialClaim,
            language = language,
            onDismiss = {
                showInitialAdminDialog = false
                onMemberAuthSuccess()
            },
            onClaimAdmin = { initialPassword, onSuccess, onFailure ->
                onClaimInitialAdmin(initialPassword, onSuccess, onFailure)
            },
            onSuccess = {
                showInitialAdminDialog = false
                onAdminAuthSuccess()
            }
        )
    }

    // Admin Password Dialog (For verified admin Gmail)
    if (showPasswordDialog) {
        AdminPasswordVerificationDialog(
            targetEmail = adminTargetEmail,
            language = language,
            onDismiss = {
                showPasswordDialog = false
                // "onnothay user access pabe" -> If dismissed/cancelled, enter as regular member
                onMemberAuthSuccess()
            },
            onVerifyPassword = { password, onSuccess, onFailure ->
                onVerifyAdminPasswordAsync(password, onSuccess, onFailure)
            },
            onSuccess = {
                showPasswordDialog = false
                onAdminAuthSuccess()
            },
            onForgotPassword = {
                showPasswordDialog = false
                showRecoveryDialog = true
            }
        )
    }

    // Professional Admin Password Recovery Dialog (Forgot Password)
    if (showRecoveryDialog) {
        AdminPasswordRecoveryDialog(
            targetEmail = if (adminTargetEmail.isNotBlank()) adminTargetEmail else managerEmail,
            language = language,
            onDismiss = {
                showRecoveryDialog = false
                onMemberAuthSuccess()
            },
            onRequestOtp = { email, onSent ->
                onRequestAdminOtp(email, onSent)
            },
            onVerifyOtp = { code, email, onSuccess, onFailure ->
                onVerifyAdminOtp(code, email, onSuccess, onFailure)
            },
            onRecoverPassword = { newPassword, onSuccess, onFailure ->
                onRecoverAdminPassword(newPassword, onSuccess, onFailure)
            },
            onSuccess = {
                showRecoveryDialog = false
                onAdminAuthSuccess()
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
                    .size(92.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(EmeraldPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = "Meal Management Logo",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Meal Management",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (language == Language.BENGALI)
                    "মেস মিল ও হিসাব ব্যবস্থাপনা"
                else
                    "Mess Meal & Expense Management",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // PRIMARY SIGN-IN BUTTON: "গুগল দিয়ে প্রবেশ করুন" (For all users / members -> gets user access)
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
                                // "1st. sudhu login option thakbe gmail diye. tahole user access pabe."
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
                        .testTag("google_signin_btn"),
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
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = "Sign In",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == Language.BENGALI)
                                    "গুগল দিয়ে প্রবেশ করুন"
                                else
                                    "Sign In with Google",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (language == Language.BENGALI)
                                    "মেস মিল ও খরচের হিসাব দেখতে প্রবেশ করুন"
                                else
                                    "Sign in to view meals and expenses",
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
