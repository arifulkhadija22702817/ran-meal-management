package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.components.AdminOtpDialog
import com.example.ui.components.AdminPasswordVerificationDialog
import com.example.ui.components.InitialAdminClaimDialog
import com.example.ui.theme.AmberSecondary
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.data.repository.MessRepository
import com.example.ui.Language
import com.example.ui.MessViewModel
import com.example.ui.components.MessTopBar
import com.example.ui.components.NavigationTabRow
import com.example.ui.screens.AdminAuditScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BazaarScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EggStockScreen
import com.example.ui.screens.JomaTkScreen
import com.example.ui.screens.MealSheetScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.RiceStockScreen
import com.example.ui.screens.UtilitiesScreen
import com.example.ui.screens.performSignOut
import com.example.ui.theme.MessMateTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MessMateTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val repository = remember { MessRepository.create(context) }
    val viewModel = remember { MessViewModel(repository) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    val isGuest by viewModel.isGuestUser.collectAsState()
    val isAdminAuth by viewModel.isAdminAuthenticated.collectAsState()
    val mess by viewModel.messState.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val canEnterMainApp = currentUser != null

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            viewModel.syncUserProfile()
        }
    }

    if (canEnterMainApp) {
        MainAppContent(
            viewModel = viewModel,
            onSignedOut = {
                viewModel.signOutAdmin()
                currentUser = null
                performSignOut(context, credentialManager, coroutineScope) {}
            }
        )
    } else {
        AuthScreen(
            mess = mess,
            language = language,
            onEnterAsGuest = {
                viewModel.enterAsGuest()
            },
            onMemberAuthSuccess = {
                viewModel.setAdminAuthenticated(false)
                currentUser = Firebase.auth.currentUser
                viewModel.syncUserProfile()
            },
            onAdminAuthSuccess = {
                viewModel.setAdminAuthenticated(true)
                currentUser = Firebase.auth.currentUser
                viewModel.syncUserProfile()
            },
            onClaimInitialAdmin = { password, onSuccess, onFailure ->
                viewModel.claimInitialAdmin(password, onSuccess, onFailure)
            },
            onVerifyAdminPasswordAsync = { password, onSuccess, onFailure ->
                viewModel.verifyAdminPassword(password, onSuccess, onFailure)
            },
            onRequestAdminOtp = { email, onSent ->
                viewModel.requestAdminOtp(email, onSent)
            },
            onVerifyAdminOtp = { code, email, onSuccess, onFailure ->
                viewModel.verifyAdminOtp(code, email, onSuccess, onFailure)
            }
        )
    }
}

@Composable
fun MainAppContent(
    viewModel: MessViewModel,
    onSignedOut: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showInAppOtpDialog by remember { mutableStateOf(false) }
    var showInAppPasswordDialog by remember { mutableStateOf(false) }
    var showInitialAdminClaimDialog by remember { mutableStateOf(false) }
    var showNotAdminAlert by remember { mutableStateOf(false) }

    val mess by viewModel.messState.collectAsState()
    val members by viewModel.membersState.collectAsState()
    val dailyMeals by viewModel.dailyMealsState.collectAsState()
    val expenses by viewModel.expensesState.collectAsState()
    val utilities by viewModel.utilitiesState.collectAsState()
    val eggRecords by viewModel.eggRecordsState.collectAsState()
    val eggSummary by viewModel.eggSummaryState.collectAsState()
    val auditLogs by viewModel.auditLogsState.collectAsState()
    val adminHistory by viewModel.adminHistoryState.collectAsState()
    val summary by viewModel.summaryState.collectAsState()
    val backups by viewModel.backupsState.collectAsState()

    val currentMonth by viewModel.currentMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val isAdmin = viewModel.isUserAdmin(mess)
    val available45Dates = remember(selectedDate) { viewModel.getAvailable45Dates() }

    val currentAuthUser = Firebase.auth.currentUser
    val currentEmail = currentAuthUser?.email?.trim()?.lowercase() ?: ""
    val managerEmail = mess?.managerEmail?.trim()?.lowercase() ?: ""

    val isManagerUnassigned = managerEmail.isBlank() ||
            managerEmail == "manager_placeholder" ||
            managerEmail.contains("placeholder")

    val isRegisteredManager = currentEmail.isNotBlank() &&
            managerEmail.isNotBlank() &&
            currentEmail.equals(managerEmail, ignoreCase = true)

    val requestAdminUnlock = {
        if (isManagerUnassigned) {
            showInitialAdminClaimDialog = true
        } else if (isRegisteredManager && currentAuthUser?.email != null) {
            viewModel.requestAdminOtp(currentAuthUser.email!!) {
                showInAppOtpDialog = true
            }
        } else {
            showNotAdminAlert = true
        }
    }

    if (showInitialAdminClaimDialog && currentAuthUser?.email != null) {
        InitialAdminClaimDialog(
            userEmail = currentAuthUser.email!!,
            language = language,
            onDismiss = { showInitialAdminClaimDialog = false },
            onClaimAdmin = { pass, onSuccess, onFailure ->
                viewModel.claimInitialAdmin(pass, onSuccess, onFailure)
            },
            onSuccess = {
                showInitialAdminClaimDialog = false
                viewModel.setAdminAuthenticated(true)
            }
        )
    }

    if (showInAppOtpDialog && currentAuthUser?.email != null) {
        AdminOtpDialog(
            targetEmail = currentAuthUser.email!!,
            language = language,
            onDismiss = { showInAppOtpDialog = false },
            onVerifySuccess = {
                showInAppOtpDialog = false
                showInAppPasswordDialog = true
            },
            onResendOtp = {
                viewModel.requestAdminOtp(currentAuthUser.email!!)
            }
        )
    }

    if (showInAppPasswordDialog && currentAuthUser?.email != null) {
        AdminPasswordVerificationDialog(
            targetEmail = currentAuthUser.email!!,
            language = language,
            onDismiss = { showInAppPasswordDialog = false },
            onVerifyPassword = { password, onSuccess, onFailure ->
                viewModel.verifyAdminPassword(password, onSuccess, onFailure)
            },
            onSuccess = {
                showInAppPasswordDialog = false
                viewModel.setAdminAuthenticated(true)
            }
        )
    }

    if (showNotAdminAlert) {
        AlertDialog(
            onDismissRequest = { showNotAdminAlert = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = if (language == Language.BENGALI) "অ্যাডমিন অনুমতি নেই" else "Not Authorized",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == Language.BENGALI)
                        "আপনার বর্তমান জিমেইল (${currentAuthUser?.email}) এই মেসের নিবন্ধিত অ্যাডমিনের (${mess?.managerEmail ?: "নির্ধারিত জিমেইল"}) সাথে মেলেনি।"
                    else
                        "Your current signed-in Gmail does not match the designated mess manager."
                )
            },
            confirmButton = {
                TextButton(onClick = { showNotAdminAlert = false }) {
                    Text(text = if (language == Language.BENGALI) "ঠিক আছে" else "OK")
                }
            }
        )
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MessTopBar(
                mess = mess,
                isAdmin = isAdmin,
                language = language,
                currentMonth = currentMonth,
                userEmail = currentAuthUser?.email,
                userPhotoUrl = currentAuthUser?.photoUrl?.toString(),
                onToggleLanguage = { viewModel.toggleLanguage() },
                onSignOut = onSignedOut,
                onSwitchToAdmin = if (!isAdmin) {
                    { requestAdminUnlock() }
                } else null
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Helper Banner for Manager / Unassigned mess who hasn't unlocked admin yet
            if (!isAdmin && (isRegisteredManager || isManagerUnassigned)) {
                Surface(
                    color = AmberSecondary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = AmberSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isManagerUnassigned) {
                                    if (language == Language.BENGALI)
                                        "এই মেসে কোনো অ্যাডমিন নির্ধারিত নেই। প্রথম অ্যাডমিন হতে পাসওয়ার্ড তৈরি করুন।"
                                    else
                                        "No manager assigned. Set up password to become the first manager."
                                } else {
                                    if (language == Language.BENGALI)
                                        "আপনি নিবন্ধিত অ্যাডমিন। পূর্ণ নিয়ন্ত্রণ পেতে ৩-ধাপের নিরাপত্তা যাচাই সম্পন্ন করুন।"
                                    else
                                        "You are the manager. Complete 3-tier security to unlock management."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(
                            onClick = { requestAdminUnlock() }
                        ) {
                            Text(
                                text = if (language == Language.BENGALI) "আনলক করুন" else "Unlock",
                                fontWeight = FontWeight.Bold,
                                color = AmberSecondary
                            )
                        }
                    }
                }
            }

            NavigationTabRow(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                language = language,
                isAdmin = isAdmin
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> DashboardScreen(
                        summary = summary,
                        eggSummary = eggSummary,
                        backups = backups,
                        currentMonth = currentMonth,
                        messName = mess?.name ?: "Meal Management",
                        language = language,
                        isAdmin = isAdmin,
                        onNavigateToMealSheet = { selectedTabIndex = 1 },
                        onNavigateToReports = { selectedTabIndex = 7 },
                        onNavigateToEggStock = { selectedTabIndex = 4 },
                        onNavigateToJomaTk = { selectedTabIndex = 3 },
                        onToggleCalculationMode = { viewModel.setCalculationMode(it) },
                        onResetData = { pwd, label, onDone ->
                            viewModel.resetMessData(pwd, label, onDone)
                        },
                        onRestoreBackup = { backupId, onDone ->
                            viewModel.restoreBackup(backupId, onDone)
                        },
                        onCreateBackup = { label, onDone ->
                            viewModel.createManualBackup(label, onDone)
                        }
                    )
                    1 -> MealSheetScreen(
                        selectedDate = selectedDate,
                        available45Dates = available45Dates,
                        onDateSelected = { viewModel.selectDate(it) },
                        members = members,
                        dailyMeals = dailyMeals,
                        isAdmin = isAdmin,
                        language = language,
                        messName = mess?.name ?: "Meal Management",
                        expenses = expenses,
                        eggRecords = eggRecords,
                        eggSummary = eggSummary,
                        calculationMode = summary.calculationMode,
                        fullMealRate = summary.fullMealRate,
                        partialMealRate = summary.partialMealRate,
                        summary = summary,
                        onUpdateMeal = { date, memId, type, count ->
                            viewModel.updateMeal(date, memId, type, count)
                        },
                        onToggleFullDayMeal = { date, memId, turnOn ->
                            viewModel.toggleMemberFullDayMeal(date, memId, turnOn)
                        },
                        onSaveMember = { viewModel.saveMember(it) },
                        onDeleteMember = { id, name -> viewModel.deleteMember(id, name) },
                        onCopyPreviousDayMeals = { viewModel.copyMealsFromPreviousDate(it) }
                    )
                    2 -> BazaarScreen(
                        expenses = expenses,
                        members = members,
                        currentMonth = currentMonth,
                        isAdmin = isAdmin,
                        language = language,
                        onSaveExpense = { viewModel.saveExpense(it) },
                        onDeleteExpense = { id, title -> viewModel.deleteExpense(id, title) }
                    )
                    3 -> JomaTkScreen(
                        summary = summary,
                        currentMonth = currentMonth,
                        messName = mess?.name ?: "Meal Management",
                        isAdmin = isAdmin,
                        language = language,
                        onUpdateMemberDeposit = { viewModel.saveMember(it) }
                    )
                    4 -> EggStockScreen(
                        eggRecords = eggRecords,
                        eggSummary = eggSummary,
                        currentMonth = currentMonth,
                        isAdmin = isAdmin,
                        language = language,
                        onSaveEggRecord = { viewModel.saveEggRecord(it) },
                        onDeleteEggRecord = { id, info -> viewModel.deleteEggRecord(id, info) }
                    )
                    5 -> RiceStockScreen(
                        summary = summary,
                        isAdmin = isAdmin,
                        language = language,
                        onUpdateMemberDeposit = { viewModel.saveMember(it) }
                    )
                    6 -> UtilitiesScreen(
                        utilities = utilities,
                        currentMonth = currentMonth,
                        activeMemberCount = members.count { it.active },
                        isAdmin = isAdmin,
                        language = language,
                        onSaveUtility = { viewModel.saveUtility(it) },
                        onDeleteUtility = { id, title -> viewModel.deleteUtility(id, title) }
                    )
                    7 -> ReportsScreen(
                        messName = mess?.name ?: "Meal Management",
                        currentMonth = currentMonth,
                        summary = summary,
                        utilities = utilities,
                        eggSummary = eggSummary,
                        isAdmin = isAdmin,
                        language = language,
                        onUpdateMember = { viewModel.saveMember(it) }
                    )
                    8 -> AdminAuditScreen(
                        mess = mess,
                        auditLogs = auditLogs,
                        adminHistory = adminHistory,
                        isAdmin = isAdmin,
                        language = language,
                        onTransferManager = { newEmail, note, onDone ->
                            viewModel.transferManager(newEmail = newEmail, targetUid = null, note = note, onDone = onDone)
                        },
                        onTransferAdmin = { newEmail, newUid ->
                            viewModel.transferAdmin(newEmail, newUid)
                        },
                        onTransferAdminWithPassword = { currentPassword, newEmail, newPassword, onDone ->
                            viewModel.transferManagerWithPassword(currentPassword, newEmail, newPassword, onDone)
                        },
                        onChangePassword = { oldPass, newPass, onDone ->
                            viewModel.changeAdminPassword(oldPass, newPass, onDone)
                        }
                    )
                }
            }
        }
    }
}
