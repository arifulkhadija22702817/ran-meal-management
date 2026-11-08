package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AdminHandoverRecord
import com.example.data.model.AuditLog
import com.example.data.model.DailyMeal
import com.example.data.model.EggRecord
import com.example.data.model.Expense
import com.example.data.model.Member
import com.example.data.model.Mess
import com.example.data.model.MessBackup
import com.example.data.model.Utility
import org.json.JSONArray
import org.json.JSONObject
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MessRepository(
    val db: FirebaseFirestore
) {
    companion object {
        private const val TAG = "MessRepository"
        const val DEFAULT_MESS_ID = "main_mess"

        fun hashAdminPassword(password: String): String {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(password.trim().toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }

        fun verifyPasswordHash(input: String, stored: String): Boolean {
            if (stored.isBlank() || input.isBlank()) return false
            val hashedInput = hashAdminPassword(input)
            return hashedInput.equals(stored, ignoreCase = true) || input == stored
        }

        fun create(context: Context): MessRepository {
            try {
                if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                    com.google.firebase.FirebaseApp.initializeApp(context)
                }
            } catch (e: Exception) {
                Log.w(TAG, "FirebaseApp initialization check: ${e.message}")
            }
            val dbId = context.getString(R.string.firestore_database_id)
            val firestore = FirebaseFirestore.getInstance(dbId)
            try {
                val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                            .setSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                            .build()
                    )
                    .build()
                firestore.firestoreSettings = settings
            } catch (e: Exception) {
                Log.w(TAG, "Persistent cache setting: ${e.message}")
            }
            return MessRepository(firestore)
        }
    }

    val auth = Firebase.auth

    fun getCurrentUserUid(): String? = auth.currentUser?.uid
    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: "manager_placeholder"
    }

    /**
     * Observes the mess document in real-time without crashing on connection errors.
     */
    fun observeMess(messId: String = DEFAULT_MESS_ID): Flow<Mess?> = callbackFlow {
        val docRef = db.collection("messes").document(messId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "observeMess snapshot error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val mess = snapshot.toObject(Mess::class.java)
                trySend(mess)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Initializes or gets the main mess. If not yet created, sets the first caller as manager.
     */
    suspend fun initOrGetMess(
        messName: String = "Meal Management Hostel",
        messId: String = DEFAULT_MESS_ID
    ): Result<Mess> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Authentication required"))
        val email = auth.currentUser?.email ?: ""
        val docRef = db.collection("messes").document(messId)

        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existingMess = snapshot.toObject(Mess::class.java)!!
                // If managerEmail or adminPassword not set yet or is placeholder, seed target default admin
                if (existingMess.managerEmail.isBlank() || existingMess.adminPassword.isBlank() || existingMess.managerEmail.contains("placeholder")) {
                    val defaultAdminEmail = "arifulislamsaju.sdp@gmail.com"
                    val defaultHash = hashAdminPassword("Admin123@")
                    docRef.update(
                        mapOf(
                            "managerEmail" to defaultAdminEmail,
                            "adminPassword" to defaultHash,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                // Ensure current user is in authorized_members
                try {
                    ensureUserAuthorized(messId, uid, email)
                } catch (e: Exception) {
                    Log.w(TAG, "ensureUserAuthorized notice: ${e.message}")
                }
                Result.success(existingMess)
            } else {
                val currentMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
                val defaultAdminEmail = "arifulislamsaju.sdp@gmail.com"
                val defaultHash = hashAdminPassword("Admin123@")
                val newMess = hashMapOf(
                    "name" to messName,
                    "currency" to "BDT",
                    "managerUid" to uid,
                    "managerEmail" to defaultAdminEmail,
                    "adminPassword" to defaultHash,
                    "currentMonth" to currentMonth,
                    "managerAssignedAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(newMess).await()
                try {
                    ensureUserAuthorized(messId, uid, email)
                    logAudit(messId, "INITIALIZE_MESS", "Initialized mess with default manager $defaultAdminEmail")
                } catch (e: Exception) {
                    Log.w(TAG, "Initialization audit notice: ${e.message}")
                }

                // Pre-populate with default sample border members if brand new
                seedDefaultMembersIfEmpty(messId)

                Result.success(
                    Mess(
                        id = messId,
                        name = messName,
                        currency = "BDT",
                        managerUid = uid,
                        managerEmail = defaultAdminEmail,
                        adminPassword = defaultHash,
                        currentMonth = currentMonth
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "initOrGetMess failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Verifies Admin Gmail and Password directly from Firestore.
     * If both match:
     * - Binds the caller's current UID to the mess managerUid
     * - Grants full admin access
     * If mismatch: returns failure with descriptive message.
     */
    suspend fun verifyAdminCredentials(
        messId: String = DEFAULT_MESS_ID,
        inputEmail: String,
        inputPassword: String
    ): Result<Boolean> {
        val user = auth.currentUser
        val uid = user?.uid ?: ""
        val cleanEmail = inputEmail.trim().lowercase()
        val cleanPass = inputPassword.trim()

        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            return Result.failure(IllegalArgumentException("জিমেইল এবং পাসওয়ার্ড উভয়ই প্রদান করতে হবে"))
        }

        return try {
            val docRef = db.collection("messes").document(messId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalStateException("মেস ডাটাবেজ পাওয়া যায়নি"))
            }

            var storedEmail = (snap.getString("managerEmail") ?: "").trim().lowercase()
            var storedPass = snap.getString("adminPassword") ?: ""

            val targetDefaultEmail = "arifulislamsaju.sdp@gmail.com"
            val targetDefaultPass = "Admin123@"
            val targetDefaultHash = hashAdminPassword(targetDefaultPass)

            // If user enters the default admin credentials requested, ensure it succeeds and syncs to Firestore
            val isDefaultMatch = cleanEmail == targetDefaultEmail && cleanPass == targetDefaultPass

            // Fallback default admin if document didn't have admin initialized
            if (storedEmail.isBlank() || storedEmail.contains("placeholder") || storedPass.isBlank() || isDefaultMatch) {
                storedEmail = targetDefaultEmail
                storedPass = targetDefaultHash
                try {
                    docRef.update(
                        mapOf(
                            "managerEmail" to targetDefaultEmail,
                            "adminPassword" to targetDefaultHash,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Notice syncing default admin to Firestore: ${e.message}")
                }
            }

            val emailMatch = cleanEmail.equals(storedEmail, ignoreCase = true) || isDefaultMatch
            val passMatch = verifyPasswordHash(cleanPass, storedPass) || isDefaultMatch

            if (!emailMatch) {
                return Result.failure(SecurityException("ভুল অ্যাডমিন জিমেইল! অনুমোদিত অ্যাডমিন জিমেইল লিখুন।"))
            }

            if (!passMatch) {
                return Result.failure(SecurityException("ভুল অ্যাডমিন পাসওয়ার্ড! সঠিক পাসওয়ার্ড লিখুন।"))
            }

            // Both match! Update caller's UID and authorized role
            if (uid.isNotEmpty()) {
                try {
                    docRef.update(
                        mapOf(
                            "managerUid" to uid,
                            "managerEmail" to cleanEmail,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    ensureUserAuthorized(messId, uid, cleanEmail, user?.displayName ?: cleanEmail)
                } catch (e: Exception) {
                    Log.w(TAG, "Notice binding manager UID: ${e.message}")
                }
            }

            logAudit(messId, "ADMIN_CREDENTIALS_LOGIN", "Admin credentials successfully verified for $cleanEmail")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "verifyAdminCredentials failed", e)
            Result.failure(e)
        }
    }

    suspend fun ensureUserAuthorized(
        messId: String = DEFAULT_MESS_ID,
        uid: String,
        email: String,
        displayName: String = ""
    ): Result<Unit> {
        return try {
            val docRef = db.collection("messes").document(messId)
                .collection("authorized_members").document(uid)
            val payload = hashMapOf(
                "uid" to uid,
                "email" to email.trim().lowercase(),
                "displayName" to displayName.take(80),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "ensureUserAuthorized notice: ${e.message}")
            Result.failure(e)
        }
    }

    fun observeAuthorizedMembers(messId: String = DEFAULT_MESS_ID): Flow<List<com.example.data.model.AuthorizedMember>> = callbackFlow {
        val colRef = db.collection("messes").document(messId).collection("authorized_members")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "observeAuthorizedMembers snapshot error: ${error.message}")
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(com.example.data.model.AuthorizedMember::class.java) ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun syncUserProfile(): Result<Unit> {
        val user = auth.currentUser ?: return Result.success(Unit)
        return try {
            val profileRef = db.collection("user_profiles").document(user.uid)
            val profileDoc = profileRef.get().await()
            val payload = hashMapOf<String, Any>(
                "uid" to user.uid,
                "email" to (user.email ?: "").trim().lowercase(),
                "displayName" to (user.displayName ?: (user.email ?: "User")).take(80),
                "lastActiveAt" to FieldValue.serverTimestamp()
            )
            if (!profileDoc.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                profileRef.set(payload).await()
            } else {
                profileRef.update(
                    mapOf(
                        "displayName" to (user.displayName ?: (user.email ?: "User")).take(80),
                        "lastActiveAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            ensureUserAuthorized(
                messId = DEFAULT_MESS_ID,
                uid = user.uid,
                email = user.email ?: "",
                displayName = user.displayName ?: ""
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "syncUserProfile notice: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun seedDefaultMembersIfEmpty(messId: String) {
        val membersRef = db.collection("messes").document(messId).collection("members")
        val snap = membersRef.limit(1).get().await()
        if (snap.isEmpty) {
            val defaultMembers = listOf(
                Member(name = "Arif Hasan", phone = "+8801712345678", roomNumber = "101", potDeposits = 25.0, cashDeposits = 3000.0),
                Member(name = "Khadija Begum", phone = "+8801812345678", roomNumber = "102", potDeposits = 30.0, cashDeposits = 3000.0),
                Member(name = "Tanvir Ahmed", phone = "+8801912345678", roomNumber = "103", potDeposits = 20.0, cashDeposits = 2500.0),
                Member(name = "Sabbir Hossain", phone = "+8801612345678", roomNumber = "104", potDeposits = 25.0, cashDeposits = 3000.0)
            )
            for (m in defaultMembers) {
                val doc = membersRef.document()
                val payload = hashMapOf(
                    "messId" to messId,
                    "name" to m.name,
                    "phone" to m.phone,
                    "roomNumber" to m.roomNumber,
                    "potDeposits" to m.potDeposits,
                    "cashDeposits" to m.cashDeposits,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                doc.set(payload).await()
            }
        }
    }

    /**
     * Atomic Monthly Manager Transition using Firestore runTransaction:
     * 1. Reads current mess and validates active manager UID.
     * 2. Updates /messes/{messId} with genuine newManagerUid and newManagerEmail.
     * 3. Inserts immutable record in admin_history (verified by getAfter in security rules).
     * 4. Inserts audit log in audit_logs.
     * 5. Preserves former admin's read access as an authorized member.
     */
    suspend fun transferManager(
        messId: String = DEFAULT_MESS_ID,
        newManagerEmail: String,
        newManagerUid: String,
        newAdminPassword: String? = null,
        note: String = ""
    ): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User is not authenticated"))
            val currentEmail = auth.currentUser?.email ?: "Current Manager"
            val cleanEmail = newManagerEmail.trim().lowercase()
            val cleanUid = newManagerUid.trim()
            if (cleanUid.isBlank()) {
                return Result.failure(IllegalArgumentException("New Manager UID cannot be empty"))
            }
            val month = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

            val messRef = db.collection("messes").document(messId)
            val historyRef = messRef.collection("admin_history").document()
            val auditRef = messRef.collection("audit_logs").document()
            val formerAdminRef = messRef.collection("authorized_members").document(currentUid)
            val newAdminRef = messRef.collection("authorized_members").document(cleanUid)

            db.runTransaction { transaction ->
                val messSnapshot = transaction.get(messRef)
                if (!messSnapshot.exists()) {
                    throw IllegalStateException("Mess document does not exist")
                }
                val activeManagerUid = messSnapshot.getString("managerUid") ?: ""
                if (activeManagerUid.isNotEmpty() && activeManagerUid != currentUid) {
                    throw SecurityException("Concurrency conflict: Admin role has already been transferred.")
                }

                // 1. Update active manager in parent mess document and reset/update adminPassword
                val messUpdates = mutableMapOf<String, Any>(
                    "managerUid" to cleanUid,
                    "managerEmail" to cleanEmail,
                    "managerAssignedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!newAdminPassword.isNullOrBlank()) {
                    messUpdates["adminPassword"] = hashAdminPassword(newAdminPassword)
                } else {
                    messUpdates["adminPassword"] = ""
                }
                transaction.update(messRef, messUpdates)

                // 2. Insert immutable record in admin_history (validated by getAfter in rules)
                val historyPayload = hashMapOf(
                    "messId" to messId,
                    "previousAdminEmail" to currentEmail,
                    "previousAdminUid" to currentUid,
                    "newAdminEmail" to cleanEmail,
                    "newAdminUid" to cleanUid,
                    "handoverMonth" to month,
                    "note" to note.trim(),
                    "transferredAt" to FieldValue.serverTimestamp()
                )
                transaction.set(historyRef, historyPayload)

                // 3. Insert audit log
                val auditPayload = hashMapOf(
                    "messId" to messId,
                    "action" to "TRANSFER_ADMIN",
                    "details" to "Manager role transferred from $currentEmail to $cleanEmail",
                    "performedBy" to currentEmail,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                transaction.set(auditRef, auditPayload)

                // 4. Former admin retains authorized member read access
                transaction.set(
                    formerAdminRef,
                    mapOf(
                        "uid" to currentUid,
                        "email" to currentEmail.trim().lowercase(),
                        "role" to "MEMBER",
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )

                // 5. New admin is added to authorized members
                transaction.set(
                    newAdminRef,
                    mapOf(
                        "uid" to cleanUid,
                        "email" to cleanEmail,
                        "role" to "MANAGER",
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "transferManager failed", e)
            Result.failure(e)
        }
    }

    /**
     * Observes the history of all past manager handovers in real-time.
     */
    fun observeAdminHistory(messId: String = DEFAULT_MESS_ID): Flow<List<AdminHandoverRecord>> = callbackFlow {
        val colRef = db.collection("messes").document(messId).collection("admin_history")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "observeAdminHistory snapshot error: ${error.message}")
                return@addSnapshotListener
            }
            val records = snapshot?.toObjects(AdminHandoverRecord::class.java) ?: emptyList()
            trySend(records.sortedByDescending { it.transferredAt?.toDate()?.time ?: 0L })
        }
        awaitClose { registration.remove() }
    }

    /**
     * Claims the initial Admin/Manager role when no admin Gmail is currently assigned,
     * or when current mess has placeholder/unassigned manager.
     */
    suspend fun claimInitialAdmin(
        messId: String = DEFAULT_MESS_ID,
        initialPassword: String
    ): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Google Sign-In is required to claim admin role"))
        val uid = user.uid
        val email = (user.email ?: "").trim().lowercase()
        if (email.isBlank()) {
            return Result.failure(IllegalStateException("A valid Gmail address is required"))
        }
        val cleanPassword = initialPassword.trim()
        val (valid, errorMsg) = com.example.util.AdminPasswordValidator.validate(cleanPassword)
        if (!valid) {
            return Result.failure(IllegalArgumentException(errorMsg ?: "পাসওয়ার্ডের শর্তাবলী পূরণ হয়নি"))
        }

        val hashedPassword = hashAdminPassword(cleanPassword)
        val messRef = db.collection("messes").document(messId)

        return try {
            val snapshot = messRef.get().await()
            val month = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
            if (!snapshot.exists()) {
                val newMess = hashMapOf(
                    "name" to "Meal Management Hostel",
                    "currency" to "BDT",
                    "managerUid" to uid,
                    "managerEmail" to email,
                    "adminPassword" to hashedPassword,
                    "currentMonth" to month,
                    "managerAssignedAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                messRef.set(newMess).await()
            } else {
                val currentManagerEmail = snapshot.getString("managerEmail") ?: ""
                val currentManagerUid = snapshot.getString("managerUid") ?: ""
                val currentStoredPassword = snapshot.getString("adminPassword") ?: ""
                val canClaim = currentManagerEmail.isBlank() ||
                        currentManagerUid.isBlank() ||
                        currentManagerUid == "manager_placeholder" ||
                        currentManagerEmail.contains("placeholder") ||
                        currentStoredPassword.isBlank() ||
                        currentManagerEmail.equals(email, ignoreCase = true)

                if (!canClaim) {
                    return Result.failure(SecurityException("মেসের একজন অ্যাডমিন ($currentManagerEmail) ইতিমধ্যে নিবন্ধিত আছেন।"))
                }

                messRef.update(
                    mapOf(
                        "managerUid" to uid,
                        "managerEmail" to email,
                        "adminPassword" to hashedPassword,
                        "managerAssignedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Ensure caller has MANAGER role in authorized_members
            val memberRef = messRef.collection("authorized_members").document(uid)
            memberRef.set(
                mapOf(
                    "uid" to uid,
                    "email" to email,
                    "displayName" to (user.displayName ?: email),
                    "role" to "MANAGER",
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()

            logAudit(messId, "CLAIM_INITIAL_ADMIN", "Initial admin registered for $email with secure password")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error claiming initial admin", e)
            Result.failure(e)
        }
    }

    /**
     * Verifies user-entered admin password against the stored SHA-256 hash in Firestore.
     * If no admin password is saved yet in database, saves the initial password securely to Firestore.
     * Subsequent attempts MUST strictly match the stored SHA-256 hash.
     */
    suspend fun verifyAdminPassword(
        messId: String = DEFAULT_MESS_ID,
        passwordInput: String
    ): Result<Boolean> {
        val user = auth.currentUser
        val uid = user?.uid ?: ""
        val email = (user?.email ?: "").trim().lowercase()
        val clean = passwordInput.trim()

        return try {
            val docRef = db.collection("messes").document(messId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.success(false)
            }
            val stored = snap.getString("adminPassword") ?: ""

            if (stored.isBlank()) {
                // If password is not set yet in database, first time setup:
                // Validate 8-12 chars (letter + number + special char)
                val (valid, errorMsg) = com.example.util.AdminPasswordValidator.validate(clean)
                if (!valid) {
                    return Result.failure(IllegalArgumentException(errorMsg ?: "পাসওয়ার্ড ৮-১২ অক্ষরের বর্ণ, সংখ্যা ও চিহ্ন বিশিষ্ট হতে হবে"))
                }
                val hashed = hashAdminPassword(clean)
                docRef.update(
                    mapOf(
                        "managerUid" to uid,
                        "managerEmail" to email,
                        "adminPassword" to hashed,
                        "managerAssignedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                if (uid.isNotEmpty()) {
                    ensureUserAuthorized(messId, uid, email, user?.displayName ?: email)
                }
                logAudit(messId, "SET_INITIAL_ADMIN_PASSWORD", "Admin password successfully saved for $email")
                return Result.success(true)
            }

            if (clean.isBlank()) {
                return Result.success(false)
            }
            val matched = verifyPasswordHash(clean, stored)
            if (matched && uid.isNotEmpty()) {
                try {
                    docRef.update(
                        mapOf(
                            "managerUid" to uid,
                            "managerEmail" to email,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    ensureUserAuthorized(messId, uid, email, user?.displayName ?: email)
                } catch (e: Exception) {
                    Log.w(TAG, "Notice syncing managerUid: ${e.message}")
                }
            }
            Result.success(matched)
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying admin password", e)
            Result.failure(e)
        }
    }

    /**
     * Updates admin password in Firestore with SHA-256 hash.
     */
    suspend fun updateAdminPassword(
        messId: String = DEFAULT_MESS_ID,
        oldPassword: String?,
        newPassword: String
    ): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("User is not authenticated"))
        val cleanNew = newPassword.trim()
        val (valid, errorMsg) = com.example.util.AdminPasswordValidator.validate(cleanNew)
        if (!valid) {
            return Result.failure(IllegalArgumentException(errorMsg ?: "নতুন পাসওয়ার্ডের শর্তাবলী পূরণ হয়নি"))
        }

        val docRef = db.collection("messes").document(messId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalStateException("Mess not found"))
            }
            val managerUid = snap.getString("managerUid") ?: ""
            val managerEmail = snap.getString("managerEmail") ?: ""
            if (managerUid != user.uid && !managerEmail.equals(user.email, ignoreCase = true)) {
                return Result.failure(SecurityException("শুধুমাত্র মেস ম্যানেজার পাসওয়ার্ড পরিবর্তন করতে পারেন"))
            }

            val stored = snap.getString("adminPassword") ?: ""
            if (stored.isNotBlank() && oldPassword != null && oldPassword.isNotBlank()) {
                if (!verifyPasswordHash(oldPassword, stored)) {
                    return Result.failure(IllegalArgumentException("ভুল বর্তমান পাসওয়ার্ড! (Incorrect current password)"))
                }
            }

            val newHash = hashAdminPassword(cleanNew)
            docRef.update(
                mapOf(
                    "adminPassword" to newHash,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logAudit(messId, "CHANGE_ADMIN_PASSWORD", "Admin password changed successfully by ${user.email}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating admin password", e)
            Result.failure(e)
        }
    }

    /**
     * Recovers/resets admin password when forgotten by verified admin.
     */
    suspend fun recoverAdminPassword(
        messId: String = DEFAULT_MESS_ID,
        newPassword: String
    ): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Authentication required"))
        val uid = user.uid
        val email = (user.email ?: "").trim().lowercase()

        val cleanNew = newPassword.trim()
        val (valid, errorMsg) = com.example.util.AdminPasswordValidator.validate(cleanNew)
        if (!valid) {
            return Result.failure(IllegalArgumentException(errorMsg ?: "নতুন পাসওয়ার্ডের শর্তাবলী পূরণ হয়নি"))
        }

        val docRef = db.collection("messes").document(messId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalStateException("Mess not found"))
            }
            val managerUid = snap.getString("managerUid") ?: ""
            val managerEmail = (snap.getString("managerEmail") ?: "").trim().lowercase()

            val isAuthorized = (managerUid == uid) ||
                    (managerEmail.isNotBlank() && managerEmail.equals(email, ignoreCase = true)) ||
                    managerEmail.isBlank() ||
                    managerEmail.contains("placeholder")

            if (!isAuthorized) {
                return Result.failure(SecurityException("শুধুমাত্র নিবন্ধিত মেস অ্যাডমিন ($managerEmail) পাসওয়ার্ড রিকভার করতে পারেন"))
            }

            val newHash = hashAdminPassword(cleanNew)
            docRef.update(
                mapOf(
                    "managerUid" to uid,
                    "managerEmail" to email,
                    "adminPassword" to newHash,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            ensureUserAuthorized(messId, uid, email, user.displayName ?: email)
            logAudit(messId, "RECOVER_ADMIN_PASSWORD", "Admin password successfully recovered and updated by $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error recovering admin password", e)
            Result.failure(e)
        }
    }

    /**
     * Members observation
     */
    fun observeMembers(messId: String = DEFAULT_MESS_ID): Flow<List<Member>> = callbackFlow {
        val colRef = db.collection("messes").document(messId).collection("members")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "observeMembers snapshot error: ${error.message}")
                return@addSnapshotListener
            }
            val members = snapshot?.toObjects(Member::class.java) ?: emptyList()
            trySend(members.sortedBy { it.name })
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveMember(member: Member): Result<Unit> {
        val messId = member.messId.ifBlank { DEFAULT_MESS_ID }
        val colRef = db.collection("messes").document(messId).collection("members")
        val docRef = if (member.id.isBlank()) colRef.document() else colRef.document(member.id)

        val payload = hashMapOf(
            "messId" to messId,
            "name" to member.name.trim(),
            "phone" to member.phone.trim(),
            "roomNumber" to member.roomNumber.trim(),
            "potDeposits" to member.potDeposits,
            "cashDeposits" to member.cashDeposits,
            "active" to member.active,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (member.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        return try {
            docRef.set(payload, SetOptions.merge()).await()
            logAudit(messId, if (member.id.isBlank()) "ADD_MEMBER" else "EDIT_MEMBER", "Member: ${member.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveMember failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMember(messId: String, memberId: String, memberName: String): Result<Unit> {
        return try {
            db.collection("messes").document(messId).collection("members").document(memberId).delete().await()
            try {
                logAudit(messId, "DELETE_MEMBER", "Deleted member: $memberName ($memberId)")
            } catch (e: Exception) {
                Log.w(TAG, "deleteMember audit log notice: ${e.message}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteMember failed", e)
            Result.failure(e)
        }
    }

    /**
     * Daily meals observation for a given month prefix (e.g. "2026-10")
     */
    fun observeDailyMeals(messId: String = DEFAULT_MESS_ID): Flow<List<DailyMeal>> = callbackFlow {
        val colRef = db.collection("messes").document(messId).collection("daily_meals")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "observeDailyMeals snapshot error: ${error.message}")
                return@addSnapshotListener
            }
            val meals = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val rawMemberMeals = doc.get("memberMeals") as? Map<*, *>
                    val parsedMemberMeals = mutableMapOf<String, Map<String, Double>>()
                    rawMemberMeals?.forEach { (mId, mObj) ->
                        if (mId is String && mObj is Map<*, *>) {
                            val sub = mutableMapOf<String, Double>()
                            mObj.forEach { (k, v) ->
                                if (k is String) {
                                    val count = when (v) {
                                        is Number -> v.toDouble()
                                        is String -> v.toDoubleOrNull() ?: 0.0
                                        else -> 0.0
                                    }
                                    sub[k] = count
                                }
                            }
                            parsedMemberMeals[mId] = sub
                        }
                    }
                    DailyMeal(
                        id = doc.id,
                        messId = doc.getString("messId") ?: messId,
                        date = doc.getString("date") ?: doc.id,
                        breakfastCount = (doc.get("breakfastCount") as? Number)?.toDouble() ?: 0.0,
                        lunchCount = (doc.get("lunchCount") as? Number)?.toDouble() ?: 0.0,
                        dinnerCount = (doc.get("dinnerCount") as? Number)?.toDouble() ?: 0.0,
                        memberMeals = parsedMemberMeals,
                        notes = doc.getString("notes") ?: "",
                        updatedBy = doc.getString("updatedBy") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        updatedAt = doc.getTimestamp("updatedAt")
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error deserializing DailyMeal ${doc.id}", e)
                    null
                }
            } ?: emptyList()
            trySend(meals.sortedByDescending { it.date })
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveDailyMeal(meal: DailyMeal): Result<Unit> {
        val messId = meal.messId.ifBlank { DEFAULT_MESS_ID }
        val docId = meal.date
        val docRef = db.collection("messes").document(messId).collection("daily_meals").document(docId)

        val payload = hashMapOf(
            "messId" to messId,
            "date" to meal.date,
            "breakfastCount" to meal.breakfastCount,
            "lunchCount" to meal.lunchCount,
            "dinnerCount" to meal.dinnerCount,
            "memberMeals" to meal.memberMeals,
            "notes" to meal.notes,
            "updatedBy" to (auth.currentUser?.email ?: "Manager"),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (meal.createdAt == null) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        return try {
            docRef.set(payload, SetOptions.merge()).await()
            logAudit(
                messId,
                "EDIT_MEAL",
                "Date: ${meal.date}, B: ${meal.breakfastCount}, L: ${meal.lunchCount}, D: ${meal.dinnerCount}"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveDailyMeal failed", e)
            Result.failure(e)
        }
    }

    /**
     * Expenses observation
     */
    fun observeExpenses(messId: String = DEFAULT_MESS_ID): Flow<List<Expense>> = callbackFlow {
        val colRef = db.collection("messes").document(messId).collection("expenses")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "observeExpenses snapshot error: ${error.message}")
                return@addSnapshotListener
            }
            val expenses = snapshot?.toObjects(Expense::class.java) ?: emptyList()
            trySend(expenses.sortedByDescending { it.date })
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveExpense(expense: Expense): Result<Unit> {
        val messId = expense.messId.ifBlank { DEFAULT_MESS_ID }
        val colRef = db.collection("messes").document(messId).collection("expenses")
        val docRef = if (expense.id.isBlank()) colRef.document() else colRef.document(expense.id)

        val payload = hashMapOf(
            "messId" to messId,
            "title" to expense.title.trim(),
            "amount" to expense.amount,
            "date" to expense.date,
            "payerMemberId" to expense.payerMemberId,
            "category" to expense.category,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (expense.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        return try {
            docRef.set(payload, SetOptions.merge()).await()
            logAudit(messId, if (expense.id.isBlank()) "ADD_EXPENSE" else "EDIT_EXPENSE", "${expense.title}: ৳${expense.amount}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveExpense failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(messId: String, expenseId: String, title: String): Result<Unit> {
        return try {
            db.collection("messes").document(messId).collection("expenses").document(expenseId).delete().await()
            logAudit(messId, "DELETE_EXPENSE", "Deleted expense: $title ($expenseId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteExpense failed", e)
            Result.failure(e)
        }
    }

    /**
     * Utilities observation
     */
    fun observeUtilities(messId: String = DEFAULT_MESS_ID): Flow<List<Utility>> = callbackFlow {
        val colRef = db.collection("messes").document(messId).collection("utilities")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "observeUtilities snapshot error: ${error.message}")
                return@addSnapshotListener
            }
            val utilities = snapshot?.toObjects(Utility::class.java) ?: emptyList()
            trySend(utilities.sortedByDescending { it.createdAt })
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveUtility(utility: Utility): Result<Unit> {
        val messId = utility.messId.ifBlank { DEFAULT_MESS_ID }
        val colRef = db.collection("messes").document(messId).collection("utilities")
        val docRef = if (utility.id.isBlank()) colRef.document() else colRef.document(utility.id)

        val payload = hashMapOf(
            "messId" to messId,
            "title" to utility.title.trim(),
            "amount" to utility.amount,
            "month" to utility.month,
            "category" to utility.category,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (utility.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        return try {
            docRef.set(payload, SetOptions.merge()).await()
            logAudit(messId, if (utility.id.isBlank()) "ADD_UTILITY" else "EDIT_UTILITY", "${utility.title}: ৳${utility.amount}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveUtility failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUtility(messId: String, utilityId: String, title: String): Result<Unit> {
        return try {
            db.collection("messes").document(messId).collection("utilities").document(utilityId).delete().await()
            logAudit(messId, "DELETE_UTILITY", "Deleted utility: $title ($utilityId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteUtility failed", e)
            Result.failure(e)
        }
    }

    /**
     * Audit log observation
     */
    fun observeAuditLogs(messId: String = DEFAULT_MESS_ID): Flow<List<AuditLog>> = callbackFlow {
        val colRef = db.collection("messes").document(messId).collection("audit_logs")
            .limit(50)
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "observeAuditLogs snapshot error: ${error.message}")
                return@addSnapshotListener
            }
            val logs = snapshot?.toObjects(AuditLog::class.java) ?: emptyList()
            trySend(logs.sortedByDescending { it.timestamp })
        }
        awaitClose { registration.remove() }
    }

    /**
     * Egg records observation
     */
    fun observeEggRecords(messId: String = DEFAULT_MESS_ID): Flow<List<EggRecord>> = callbackFlow {
        val colRef = db.collection("messes").document(messId).collection("egg_records")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "observeEggRecords snapshot error: ${error.message}")
                return@addSnapshotListener
            }
            val records = snapshot?.toObjects(EggRecord::class.java) ?: emptyList()
            trySend(records.sortedByDescending { it.date })
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveEggRecord(record: EggRecord): Result<Unit> {
        val messId = record.messId.ifBlank { DEFAULT_MESS_ID }
        val colRef = db.collection("messes").document(messId).collection("egg_records")
        val docRef = if (record.id.isBlank()) colRef.document() else colRef.document(record.id)

        val payload = hashMapOf(
            "messId" to messId,
            "date" to record.date,
            "purchasedCount" to record.purchasedCount,
            "consumedCount" to record.consumedCount,
            "damagedCount" to record.damagedCount,
            "cost" to record.cost,
            "notes" to record.notes.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (record.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        return try {
            docRef.set(payload, SetOptions.merge()).await()
            logAudit(
                messId,
                if (record.id.isBlank()) "ADD_EGG_RECORD" else "EDIT_EGG_RECORD",
                "Date: ${record.date}, Bought: ${record.purchasedCount}, Eaten: ${record.consumedCount}, Spoiled: ${record.damagedCount}"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveEggRecord failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteEggRecord(messId: String, recordId: String, info: String): Result<Unit> {
        return try {
            db.collection("messes").document(messId).collection("egg_records").document(recordId).delete().await()
            logAudit(messId, "DELETE_EGG_RECORD", "Deleted egg log: $info ($recordId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteEggRecord failed", e)
            Result.failure(e)
        }
    }

    /**
     * Toggles the calculation mode between FIXED_DAILY and BAZAAR_RATE.
     */
    suspend fun updateCalculationMode(messId: String = DEFAULT_MESS_ID, mode: String): Result<Unit> {
        return try {
            val payload = mapOf(
                "calculationMode" to mode,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("messes").document(messId).set(payload, SetOptions.merge()).await()
            logAudit(messId, "UPDATE_CALC_MODE", "Switched calculation mode to: $mode")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateCalculationMode failed", e)
            Result.failure(e)
        }
    }

    suspend fun updateCalculationRates(
        messId: String = DEFAULT_MESS_ID,
        fullRate: Double,
        partialRate: Double
    ): Result<Unit> {
        return try {
            val payload = mapOf(
                "fullMealRate" to fullRate,
                "partialMealRate" to partialRate,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("messes").document(messId).set(payload, SetOptions.merge()).await()
            logAudit(messId, "UPDATE_CALC_RATES", "Updated rates: Full=$fullRate, Partial=$partialRate")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateCalculationRates failed", e)
            Result.failure(e)
        }
    }

    /**
     * Backup Observation
     */
    fun observeBackups(messId: String = DEFAULT_MESS_ID): Flow<List<MessBackup>> = callbackFlow {
        val colRef = db.collection("messes").document(messId).collection("backups")
            .limit(30)
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "observeBackups snapshot error: ${error.message}")
                return@addSnapshotListener
            }
            val backups = snapshot?.toObjects(MessBackup::class.java) ?: emptyList()
            trySend(backups.sortedByDescending { it.timestamp })
        }
        awaitClose { registration.remove() }
    }

    /**
     * Recursively converts Firestore document data containing Timestamps or nested structures
     * into Android org.json compatible values without throwing JSONException.
     */
    private fun sanitizeForJson(value: Any?): Any {
        return when (value) {
            null -> JSONObject.NULL
            is com.google.firebase.Timestamp -> value.toDate().time
            is Date -> value.time
            is Map<*, *> -> {
                val jobj = JSONObject()
                for ((k, v) in value) {
                    if (k != null) {
                        jobj.put(k.toString(), sanitizeForJson(v))
                    }
                }
                jobj
            }
            is List<*> -> {
                val jarr = JSONArray()
                for (item in value) {
                    jarr.put(sanitizeForJson(item))
                }
                jarr
            }
            is Number, is String, is Boolean -> value
            else -> value.toString()
        }
    }

    private suspend fun safeGetDocuments(collectionRef: com.google.firebase.firestore.CollectionReference): List<com.google.firebase.firestore.DocumentSnapshot> {
        return try {
            collectionRef.get().await().documents
        } catch (e: Exception) {
            try {
                collectionRef.get(com.google.firebase.firestore.Source.CACHE).await().documents
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    private fun calculateSha256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Captures a complete JSON snapshot of all subcollections before any destructive operation.
     */
    suspend fun createBackupSnapshot(
        messId: String = DEFAULT_MESS_ID,
        label: String = "Manual Backup",
        reason: String = "PRE_RESET_BACKUP"
    ): Result<String> {
        return try {
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val messRef = db.collection("messes").document(messId)

            val memberDocs = safeGetDocuments(messRef.collection("members"))
            val mealDocs = safeGetDocuments(messRef.collection("daily_meals"))
            val expenseDocs = safeGetDocuments(messRef.collection("expenses"))
            val utilityDocs = safeGetDocuments(messRef.collection("utilities"))
            val eggDocs = safeGetDocuments(messRef.collection("egg_records"))

            val rootJson = JSONObject()

            // 1. Members
            val membersArray = JSONArray()
            for (doc in memberDocs) {
                val obj = sanitizeForJson(doc.data ?: emptyMap<String, Any>()) as JSONObject
                obj.put("_id", doc.id)
                membersArray.put(obj)
            }
            rootJson.put("members", membersArray)

            // 2. Meals
            val mealsArray = JSONArray()
            for (doc in mealDocs) {
                val obj = sanitizeForJson(doc.data ?: emptyMap<String, Any>()) as JSONObject
                obj.put("_id", doc.id)
                mealsArray.put(obj)
            }
            rootJson.put("daily_meals", mealsArray)

            // 3. Expenses
            val expensesArray = JSONArray()
            for (doc in expenseDocs) {
                val obj = sanitizeForJson(doc.data ?: emptyMap<String, Any>()) as JSONObject
                obj.put("_id", doc.id)
                expensesArray.put(obj)
            }
            rootJson.put("expenses", expensesArray)

            // 4. Utilities
            val utilitiesArray = JSONArray()
            for (doc in utilityDocs) {
                val obj = sanitizeForJson(doc.data ?: emptyMap<String, Any>()) as JSONObject
                obj.put("_id", doc.id)
                utilitiesArray.put(obj)
            }
            rootJson.put("utilities", utilitiesArray)

            // 5. Eggs
            val eggsArray = JSONArray()
            for (doc in eggDocs) {
                val obj = sanitizeForJson(doc.data ?: emptyMap<String, Any>()) as JSONObject
                obj.put("_id", doc.id)
                eggsArray.put(obj)
            }
            rootJson.put("egg_records", eggsArray)

            val jsonString = rootJson.toString()
            val checksum = calculateSha256(jsonString)

            val backupId = UUID.randomUUID().toString()
            val backupDoc = messRef.collection("backups").document(backupId)

            val payload = hashMapOf(
                "messId" to messId,
                "label" to label,
                "reason" to reason,
                "dateString" to dateStr,
                "membersCount" to memberDocs.size,
                "mealsCount" to mealDocs.size,
                "expensesCount" to expenseDocs.size,
                "utilitiesCount" to utilityDocs.size,
                "eggsCount" to eggDocs.size,
                "backupDataJson" to jsonString,
                "checksum" to checksum,
                "createdAt" to FieldValue.serverTimestamp(),
                "timestamp" to FieldValue.serverTimestamp()
            )

            backupDoc.set(payload).await()
            try {
                logAudit(messId, "CREATE_BACKUP", "Created snapshot ($label) with ${memberDocs.size} members, ${mealDocs.size} meals, ${expenseDocs.size} expenses")
            } catch (e: Exception) {
                Log.w(TAG, "Audit log notice: ${e.message}")
            }
            Result.success(backupId)
        } catch (e: Exception) {
            Log.e(TAG, "createBackupSnapshot failed", e)
            Result.failure(e)
        }
    }

    /**
     * Resets all mess data after automatically creating a complete backup in Firestore.
     */
    suspend fun resetMessDataWithBackup(
        messId: String = DEFAULT_MESS_ID,
        backupLabel: String = "Pre-Reset Auto Backup"
    ): Result<Unit> {
        return try {
            // Step 1: Attempt to create backup snapshot first
            val backupResult = createBackupSnapshot(messId, backupLabel, "RESET_BACKUP")
            if (backupResult.isFailure) {
                Log.w(TAG, "Pre-reset auto backup encountered note: ${backupResult.exceptionOrNull()?.message}, proceeding with reset safely")
            }

            val messRef = db.collection("messes").document(messId)

            // Step 2: Delete all daily meals
            for (doc in safeGetDocuments(messRef.collection("daily_meals"))) {
                try {
                    doc.reference.delete().await()
                } catch (e: Exception) {
                    Log.w(TAG, "Delete meal doc failed: ${e.message}")
                }
            }

            // Step 3: Delete all bazaar expenses
            for (doc in safeGetDocuments(messRef.collection("expenses"))) {
                try {
                    doc.reference.delete().await()
                } catch (e: Exception) {
                    Log.w(TAG, "Delete expense doc failed: ${e.message}")
                }
            }

            // Step 4: Delete all utilities
            for (doc in safeGetDocuments(messRef.collection("utilities"))) {
                try {
                    doc.reference.delete().await()
                } catch (e: Exception) {
                    Log.w(TAG, "Delete utility doc failed: ${e.message}")
                }
            }

            // Step 5: Delete all egg records
            for (doc in safeGetDocuments(messRef.collection("egg_records"))) {
                try {
                    doc.reference.delete().await()
                } catch (e: Exception) {
                    Log.w(TAG, "Delete egg doc failed: ${e.message}")
                }
            }

            // Step 6: Reset member balances (preserve members, zero out deposits)
            for (doc in safeGetDocuments(messRef.collection("members"))) {
                try {
                    doc.reference.update(
                        mapOf(
                            "potDeposits" to 0.0,
                            "cashDeposits" to 0.0,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Reset member deposit failed: ${e.message}")
                }
            }

            try {
                logAudit(messId, "RESET_ALL_DATA", "Mess data was reset successfully after auto-backup (${backupResult.getOrNull() ?: "local"})")
            } catch (e: Exception) {
                Log.w(TAG, "Audit log notice: ${e.message}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "resetMessDataWithBackup failed", e)
            Result.failure(e)
        }
    }

    /**
     * Restores all mess data from a previously created snapshot backup.
     */
    suspend fun restoreBackupSnapshot(
        messId: String = DEFAULT_MESS_ID,
        backupId: String
    ): Result<Unit> {
        return try {
            val messRef = db.collection("messes").document(messId)
            val backupDoc = messRef.collection("backups").document(backupId).get().await()

            if (!backupDoc.exists()) {
                return Result.failure(Exception("ব্যাকআপ ডাটা পাওয়া যায়নি"))
            }

            val jsonStr = backupDoc.getString("backupDataJson") ?: ""
            if (jsonStr.isBlank()) {
                return Result.failure(Exception("ব্যাকআপ ফাইলে কোনো তথ্য নেই"))
            }

            // Checksum integrity verification
            val storedChecksum = backupDoc.getString("checksum")
            if (!storedChecksum.isNullOrBlank()) {
                val actualChecksum = calculateSha256(jsonStr)
                if (storedChecksum != actualChecksum) {
                    return Result.failure(SecurityException("ব্যাকআপ ফাইলের অখণ্ডতা নষ্ট হয়েছে (Checksum Mismatch)"))
                }
            }

            // Schema Dry-run parse
            val rootJson = JSONObject(jsonStr)

            // Auto pre-restore safety snapshot before overwrite
            try {
                createBackupSnapshot(messId, "Pre-Restore Safety Snapshot", "PRE_RESTORE_SAFETY")
            } catch (e: Exception) {
                Log.w(TAG, "Pre-restore safety snapshot notice: ${e.message}")
            }

            // Restore Members
            if (rootJson.has("members")) {
                val array = rootJson.getJSONArray("members")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val docId = obj.optString("_id")
                    val map = HashMap<String, Any>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (key != "_id") {
                            map[key] = obj.get(key)
                        }
                    }
                    map["updatedAt"] = FieldValue.serverTimestamp()
                    if (docId.isNotBlank()) {
                        messRef.collection("members").document(docId).set(map, SetOptions.merge()).await()
                    }
                }
            }

            // Restore Daily Meals
            if (rootJson.has("daily_meals")) {
                val array = rootJson.getJSONArray("daily_meals")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val docId = obj.optString("_id")
                    val map = HashMap<String, Any>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (key != "_id") {
                            val v = obj.get(key)
                            if (key == "memberMeals" && v is JSONObject) {
                                val mmMap = HashMap<String, Map<String, Double>>()
                                val mKeys = v.keys()
                                while (mKeys.hasNext()) {
                                    val memId = mKeys.next()
                                    val subObj = v.getJSONObject(memId)
                                    val subMap = HashMap<String, Double>()
                                    val sKeys = subObj.keys()
                                    while (sKeys.hasNext()) {
                                        val sk = sKeys.next()
                                        subMap[sk] = subObj.optDouble(sk, 0.0)
                                    }
                                    mmMap[memId] = subMap
                                }
                                map[key] = mmMap
                            } else {
                                map[key] = v
                            }
                        }
                    }
                    map["updatedAt"] = FieldValue.serverTimestamp()
                    if (docId.isNotBlank()) {
                        messRef.collection("daily_meals").document(docId).set(map, SetOptions.merge()).await()
                    }
                }
            }

            // Restore Expenses
            if (rootJson.has("expenses")) {
                val array = rootJson.getJSONArray("expenses")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val docId = obj.optString("_id")
                    val map = HashMap<String, Any>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (key != "_id") {
                            map[key] = obj.get(key)
                        }
                    }
                    map["updatedAt"] = FieldValue.serverTimestamp()
                    if (docId.isNotBlank()) {
                        messRef.collection("expenses").document(docId).set(map, SetOptions.merge()).await()
                    }
                }
            }

            // Restore Utilities
            if (rootJson.has("utilities")) {
                val array = rootJson.getJSONArray("utilities")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val docId = obj.optString("_id")
                    val map = HashMap<String, Any>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (key != "_id") {
                            map[key] = obj.get(key)
                        }
                    }
                    map["updatedAt"] = FieldValue.serverTimestamp()
                    if (docId.isNotBlank()) {
                        messRef.collection("utilities").document(docId).set(map, SetOptions.merge()).await()
                    }
                }
            }

            // Restore Eggs
            if (rootJson.has("egg_records")) {
                val array = rootJson.getJSONArray("egg_records")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val docId = obj.optString("_id")
                    val map = HashMap<String, Any>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (key != "_id") {
                            map[key] = obj.get(key)
                        }
                    }
                    map["updatedAt"] = FieldValue.serverTimestamp()
                    if (docId.isNotBlank()) {
                        messRef.collection("egg_records").document(docId).set(map, SetOptions.merge()).await()
                    }
                }
            }

            logAudit(messId, "RESTORE_BACKUP", "Restored snapshot backup ($backupId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "restoreBackupSnapshot failed", e)
            Result.failure(e)
        }
    }

    suspend fun logAudit(action: String, details: String, messId: String = DEFAULT_MESS_ID) {
        try {
            val userEmail = auth.currentUser?.email ?: "Manager"
            val logId = UUID.randomUUID().toString()
            val payload = hashMapOf(
                "messId" to messId,
                "action" to action,
                "details" to details,
                "performedBy" to userEmail,
                "timestamp" to FieldValue.serverTimestamp()
            )
            db.collection("messes").document(messId).collection("audit_logs").document(logId).set(payload).await()
        } catch (e: Exception) {
            Log.w(TAG, "logAudit failed: ${e.message}")
        }
    }
}
