package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val CALCULATION_MODE_FIXED_DAILY = "FIXED_DAILY"
const val CALCULATION_MODE_BAZAAR_RATE = "BAZAAR_RATE"

@IgnoreExtraProperties
data class Mess(
    @DocumentId val id: String = "",
    val name: String = "",
    val currency: String = "BDT",
    val managerUid: String = "",
    val managerEmail: String = "",
    val currentMonth: String = "",
    val adminPassword: String = "",
    val calculationMode: String = CALCULATION_MODE_FIXED_DAILY, // "FIXED_DAILY" or "BAZAAR_RATE"
    val fullMealRate: Double = 40.0,    // Full (B+L+D) or (L+D)
    val partialMealRate: Double = 25.0, // (B+L) or (D+B) or Only Lunch or Only Dinner
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @ServerTimestamp val managerAssignedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Member(
    @DocumentId val id: String = "",
    val messId: String = "",
    val name: String = "",
    val phone: String = "",
    val roomNumber: String = "",
    val potDeposits: Double = 0.0,
    val cashDeposits: Double = 0.0,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailyMeal(
    @DocumentId val id: String = "", // Typically YYYY-MM-DD
    val messId: String = "",
    val date: String = "",
    val breakfastCount: Double = 0.0,
    val lunchCount: Double = 0.0,
    val dinnerCount: Double = 0.0,
    // Map of memberId -> Map of ("b" -> Double, "l" -> Double, "d" -> Double)
    val memberMeals: Map<String, Map<String, Double>> = emptyMap(),
    val notes: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val messId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val payerMemberId: String = "",
    val category: String = "BAZAAR", // BAZAAR, SPICES, EXTRA
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Utility(
    @DocumentId val id: String = "",
    val messId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val month: String = "", // YYYY-MM
    val category: String = "OTHER", // RENT, ELECTRICITY, GAS, WIFI, MAID, OTHER
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val messId: String = "",
    val action: String = "",
    val details: String = "",
    val performedBy: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)

data class MemberSummary(
    val member: Member,
    val totalBreakfast: Double,
    val totalLunch: Double,
    val totalDinner: Double,
    val totalMeals: Double,
    val totalPotsConsumed: Double,
    val remainingPots: Double,
    val mealCost: Double,
    val utilityShare: Double,
    val totalCost: Double,
    val cashDeposited: Double,
    val netBalance: Double, // Positive = Refund/Credit, Negative = Due/Payable
    val todayCost: Double = 0.0 // Instant today's cost incurred by this member
)

@IgnoreExtraProperties
data class EggRecord(
    @DocumentId val id: String = "",
    val messId: String = "",
    val date: String = "",
    val purchasedCount: Int = 0, // কয়টি ডিম কেনা হলো
    val consumedCount: Int = 0,  // কয়টি ডিম খাওয়া হলো
    val damagedCount: Int = 0,   // কয়টি ডিম নষ্ট বা ভাঙা হলো
    val cost: Double = 0.0,      // মোট কত টাকা খরচ হলো
    val notes: String = "",      // নোট / মন্তব্য
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class EggSummary(
    val totalPurchased: Int = 0,
    val totalConsumed: Int = 0,
    val totalDamaged: Int = 0,
    val currentStock: Int = 0, // totalPurchased - totalConsumed - totalDamaged
    val totalCost: Double = 0.0
)

data class MessSummary(
    val totalMeals: Double,
    val totalBazaarCost: Double,
    val mealRate: Double,
    val totalUtilityCost: Double,
    val perMemberUtility: Double,
    val totalPotsDeposited: Double,
    val totalPotsConsumed: Double,
    val remainingMessPots: Double,
    val memberSummaries: List<MemberSummary>,
    val calculationMode: String = CALCULATION_MODE_FIXED_DAILY,
    val fullMealRate: Double = 40.0,
    val partialMealRate: Double = 25.0,
    val totalMealCost: Double = 0.0,
    val todayMealCost: Double = 0.0
)

@IgnoreExtraProperties
data class MessBackup(
    @DocumentId val id: String = "",
    val messId: String = "",
    val label: String = "",
    val reason: String = "PRE_RESET_BACKUP",
    val dateString: String = "",
    val membersCount: Int = 0,
    val mealsCount: Int = 0,
    val expensesCount: Int = 0,
    val utilitiesCount: Int = 0,
    val eggsCount: Int = 0,
    val backupDataJson: String = "",
    val checksum: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminHandoverRecord(
    @DocumentId val id: String = "",
    val messId: String = "",
    val previousAdminEmail: String = "",
    val previousAdminUid: String = "",
    val newAdminEmail: String = "",
    val newAdminUid: String = "",
    @ServerTimestamp val transferredAt: Timestamp? = null,
    val handoverMonth: String = "",
    val note: String = ""
)

@IgnoreExtraProperties
data class AuthorizedMember(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "MEMBER",
    @ServerTimestamp val updatedAt: Timestamp? = null
)


