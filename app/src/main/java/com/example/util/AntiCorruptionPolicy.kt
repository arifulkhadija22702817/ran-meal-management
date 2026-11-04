package com.example.util

import com.example.data.model.DailyMeal
import com.example.data.model.EggRecord
import com.example.data.model.Expense
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 24-Hour Anti-Corruption Lock Policy
 *
 * Ensures that 24 hours after any meal on/off, bazaar expense, egg record,
 * or cash/rice transaction is logged or updated, it becomes permanently locked.
 * Even the manager cannot edit or delete it, preventing any retroactive tampering
 * or corruption.
 */
object AntiCorruptionPolicy {
    // 24 Hours in milliseconds
    const val LOCK_DURATION_MS: Long = 24 * 60 * 60 * 1000L

    /**
     * Checks if 24 hours have elapsed since a given timestamp or date.
     */
    fun isTimestampLocked(timestamp: Timestamp?, fallbackDateStr: String? = null): Boolean {
        val now = System.currentTimeMillis()
        if (timestamp != null) {
            val entryTime = timestamp.toDate().time
            return (now - entryTime) >= LOCK_DURATION_MS
        }
        if (!fallbackDateStr.isNullOrBlank()) {
            return isDatePast24Hours(fallbackDateStr)
        }
        return false
    }

    /**
     * Checks if a DailyMeal attendance sheet is locked.
     * Rule:
     * - Today's date or future dates are ACTIVE and NEVER locked.
     * - Past dates (older than today) are locked if 24 hours have elapsed since the date or last modification.
     */
    fun isMealLocked(dailyMeal: DailyMeal?, selectedDate: String): Boolean {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        // Today or future dates can ALWAYS be edited by the manager
        if (selectedDate >= todayStr) {
            return false
        }

        // For past dates:
        val ts = dailyMeal?.updatedAt ?: dailyMeal?.createdAt
        return if (ts != null) {
            (System.currentTimeMillis() - ts.toDate().time) >= LOCK_DURATION_MS
        } else {
            isDatePast24Hours(dailyMeal?.date?.ifBlank { selectedDate } ?: selectedDate)
        }
    }

    /**
     * Checks if a Bazaar expense is locked.
     */
    fun isExpenseLocked(expense: Expense): Boolean {
        val ts = expense.updatedAt ?: expense.createdAt
        return if (ts != null) {
            (System.currentTimeMillis() - ts.toDate().time) >= LOCK_DURATION_MS
        } else {
            isDatePast24Hours(expense.date)
        }
    }

    /**
     * Checks if an Egg record is locked.
     */
    fun isEggRecordLocked(record: EggRecord): Boolean {
        val ts = record.updatedAt ?: record.createdAt
        return if (ts != null) {
            (System.currentTimeMillis() - ts.toDate().time) >= LOCK_DURATION_MS
        } else {
            isDatePast24Hours(record.date)
        }
    }

    /**
     * For calendar dates: checks if more than 24 hours have passed since the end of that date.
     */
    fun isDatePast24Hours(dateStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateStr) ?: return false
            val now = System.currentTimeMillis()
            // 24 hours after the end of that date (start of day + 48 hours)
            (now - (date.time + (2 * LOCK_DURATION_MS))) >= 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns a human-readable lock countdown or status badge.
     */
    fun getLockStatusMessage(timestamp: Timestamp?, fallbackDateStr: String?, isBengali: Boolean): String {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (!fallbackDateStr.isNullOrBlank() && fallbackDateStr >= todayStr) {
            return if (isBengali) "🟢 আজকের সক্রিয় মিল শিট (এডিট ও পরিবর্তনযোগ্য)" else "🟢 Active Meal Sheet for Today (Editable)"
        }
        val now = System.currentTimeMillis()
        val tsMillis = timestamp?.toDate()?.time
        if (tsMillis != null) {
            val elapsed = now - tsMillis
            if (elapsed >= LOCK_DURATION_MS) {
                return if (isBengali) "🔒 ২৪ ঘণ্টা অতিক্রান্ত (স্থায়ীভাবে লক)" else "🔒 24 Hours Passed (Permanently Locked)"
            }
            val remainingHours = ((LOCK_DURATION_MS - elapsed) / (1000 * 60 * 60)).coerceAtLeast(0)
            val remainingMins = (((LOCK_DURATION_MS - elapsed) / (1000 * 60)) % 60).coerceAtLeast(0)
            return if (isBengali) "⏳ লক হতে বাকি: ${remainingHours}ঘণ্টা ${remainingMins}মি" else "⏳ Lock in: ${remainingHours}h ${remainingMins}m"
        }
        if (isDatePast24Hours(fallbackDateStr ?: "")) {
            return if (isBengali) "🔒 ২৪ ঘণ্টা অতিক্রান্ত (স্থায়ীভাবে লক)" else "🔒 24 Hours Passed (Permanently Locked)"
        }
        return if (isBengali) "⏳ ২৪ ঘণ্টা পর স্বয়ংক্রিয় লক হবে" else "⏳ Auto-locks 24h after entry"
    }
}
