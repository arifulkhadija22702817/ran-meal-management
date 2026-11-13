package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminHandoverRecord
import com.example.data.model.AuditLog
import com.example.data.model.DailyMeal
import com.example.data.model.EggRecord
import com.example.data.model.EggSummary
import com.example.data.model.Expense
import com.example.data.model.Member
import com.example.data.model.Mess
import com.example.data.model.MessBackup
import com.example.data.model.MessSummary
import com.example.data.model.Utility
import com.example.data.repository.MessRepository
import com.example.util.AntiCorruptionPolicy
import com.example.util.MessCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class Language(val code: String, val label: String) {
    BENGALI("bn", "বাংলা"),
    ENGLISH("en", "English")
}

class MessViewModel(
    private val repository: MessRepository
) : ViewModel() {

    companion object {
        private const val TAG = "MessViewModel"
    }

    private val _currentMonth = MutableStateFlow(
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    )
    val currentMonth: StateFlow<String> = _currentMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    )
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Default language is now Bengali (বাংলা)
    private val _currentLanguage = MutableStateFlow(Language.BENGALI)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _isOperationLoading = MutableStateFlow(false)
    val isOperationLoading: StateFlow<Boolean> = _isOperationLoading.asStateFlow()

    // Guest Mode (enters without Gmail)
    private val _isGuestUser = MutableStateFlow(false)
    val isGuestUser: StateFlow<Boolean> = _isGuestUser.asStateFlow()

    // Admin password verified in session
    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    // Verified admin Gmail bound in memory for zero-latency reactivity before Firestore snapshot arrives
    private val _verifiedAdminEmail = MutableStateFlow<String?>(null)
    private val _localMessOverride = MutableStateFlow<Mess?>(null)

    val currentUserUid: String?
        get() = repository.getCurrentUserUid()
    val currentUserEmail: String?
        get() = repository.getCurrentUserEmail()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val authUidFlow: StateFlow<String?> = repository.observeAuthUid()
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.getCurrentUserUid())

    @OptIn(ExperimentalCoroutinesApi::class)
    val messState: StateFlow<Mess?> = combine(
        authUidFlow.flatMapLatest { uid ->
            if (uid == null) {
                flowOf(null)
            } else {
                repository.observeMess()
                    .catch { e ->
                        android.util.Log.e("MessViewModel", "observeMess flow error: ${e.message}", e)
                        emit(null)
                    }
            }
        },
        _localMessOverride
    ) { remoteMess, localOverride ->
        when {
            remoteMess != null && remoteMess.managerEmail.isNotBlank() && !remoteMess.managerEmail.contains("placeholder") -> remoteMess
            localOverride != null -> localOverride
            else -> remoteMess
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val membersState: StateFlow<List<Member>> = authUidFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else repository.observeMembers().catch { e ->
            android.util.Log.e("MessViewModel", "observeMembers flow error: ${e.message}", e)
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyMealsState: StateFlow<List<DailyMeal>> = authUidFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else repository.observeDailyMeals().catch { e ->
            android.util.Log.e("MessViewModel", "observeDailyMeals flow error: ${e.message}", e)
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val expensesState: StateFlow<List<Expense>> = authUidFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else repository.observeExpenses().catch { e ->
            android.util.Log.e("MessViewModel", "observeExpenses flow error: ${e.message}", e)
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val utilitiesState: StateFlow<List<Utility>> = authUidFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else repository.observeUtilities().catch { e ->
            android.util.Log.e("MessViewModel", "observeUtilities flow error: ${e.message}", e)
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val auditLogsState: StateFlow<List<AuditLog>> = authUidFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else repository.observeAuditLogs().catch { e ->
            android.util.Log.e("MessViewModel", "observeAuditLogs flow error: ${e.message}", e)
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val eggRecordsState: StateFlow<List<EggRecord>> = authUidFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else repository.observeEggRecords().catch { e ->
            android.util.Log.e("MessViewModel", "observeEggRecords flow error: ${e.message}", e)
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val backupsState: StateFlow<List<MessBackup>> = authUidFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else repository.observeBackups().catch { e ->
            android.util.Log.e("MessViewModel", "observeBackups flow error: ${e.message}", e)
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminHistoryState: StateFlow<List<AdminHandoverRecord>> = authUidFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else repository.observeAdminHistory().catch { e ->
            android.util.Log.e("MessViewModel", "observeAdminHistory flow error: ${e.message}", e)
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val authorizedMembersState: StateFlow<List<com.example.data.model.AuthorizedMember>> = authUidFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else repository.observeAuthorizedMembers().catch { e ->
            android.util.Log.e("MessViewModel", "observeAuthorizedMembers flow error: ${e.message}", e)
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Egg Inventory Auto-Calculation:
    // Remaining stock = Total Purchased - Total Consumed - Total Damaged
    val eggSummaryState: StateFlow<EggSummary> = eggRecordsState
        .combine(_currentMonth) { records, _ ->
            val totalPurchased = records.sumOf { it.purchasedCount }
            val totalConsumed = records.sumOf { it.consumedCount }
            val totalDamaged = records.sumOf { it.damagedCount }
            val currentStock = totalPurchased - totalConsumed - totalDamaged
            val totalCost = records.sumOf { it.cost }
            EggSummary(
                totalPurchased = totalPurchased,
                totalConsumed = totalConsumed,
                totalDamaged = totalDamaged,
                currentStock = currentStock,
                totalCost = totalCost
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EggSummary())

    // Combined live summary calculation with instantaneous mode and date updates
    val summaryState: StateFlow<MessSummary> = combine(
        combine(membersState, dailyMealsState, expensesState) { m, dm, ex -> Triple(m, dm, ex) },
        combine(utilitiesState, currentMonth, messState) { u, cm, ms -> Triple(u, cm, ms) },
        selectedDate
    ) { (members, dailyMeals, expenses), (utilities, month, mess), date ->
        val mode = mess?.calculationMode ?: com.example.data.model.CALCULATION_MODE_FIXED_DAILY
        val fullRate = mess?.fullMealRate ?: 40.0
        val partialRate = mess?.partialMealRate ?: 25.0
        MessCalculator.calculateSummary(
            members = members,
            dailyMeals = dailyMeals,
            expenses = expenses,
            utilities = utilities,
            monthPrefix = month,
            calculationMode = mode,
            fullMealRate = fullRate,
            partialMealRate = partialRate,
            selectedDate = date
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MessCalculator.calculateSummary(
            members = emptyList(),
            dailyMeals = emptyList(),
            expenses = emptyList(),
            utilities = emptyList(),
            monthPrefix = _currentMonth.value,
            calculationMode = com.example.data.model.CALCULATION_MODE_FIXED_DAILY
        )
    )

    private val autoPrefilledDates = mutableSetOf<String>()

    init {
        authUidFlow.onEach { uid ->
            if (uid == null) {
                _isAdminAuthenticated.value = false
                _verifiedAdminEmail.value = null
                _localMessOverride.value = null
            } else {
                val currentEmail = repository.getCurrentUserEmail()
                val savedSession = repository.getSavedAdminSession(uid, currentEmail)
                if (savedSession != null) {
                    _verifiedAdminEmail.value = savedSession.first
                    _isAdminAuthenticated.value = true
                }
                try {
                    val res = repository.initOrGetMess()
                    res.getOrNull()?.let { fetched ->
                        if (fetched.managerEmail.isNotBlank() && !fetched.managerEmail.contains("placeholder")) {
                            _localMessOverride.value = fetched
                            if (savedSession != null) {
                                val emailMismatch = !fetched.managerEmail.equals(savedSession.first, ignoreCase = true)
                                val hashMismatch = savedSession.second.isNotBlank() &&
                                    fetched.adminPassword.isNotBlank() &&
                                    fetched.adminPassword != savedSession.second
                                if (emailMismatch || hashMismatch) {
                                    _isAdminAuthenticated.value = false
                                    _verifiedAdminEmail.value = null
                                    repository.clearAdminSession()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MessViewModel", "initOrGetMess error: ${e.message}")
                }
            }
        }.launchIn(viewModelScope)

        // Revoke local admin session immediately if remote mess changes manager or password hash
        messState.onEach { remoteMess ->
            if (remoteMess != null && _isAdminAuthenticated.value) {
                val uid = repository.getCurrentUserUid()
                val email = repository.getCurrentUserEmail()
                val saved = repository.getSavedAdminSession(uid, email)
                if (remoteMess.managerEmail.isNotBlank() && !remoteMess.managerEmail.contains("placeholder")) {
                    val userEmail = (email ?: "").trim().lowercase()
                    if (userEmail.isNotBlank() && !remoteMess.managerEmail.equals(userEmail, ignoreCase = true)) {
                        _isAdminAuthenticated.value = false
                        _verifiedAdminEmail.value = null
                        repository.clearAdminSession()
                    } else if (saved != null && saved.second.isNotBlank() &&
                        remoteMess.adminPassword.isNotBlank() &&
                        remoteMess.adminPassword != saved.second &&
                        remoteMess.adminPassword != "configured"
                    ) {
                        _isAdminAuthenticated.value = false
                        _verifiedAdminEmail.value = null
                        repository.clearAdminSession()
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun grantAdminSessionForCurrentUser(configuredPasswordMarker: String = "configured") {
        val user = repository.auth.currentUser
        val email = (user?.email ?: "").trim().lowercase()
        val uid = user?.uid ?: ""
        if (email.isNotBlank()) {
            _verifiedAdminEmail.value = email
            val current = messState.value
            _localMessOverride.value = (current ?: Mess(
                id = MessRepository.DEFAULT_MESS_ID,
                name = "Meal Management Hostel",
                currency = "BDT",
                currentMonth = _currentMonth.value
            )).copy(
                managerUid = uid.ifBlank { current?.managerUid ?: "" },
                managerEmail = email,
                adminPassword = current?.adminPassword?.ifBlank { configuredPasswordMarker } ?: configuredPasswordMarker
            )
            _isAdminAuthenticated.value = true
        }
    }

    fun enterAsGuest() {
        _isGuestUser.value = true
        _isAdminAuthenticated.value = false
        _verifiedAdminEmail.value = null
    }

    fun exitGuest() {
        _isGuestUser.value = false
    }

    fun signOutAdmin() {
        _isAdminAuthenticated.value = false
        _verifiedAdminEmail.value = null
        _localMessOverride.value = null
        _isGuestUser.value = false
        repository.clearAdminSession()
    }

    fun setAdminAuthenticated(auth: Boolean) {
        _isAdminAuthenticated.value = auth
        if (!auth) {
            _verifiedAdminEmail.value = null
            repository.clearAdminSession()
        } else {
            val email = repository.auth.currentUser?.email?.trim()?.lowercase()
            val uid = repository.auth.currentUser?.uid
            if (!email.isNullOrBlank()) {
                if (_verifiedAdminEmail.value.isNullOrBlank()) {
                    _verifiedAdminEmail.value = email
                }
                val hash = messState.value?.adminPassword ?: ""
                if (!uid.isNullOrBlank() && hash.isNotBlank() && hash != "configured") {
                    repository.saveAdminSession(uid, email, hash)
                }
            }
        }
    }

    /**
     * Single Active Admin Access Control:
     * Admin access is granted ONLY IF:
     * 1. The user's signed-in Gmail strictly matches the mess's registered managerEmail (only 1 Gmail can be admin).
     * 2. The user has actively unlocked/authenticated with their secret Admin Password (_isAdminAuthenticated == true).
     * 3. Uses local verified fallback immediately on claim/verify so UI updates without waiting for snapshot latency.
     */
    val isUserAdminFlow: StateFlow<Boolean> = combine(
        _isAdminAuthenticated,
        _verifiedAdminEmail,
        messState,
        authUidFlow
    ) { isAuth, verifiedEmail, mess, uid ->
        if (!isAuth || uid == null) return@combine false
        val currentAuthUser = repository.auth.currentUser ?: return@combine false
        val userEmail = (currentAuthUser.email ?: "").trim().lowercase()
        if (userEmail.isBlank()) return@combine false

        val docManagerEmail = (mess?.managerEmail ?: "").trim().lowercase()
        val effectiveManagerEmail = if (docManagerEmail.isNotBlank() && !docManagerEmail.contains("placeholder")) {
            docManagerEmail
        } else {
            (verifiedEmail ?: "").trim().lowercase()
        }

        effectiveManagerEmail.isNotBlank() && userEmail == effectiveManagerEmail
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        // Auto carry-forward previous day's ON meals to today/next day for admin editing
        combine(isUserAdminFlow, dailyMealsState, _selectedDate) { isAdmin, meals, date ->
            Triple(isAdmin, meals, date)
        }.onEach { (isAdmin, meals, date) ->
            if (isAdmin && meals.isNotEmpty()) {
                checkAndAutoPrefillDate(date, meals)
            }
        }.launchIn(viewModelScope)
    }

    fun isUserAdmin(mess: Mess? = messState.value): Boolean {
        if (!_isAdminAuthenticated.value) return false
        val userEmail = repository.auth.currentUser?.email?.trim()?.lowercase() ?: ""
        if (userEmail.isBlank()) return false

        val docManagerEmail = (mess?.managerEmail ?: "").trim().lowercase()
        val effectiveManagerEmail = if (docManagerEmail.isNotBlank() && !docManagerEmail.contains("placeholder")) {
            docManagerEmail
        } else {
            (_verifiedAdminEmail.value ?: "").trim().lowercase()
        }

        return effectiveManagerEmail.isNotBlank() && userEmail == effectiveManagerEmail
    }

    fun complete3TierAdminAuthentication(
        passwordInput: String,
        isNewPassword: Boolean,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val user = repository.auth.currentUser
            if (user == null) {
                _isOperationLoading.value = false
                onFailure("গুগল সাইন-ইন পাওয়া যায়নি। পুনরায় চেষ্টা করুন।")
                return@launch
            }
            val email = user.email?.trim()?.lowercase() ?: ""
            val uid = user.uid

            if (isNewPassword) {
                val res = repository.claimInitialAdmin(
                    messId = MessRepository.DEFAULT_MESS_ID,
                    initialPassword = passwordInput
                )
                _isOperationLoading.value = false
                res.onSuccess {
                    grantAdminSessionForCurrentUser()
                    _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                        "অভিনন্দন! ৩-ধাপের সম্পূর্ণ নিরাপত্তা যাচাইকরণ সফল। আপনি এখন মেসের অ্যাডমিন।"
                    else
                        "Congratulations! 3-Tier security verified and you are now Admin."
                    onSuccess()
                }.onFailure {
                    onFailure(it.message ?: "Failed to set admin password")
                }
            } else {
                val res = repository.verifyAdminPassword(
                    messId = MessRepository.DEFAULT_MESS_ID,
                    passwordInput = passwordInput
                )
                _isOperationLoading.value = false
                res.onSuccess { matched ->
                    if (matched) {
                        try {
                            repository.db.collection("messes").document(MessRepository.DEFAULT_MESS_ID)
                                .update(
                                    mapOf(
                                        "managerUid" to uid,
                                        "managerEmail" to email,
                                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                    )
                                )
                        } catch (e: Exception) {
                            android.util.Log.w("MessViewModel", "Notice syncing managerUid: ${e.message}")
                        }
                        grantAdminSessionForCurrentUser()
                        _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                            "স্বাগতম অ্যাডমিন! ৩-ধাপের নিরাপত্তা যাচাইকরণ সফল হয়েছে।"
                        else
                            "Welcome Admin! 3-Tier verification successful."
                        onSuccess()
                    } else {
                        onFailure(if (_currentLanguage.value == Language.BENGALI) "ভুল অ্যাডমিন পাসওয়ার্ড!" else "Incorrect admin password!")
                    }
                }.onFailure {
                    onFailure(it.message ?: "Error verifying password")
                }
            }
        }
    }

    fun requestAdminOtp(targetEmail: String, onSent: () -> Unit = {}) {
        com.example.data.service.AdminOtpService.generateOtp(targetEmail)
        com.example.data.service.AdminOtpService.sendOtpEmail(targetEmail) { _, msg ->
            _userMessage.value = msg
            onSent()
        }
    }

    fun verifyAdminOtp(
        enteredCode: String,
        userEmail: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val (success, msg) = com.example.data.service.AdminOtpService.verifyOtp(enteredCode, userEmail)
        if (success) {
            _isAdminAuthenticated.value = true
            _userMessage.value = "স্বাগতম অ্যাডমিন! ২-স্টেপ যাচাইকরণ সফল হয়েছে।"
            viewModelScope.launch {
                repository.logAudit(
                    action = "ADMIN_2FA_VERIFIED",
                    details = "Admin successfully authenticated with email OTP verification"
                )
            }
            onSuccess()
        } else {
            onFailure(msg)
        }
    }

    fun verifyPassword(inputPassword: String, mess: Mess?): Boolean {
        val stored = mess?.adminPassword ?: ""
        return if (stored.isBlank()) true else MessRepository.verifyPasswordHash(inputPassword, stored)
    }

    fun isUsingDefaultPassword(mess: Mess?): Boolean {
        return (mess?.adminPassword ?: "").isBlank()
    }

    fun claimInitialAdmin(
        initialPassword: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.claimInitialAdmin(
                messId = MessRepository.DEFAULT_MESS_ID,
                initialPassword = initialPassword
            )
            _isOperationLoading.value = false
            result.onSuccess {
                grantAdminSessionForCurrentUser()
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                    "অভিনন্দন! আপনি সফলভাবে মেস অ্যাডমিন/ম্যানেজার হিসেবে নিবন্ধিত হয়েছেন।"
                else
                    "Congratulations! You are now the registered mess manager."
                onSuccess()
            }.onFailure {
                onFailure(it.message ?: "Failed to claim admin role")
            }
        }
    }

    fun isMessAdminConfigured(mess: Mess? = messState.value): Boolean {
        if (mess == null) return false
        val email = mess.managerEmail.trim().lowercase()
        val pass = mess.adminPassword.trim()
        return email.isNotEmpty() && !email.contains("placeholder") && pass.isNotEmpty()
    }

    fun verifyAdminCredentials(
        emailInput: String,
        passwordInput: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val res = repository.verifyAdminCredentials(
                messId = MessRepository.DEFAULT_MESS_ID,
                inputEmail = emailInput,
                inputPassword = passwordInput
            )
            _isOperationLoading.value = false
            res.onSuccess { matched ->
                if (matched) {
                    grantAdminSessionForCurrentUser()
                    _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                        "স্বাগতম অ্যাডমিন! মেস ব্যবস্থাপনা সফলভাবে আনলক হয়েছে।"
                    else
                        "Welcome Admin! Mess management unlocked successfully."
                    onSuccess()
                } else {
                    _isAdminAuthenticated.value = false
                    _verifiedAdminEmail.value = null
                    onFailure(if (_currentLanguage.value == Language.BENGALI) "ভুল অ্যাডমিন তথ্য!" else "Invalid admin credentials!")
                }
            }.onFailure { err ->
                _isAdminAuthenticated.value = false
                _verifiedAdminEmail.value = null
                onFailure(err.message ?: if (_currentLanguage.value == Language.BENGALI) "যাচাই ব্যর্থ হয়েছে" else "Verification failed")
            }
        }
    }

    fun verifyAdminPassword(
        passwordInput: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = repository.auth.currentUser
        val uid = user?.uid ?: ""
        val email = (user?.email ?: "").trim().lowercase()

        viewModelScope.launch {
            _isOperationLoading.value = true
            val res = repository.verifyAdminPassword(MessRepository.DEFAULT_MESS_ID, passwordInput)
            _isOperationLoading.value = false
            res.onSuccess { matched ->
                if (matched) {
                    grantAdminSessionForCurrentUser()
                    if (uid.isNotEmpty()) {
                        try {
                            repository.db.collection("messes").document(MessRepository.DEFAULT_MESS_ID)
                                .update(
                                    mapOf(
                                        "managerUid" to uid,
                                        "managerEmail" to email,
                                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                    )
                                )
                            repository.ensureUserAuthorized(
                                messId = MessRepository.DEFAULT_MESS_ID,
                                uid = uid,
                                email = email,
                                displayName = user?.displayName ?: email,
                                role = "MANAGER"
                            )
                        } catch (e: Exception) {
                            android.util.Log.w("MessViewModel", "Notice binding manager: ${e.message}")
                        }
                    }
                    _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                        "স্বাগতম অ্যাডমিন! মেস ব্যবস্থাপনা সফলভাবে আনলক হয়েছে।"
                    else
                        "Welcome Admin! Mess management unlocked successfully."
                    onSuccess()
                } else {
                    _isAdminAuthenticated.value = false
                    _verifiedAdminEmail.value = null
                    onFailure(if (_currentLanguage.value == Language.BENGALI) "ভুল অ্যাডমিন পাসওয়ার্ড! প্রবেশাধিকার অস্বীকৃত।" else "Incorrect admin password! Access denied.")
                }
            }.onFailure { err ->
                _isAdminAuthenticated.value = false
                _verifiedAdminEmail.value = null
                onFailure(err.message ?: if (_currentLanguage.value == Language.BENGALI) "পাসওয়ার্ড যাচাই ব্যর্থ হয়েছে" else "Password verification failed")
            }
        }
    }

    fun changeAdminPassword(
        oldPassword: String?,
        newPassword: String,
        onDone: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.updateAdminPassword(
                messId = MessRepository.DEFAULT_MESS_ID,
                oldPassword = oldPassword,
                newPassword = newPassword
            )
            _isOperationLoading.value = false
            result.onSuccess {
                val msg = if (_currentLanguage.value == Language.BENGALI)
                    "অ্যাডমিন পাসওয়ার্ড সফলভাবে আপডেট করা হয়েছে!"
                else
                    "Admin password updated successfully!"
                _userMessage.value = msg
                onDone(true, msg)
            }.onFailure {
                val msg = it.message ?: "পাসওয়ার্ড আপডেট ব্যর্থ হয়েছে"
                _userMessage.value = msg
                onDone(false, msg)
            }
        }
    }

    fun recoverAdminPassword(
        newPassword: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.recoverAdminPassword(
                messId = MessRepository.DEFAULT_MESS_ID,
                newPassword = newPassword
            )
            _isOperationLoading.value = false
            result.onSuccess {
                grantAdminSessionForCurrentUser()
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                    "অ্যাডমিন পাসওয়ার্ড সফলভাবে রিকভার ও আপডেট করা হয়েছে!"
                else
                    "Admin password recovered and updated successfully!"
                onSuccess()
            }.onFailure {
                _isAdminAuthenticated.value = false
                _verifiedAdminEmail.value = null
                onFailure(it.message ?: "পাসওয়ার্ড রিকভারি ব্যর্থ হয়েছে")
            }
        }
    }

    fun resetPasswordWithEmail(email: String, newPassword: String, mess: Mess?): Boolean {
        return true
    }

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == Language.ENGLISH) Language.BENGALI else Language.ENGLISH
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        // Also update currentMonth if date belongs to another month
        if (date.length >= 7) {
            val month = date.substring(0, 7)
            if (month != _currentMonth.value) {
                _currentMonth.value = month
            }
        }
        checkAndAutoPrefillDate(date, dailyMealsState.value)
    }

    private fun checkAndAutoPrefillDate(targetDate: String, meals: List<DailyMeal> = dailyMealsState.value) {
        if (!isUserAdmin(messState.value)) return
        if (autoPrefilledDates.contains(targetDate)) return

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (targetDate < todayStr) return

        val currentMeal = meals.firstOrNull { it.date == targetDate }
        if (currentMeal != null && currentMeal.memberMeals.isNotEmpty()) return
        if (AntiCorruptionPolicy.isMealLocked(currentMeal, targetDate)) return

        val prevDate = com.example.ui.screens.shiftDate(targetDate, -1)
        val prevMeal = meals.firstOrNull { it.date == prevDate }
        val hasAnyMealOn = prevMeal?.memberMeals?.values?.any { m ->
            (m["b"] ?: 0.0) > 0.0 || (m["l"] ?: 0.0) > 0.0 || (m["d"] ?: 0.0) > 0.0
        } == true

        if (hasAnyMealOn) {
            autoPrefilledDates.add(targetDate)
            copyMealsFromPreviousDate(targetDate, silent = true)
        }
    }

    fun copyMealsFromPreviousDate(targetDate: String, silent: Boolean = false) {
        if (!requireAdminOrShowError()) return
        val prevDate = com.example.ui.screens.shiftDate(targetDate, -1)
        val prevMeal = dailyMealsState.value.firstOrNull { it.date == prevDate }
        if (prevMeal == null || prevMeal.memberMeals.isEmpty()) {
            if (!silent) {
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                    "পূর্ববর্তী দিন ($prevDate)-এ কোনো মিলের রেকর্ড পাওয়া যায়নি।"
                else
                    "No meal data found for previous date ($prevDate)."
            }
            return
        }

        val copiedMemberMeals = prevMeal.memberMeals.mapValues { (_, mealsMap) ->
            mealsMap.toMap()
        }
        val totalB = copiedMemberMeals.values.sumOf { it["b"] ?: 0.0 }
        val totalL = copiedMemberMeals.values.sumOf { it["l"] ?: 0.0 }
        val totalD = copiedMemberMeals.values.sumOf { it["d"] ?: 0.0 }

        val newDailyMeal = DailyMeal(
            id = targetDate,
            messId = MessRepository.DEFAULT_MESS_ID,
            date = targetDate,
            breakfastCount = totalB,
            lunchCount = totalL,
            dinnerCount = totalD,
            memberMeals = copiedMemberMeals,
            notes = "Auto-copied from $prevDate"
        )

        viewModelScope.launch {
            val result = repository.saveDailyMeal(newDailyMeal)
            result.onSuccess {
                if (!silent) {
                    _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                        "পূর্ববর্তী দিন ($prevDate) থেকে আজকের মিল স্বয়ংক্রিয়ভাবে কপি করা হয়েছে! প্রয়োজন অনুযায়ী এডিট করতে পারেন।"
                    else
                        "Meals copied from $prevDate! You can now adjust any meal."
                }
            }
        }
    }

    fun selectMonth(month: String) {
        _currentMonth.value = month
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    /**
     * Returns a rolling list of 45 consecutive days for the meal sheet.
     */
    fun getAvailable45Dates(): List<String> {
        val list = mutableListOf<String>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        // Include past 42 days up to next 2 days = 45 days total
        cal.add(Calendar.DAY_OF_YEAR, -42)
        for (i in 0 until 45) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list.reversed() // most recent first
    }

    // --- Action methods (Strictly Admin / Manager Only) ---

    private fun requireAdminOrShowError(): Boolean {
        if (!isUserAdmin(messState.value)) {
            _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                "অনুমতি নেই! শুধুমাত্র মেস ম্যানেজার/অ্যাডমিন পরিবর্তন বা এন্ট্রি দিতে পারবেন।"
            else
                "Unauthorized! Only Mess Manager/Admin can make changes."
            return false
        }
        return true
    }

    fun saveMember(member: Member) {
        if (!requireAdminOrShowError()) return
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.saveMember(member)
            _isOperationLoading.value = false
            result.onSuccess {
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI) "সদস্য সফলভাবে সংরক্ষিত হয়েছে" else "Member saved successfully"
            }.onFailure {
                _userMessage.value = "Failed: ${it.message}"
            }
        }
    }

    fun deleteMember(memberId: String, memberName: String) {
        if (!requireAdminOrShowError()) return
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.deleteMember(MessRepository.DEFAULT_MESS_ID, memberId, memberName)
            _isOperationLoading.value = false
            result.onSuccess {
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI) "সদস্য মুছে ফেলা হয়েছে" else "Member deleted"
            }.onFailure {
                _userMessage.value = "Failed: ${it.message}"
            }
        }
    }

    fun updateMeal(
        date: String,
        memberId: String,
        mealType: String, // "b", "l", "d"
        value: Double
    ) {
        if (!requireAdminOrShowError()) return
        val existingDaily = dailyMealsState.value.firstOrNull { it.date == date }
        if (AntiCorruptionPolicy.isMealLocked(existingDaily, date)) {
            _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                "🔒 আপডেট করার পর ২৪ ঘণ্টা অতিক্রান্ত হয়েছে! দুর্নীতি রোধের স্বার্থে এই দিনের মিল ডাটা অটো-ব্লক করা হয়েছে।"
            else
                "🔒 24 hours have passed since update! This day's meal data is auto-blocked."
            return
        }
        viewModelScope.launch {
            val existingMeals = existingDaily?.memberMeals?.toMutableMap() ?: mutableMapOf()
            val memberEntry = existingMeals[memberId]?.toMutableMap() ?: mutableMapOf()

            memberEntry[mealType] = value
            existingMeals[memberId] = memberEntry

            val totalB = existingMeals.values.sumOf { it["b"] ?: 0.0 }
            val totalL = existingMeals.values.sumOf { it["l"] ?: 0.0 }
            val totalD = existingMeals.values.sumOf { it["d"] ?: 0.0 }

            val updatedDailyMeal = DailyMeal(
                id = date,
                messId = MessRepository.DEFAULT_MESS_ID,
                date = date,
                breakfastCount = totalB,
                lunchCount = totalL,
                dinnerCount = totalD,
                memberMeals = existingMeals,
                notes = existingDaily?.notes ?: ""
            )

            val result = repository.saveDailyMeal(updatedDailyMeal)
            result.onSuccess {
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI) "মিল আপডেট সফল হয়েছে" else "Meal updated successfully"
            }.onFailure { err ->
                _userMessage.value = "Failed: ${err.message}"
            }
        }
    }

    fun toggleMemberFullDayMeal(
        date: String,
        memberId: String,
        turnOn: Boolean
    ) {
        if (!requireAdminOrShowError()) return
        val existingDaily = dailyMealsState.value.firstOrNull { it.date == date }
        if (AntiCorruptionPolicy.isMealLocked(existingDaily, date)) {
            _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                "🔒 আপডেট করার পর ২৪ ঘণ্টা অতিক্রান্ত হয়েছে! দুর্নীতি রোধের স্বার্থে এই দিনের মিল ডাটা অটো-ব্লক করা হয়েছে।"
            else
                "🔒 24 hours have passed since update! This day's meal data is auto-blocked."
            return
        }
        viewModelScope.launch {
            val existingMeals = existingDaily?.memberMeals?.toMutableMap() ?: mutableMapOf()
            val memberEntry = existingMeals[memberId]?.toMutableMap() ?: mutableMapOf()

            if (turnOn) {
                // Turn ON full day: Breakfast 1.0, Lunch 1.0, Dinner 1.0
                memberEntry["b"] = 1.0
                memberEntry["l"] = 1.0
                memberEntry["d"] = 1.0
            } else {
                // Turn OFF full day: 0.0 for all 3 meals
                memberEntry["b"] = 0.0
                memberEntry["l"] = 0.0
                memberEntry["d"] = 0.0
            }
            existingMeals[memberId] = memberEntry

            val totalB = existingMeals.values.sumOf { it["b"] ?: 0.0 }
            val totalL = existingMeals.values.sumOf { it["l"] ?: 0.0 }
            val totalD = existingMeals.values.sumOf { it["d"] ?: 0.0 }

            val updatedDailyMeal = DailyMeal(
                id = date,
                messId = MessRepository.DEFAULT_MESS_ID,
                date = date,
                breakfastCount = totalB,
                lunchCount = totalL,
                dinnerCount = totalD,
                memberMeals = existingMeals,
                notes = existingDaily?.notes ?: ""
            )

            val result = repository.saveDailyMeal(updatedDailyMeal)
            result.onSuccess {
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI) {
                    if (turnOn) "উপস্থিতি: মিল চালু করা হয়েছে" else "উপস্থিতি: মিল বন্ধ করা হয়েছে"
                } else {
                    if (turnOn) "Meal attendance turned ON" else "Meal attendance turned OFF"
                }
            }.onFailure { err ->
                _userMessage.value = "Failed: ${err.message}"
            }
        }
    }

    fun saveExpense(expense: Expense) {
        if (!requireAdminOrShowError()) return
        if (expense.id.isNotBlank()) {
            val existing = expensesState.value.firstOrNull { it.id == expense.id }
            if (existing != null && AntiCorruptionPolicy.isExpenseLocked(existing)) {
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                    "🔒 আপডেট করার পর ২৪ ঘণ্টা অতিক্রান্ত হয়েছে! দুর্নীতি রোধের স্বার্থে এই বাজার খরচটি অটো-ব্লক করা হয়েছে।"
                else
                    "🔒 24 hours have passed since update! This expense is auto-blocked."
                return
            }
        }
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.saveExpense(expense)
            _isOperationLoading.value = false
            result.onSuccess {
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI) "বাজার খরচ সংরক্ষিত হয়েছে" else "Expense saved"
            }.onFailure {
                _userMessage.value = "Failed: ${it.message}"
            }
        }
    }

    fun deleteExpense(expenseId: String, title: String) {
        if (!requireAdminOrShowError()) return
        val existing = expensesState.value.firstOrNull { it.id == expenseId }
        if (existing != null && AntiCorruptionPolicy.isExpenseLocked(existing)) {
            _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                "🔒 আপডেট করার পর ২৪ ঘণ্টা অতিক্রান্ত হয়েছে! দুর্নীতি রোধের স্বার্থে এই বাজার খরচটি অটো-ব্লক করা হয়েছে।"
            else
                "🔒 24 hours have passed since update! This expense is auto-blocked."
            return
        }
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.deleteExpense(MessRepository.DEFAULT_MESS_ID, expenseId, title)
            _isOperationLoading.value = false
            result.onSuccess {
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI) "বাজার খরচ মুছে ফেলা হয়েছে" else "Expense removed"
            }.onFailure {
                _userMessage.value = "Failed: ${it.message}"
            }
        }
    }

    fun saveUtility(utility: Utility) {
        if (!requireAdminOrShowError()) return
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.saveUtility(utility)
            _isOperationLoading.value = false
            result.onSuccess {
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI) "ইউটিলিটি বিল যুক্ত হয়েছে" else "Utility bill recorded"
            }.onFailure {
                _userMessage.value = "Failed: ${it.message}"
            }
        }
    }

    fun deleteUtility(utilityId: String, title: String) {
        if (!requireAdminOrShowError()) return
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.deleteUtility(MessRepository.DEFAULT_MESS_ID, utilityId, title)
            _isOperationLoading.value = false
            result.onSuccess {
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI) "বিল মুছে ফেলা হয়েছে" else "Utility bill removed"
            }.onFailure {
                _userMessage.value = "Failed: ${it.message}"
            }
        }
    }

    fun saveEggRecord(record: EggRecord) {
        if (!requireAdminOrShowError()) return
        if (record.id.isNotBlank()) {
            val existing = eggRecordsState.value.firstOrNull { it.id == record.id }
            if (existing != null && AntiCorruptionPolicy.isEggRecordLocked(existing)) {
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                    "🔒 আপডেট করার পর ২৪ ঘণ্টা অতিক্রান্ত হয়েছে! দুর্নীতি রোধের স্বার্থে এই ডিমের রেকর্ডটি অটো-ব্লক করা হয়েছে।"
                else
                    "🔒 24 hours have passed since update! This egg record is auto-blocked."
                return
            }
        }
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.saveEggRecord(record)
            _isOperationLoading.value = false
            result.onSuccess {
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI) "ডিমের হিসাব সফলভাবে সংরক্ষিত হয়েছে" else "Egg record saved"
            }.onFailure {
                _userMessage.value = "Failed: ${it.message}"
            }
        }
    }

    fun deleteEggRecord(recordId: String, info: String) {
        if (!requireAdminOrShowError()) return
        val existing = eggRecordsState.value.firstOrNull { it.id == recordId }
        if (existing != null && AntiCorruptionPolicy.isEggRecordLocked(existing)) {
            _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                "🔒 আপডেট করার পর ২৪ ঘণ্টা অতিক্রান্ত হয়েছে! দুর্নীতি রোধের স্বার্থে এই ডিমের রেকর্ডটি অটো-ব্লক করা হয়েছে।"
            else
                "🔒 24 hours have passed since update! This egg record is auto-blocked."
            return
        }
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.deleteEggRecord(MessRepository.DEFAULT_MESS_ID, recordId, info)
            _isOperationLoading.value = false
            result.onSuccess {
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI) "ডিমের হিসাব মুছে ফেলা হয়েছে" else "Egg record deleted"
            }.onFailure {
                _userMessage.value = "Failed: ${it.message}"
            }
        }
    }

    fun transferAdmin(newManagerEmail: String, newManagerUid: String) {
        if (!requireAdminOrShowError()) return
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.transferManager(MessRepository.DEFAULT_MESS_ID, newManagerEmail, newManagerUid)
            _isOperationLoading.value = false
            result.onSuccess {
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                    "ম্যানেজার পরিবর্তন করে $newManagerEmail কে দায়িত্ব দেওয়া হয়েছে"
                else
                    "Manager role transferred to $newManagerEmail"
            }.onFailure {
                _userMessage.value = "Failed: ${it.message}"
            }
        }
    }

    /**
     * Atomic Monthly Manager Handover:
     * Current authenticated manager designates a new manager by Gmail address.
     * The UID is verified from authorizedMembersState (or provided directly).
     */
    fun transferManager(
        newEmail: String,
        targetUid: String? = null,
        newAdminPassword: String? = null,
        note: String = "Monthly Handover",
        onDone: (Boolean) -> Unit
    ) {
        if (!requireAdminOrShowError()) {
            onDone(false)
            return
        }
        val cleanEmail = newEmail.trim().lowercase()
        val genuineUid = targetUid?.trim()?.ifBlank { null }
            ?: authorizedMembersState.value.firstOrNull { it.email.trim().lowercase() == cleanEmail }?.uid
            ?: ("pending_" + cleanEmail.replace("@", "_at_").replace(".", "_").filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(40))

        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.transferManager(
                messId = MessRepository.DEFAULT_MESS_ID,
                newManagerEmail = cleanEmail,
                newManagerUid = genuineUid,
                newAdminPassword = newAdminPassword,
                note = note
            )
            _isOperationLoading.value = false
            result.onSuccess {
                // Instantly revoke former admin's authentication privileges in-app
                _isAdminAuthenticated.value = false
                _verifiedAdminEmail.value = null
                _localMessOverride.value = _localMessOverride.value?.copy(
                    managerEmail = cleanEmail,
                    managerUid = genuineUid,
                    adminPassword = if (newAdminPassword.isNullOrBlank()) "" else "configured"
                )
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                    "মাসিক ম্যানেজার হস্তান্তর সফল! নতুন ম্যানেজার: $cleanEmail। আপনার অ্যাডমিন ক্ষমতা প্রত্যাহার করা হয়েছে।"
                else
                    "Monthly manager transferred successfully to $cleanEmail. Your admin privileges have been revoked."
                onDone(true)
            }.onFailure {
                _userMessage.value = "Failed: ${it.message}"
                onDone(false)
            }
        }
    }

    fun syncUserProfile() {
        viewModelScope.launch {
            repository.syncUserProfile()
        }
    }

    fun transferManagerWithPassword(
        currentPassword: String,
        newEmail: String,
        newPassword: String?,
        onDone: (Boolean) -> Unit
    ) {
        val storedPass = messState.value?.adminPassword ?: ""
        val isValid = if (storedPass.isBlank()) {
            currentPassword.trim() == "Admin123@" || currentPassword.isBlank()
        } else {
            MessRepository.verifyPasswordHash(currentPassword.trim(), storedPass) ||
            (storedPass == MessRepository.hashAdminPassword("Admin123@") && currentPassword.trim() == "Admin123@")
        }
        if (!isValid) {
            _userMessage.value = if (_currentLanguage.value == Language.BENGALI)
                "ভুল বর্তমান অ্যাডমিন পাসওয়ার্ড!"
            else
                "Incorrect current admin password!"
            onDone(false)
            return
        }
        transferManager(
            newEmail = newEmail,
            targetUid = null,
            newAdminPassword = newPassword,
            note = "Monthly Handover",
            onDone = onDone
        )
    }

    /**
     * Toggles between FIXED_DAILY (40 Tk / 25 Tk) and BAZAAR_RATE modes.
     * Admin only.
     */
    fun setCalculationMode(mode: String) {
        if (!requireAdminOrShowError()) return
        viewModelScope.launch {
            _isOperationLoading.value = true
            val messId = messState.value?.id?.ifBlank { MessRepository.DEFAULT_MESS_ID } ?: MessRepository.DEFAULT_MESS_ID
            val result = repository.updateCalculationMode(messId, mode)
            _isOperationLoading.value = false
            result.onSuccess {
                _userMessage.value = if (_currentLanguage.value == Language.BENGALI) {
                    if (mode == com.example.data.model.CALCULATION_MODE_FIXED_DAILY)
                        "⚡ হিসাব পদ্ধতি পরিবর্তন: ফিক্সড দৈনিক প্যাকেজ (৪০৳/২৫৳) সক্রিয়!"
                    else
                        "⚡ হিসাব পদ্ধতি পরিবর্তন: বাজারের হিসাব অনুযায়ী মিল রেট সক্রিয়!"
                } else {
                    "Calculation mode switched to $mode"
                }
            }.onFailure {
                _userMessage.value = "Failed: ${it.message}"
            }
        }
    }

    /**
     * Resets mess data with a guaranteed automatic backup snapshot.
     * Protected by Admin authorization.
     */
    fun resetMessData(
        adminPasswordInput: String,
        backupLabel: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (!requireAdminOrShowError()) {
            val errMsg = if (_currentLanguage.value == Language.BENGALI)
                "অননুমোদিত চেষ্টা! শুধুমাত্র সক্রিয় মেস ম্যানেজার ডাটা রিসেট করতে পারবেন।"
            else
                "Unauthorized! Only the active mess manager can reset data."
            _userMessage.value = errMsg
            onResult(false, errMsg)
            return
        }

        val mess = messState.value
        val messId = mess?.id?.ifBlank { MessRepository.DEFAULT_MESS_ID } ?: MessRepository.DEFAULT_MESS_ID
        val label = backupLabel.ifBlank { "Auto-Backup before Reset (${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())})" }

        viewModelScope.launch {
            _isOperationLoading.value = true
            if (adminPasswordInput.isNotBlank()) {
                val passRes = repository.verifyAdminPassword(messId, adminPasswordInput)
                if (passRes.isFailure) {
                    _isOperationLoading.value = false
                    val errorMsg = if (_currentLanguage.value == Language.BENGALI) "ভুল অ্যাডমিন পাসওয়ার্ড!" else "Invalid admin password!"
                    _userMessage.value = errorMsg
                    onResult(false, errorMsg)
                    return@launch
                }
            }
            val result = repository.resetMessDataWithBackup(messId, label)
            _isOperationLoading.value = false
            result.onSuccess {
                val successMsg = if (_currentLanguage.value == Language.BENGALI)
                    "সব ডাটা সফলভাবে রিসেট করা হয়েছে এবং ক্লাউড ব্যাকআপ সংরক্ষিত হয়েছে!"
                else
                    "Mess data successfully reset with cloud backup created!"
                _userMessage.value = successMsg
                onResult(true, successMsg)
            }.onFailure { e ->
                val errorMsg = if (_currentLanguage.value == Language.BENGALI)
                    "রিসেট ব্যর্থ হয়েছে: ${e.message}"
                else
                    "Reset failed: ${e.message}"
                _userMessage.value = errorMsg
                onResult(false, errorMsg)
            }
        }
    }

    /**
     * Restores all mess data from a previously saved backup snapshot.
     */
    fun restoreBackup(backupId: String, onResult: (Boolean, String) -> Unit) {
        if (!requireAdminOrShowError()) {
            onResult(false, "Admin authentication required")
            return
        }
        val messId = messState.value?.id?.ifBlank { MessRepository.DEFAULT_MESS_ID } ?: MessRepository.DEFAULT_MESS_ID

        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.restoreBackupSnapshot(messId, backupId)
            _isOperationLoading.value = false
            result.onSuccess {
                val successMsg = if (_currentLanguage.value == Language.BENGALI)
                    "ব্যাকআপ থেকে সকল ডাটা সফলভাবে রিস্টোর করা হয়েছে!"
                else
                    "All data successfully restored from backup snapshot!"
                _userMessage.value = successMsg
                onResult(true, successMsg)
            }.onFailure { e ->
                val errorMsg = if (_currentLanguage.value == Language.BENGALI)
                    "রিস্টোর ব্যর্থ হয়েছে: ${e.message}"
                else
                    "Restore failed: ${e.message}"
                _userMessage.value = errorMsg
                onResult(false, errorMsg)
            }
        }
    }

    /**
     * Creates a manual backup snapshot without wiping any data.
     */
    fun createManualBackup(label: String, onResult: (Boolean, String) -> Unit) {
        if (!requireAdminOrShowError()) {
            onResult(false, "Admin authentication required")
            return
        }
        val messId = messState.value?.id?.ifBlank { MessRepository.DEFAULT_MESS_ID } ?: MessRepository.DEFAULT_MESS_ID
        val cleanLabel = label.ifBlank { "Manual Backup (${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())})" }

        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.createBackupSnapshot(messId, cleanLabel, reason = "Manual manager backup")
            _isOperationLoading.value = false
            result.onSuccess {
                val successMsg = if (_currentLanguage.value == Language.BENGALI)
                    "ক্লাউড ব্যাকআপ সফলভাবে সংরক্ষিত হয়েছে!"
                else
                    "Cloud backup snapshot successfully saved!"
                _userMessage.value = successMsg
                onResult(true, successMsg)
            }.onFailure { e ->
                val errorMsg = if (_currentLanguage.value == Language.BENGALI)
                    "ব্যাকআপ সংরক্ষণ ব্যর্থ: ${e.message}"
                else
                    "Backup failed: ${e.message}"
                _userMessage.value = errorMsg
                onResult(false, errorMsg)
            }
        }
    }
}
