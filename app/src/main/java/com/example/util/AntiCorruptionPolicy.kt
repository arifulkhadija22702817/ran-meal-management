package com.example.util

import com.example.data.model.DailyMeal
import com.example.data.model.EggRecord
import com.example.data.model.Expense
import com.google.firebase.Timestamp

/**
 * 24-Hour Anti-Corruption Auto-Block Policy:
 *
 * Rules:
 * 1. When an admin creates or updates data (meal attendance, bazaar expense, egg record),
 *    a 24-hour active window begins from the moment of that update (updatedAt ?: createdAt).
 * 2. During those 24 hours, the admin can freely correct, adjust, or update that day's data.
 * 3. Exactly 24 hours after the last update, that day's data is AUTO-BLOCKED permanently
 *    to prevent any retroactive tampering or corruption.
 * 4. If a date has no entry yet (not yet updated or logged), it is NOT blocked, allowing
 *    the admin to enter the data.
 */
object AntiCorruptionPolicy {
    // 24 Hours in milliseconds
    const val LOCK_DURATION_MS: Long = 24 * 60 * 60 * 1000L

    /**
     * Timestamp check: Checks if 24 hours have passed since the given timestamp.
     */
    fun isTimestampLocked(timestamp: Timestamp?, fallbackDateStr: String? = null): Boolean {
        if (timestamp == null) return false
        val now = System.currentTimeMillis()
        val entryTime = timestamp.toDate().time
        return (now - entryTime) >= LOCK_DURATION_MS
    }

    /**
     * DailyMeal Auto-Block Check:
     * - If no meal record has been created or updated yet: NOT locked (admin can enter data).
     * - Auto-blocks 24 hours after the last update timestamp (updatedAt ?: createdAt).
     */
    fun isMealLocked(dailyMeal: DailyMeal?, selectedDate: String): Boolean {
        if (dailyMeal == null) return false
        val lastUpdateTs = dailyMeal.updatedAt ?: dailyMeal.createdAt ?: return false
        val now = System.currentTimeMillis()
        val elapsed = now - lastUpdateTs.toDate().time
        return elapsed >= LOCK_DURATION_MS
    }

    /**
     * Bazaar Expense Auto-Block Check:
     * - Auto-blocks 24 hours after the expense was created or last updated.
     */
    fun isExpenseLocked(expense: Expense): Boolean {
        val lastUpdateTs = expense.updatedAt ?: expense.createdAt ?: return false
        val now = System.currentTimeMillis()
        val elapsed = now - lastUpdateTs.toDate().time
        return elapsed >= LOCK_DURATION_MS
    }

    /**
     * Egg Record Auto-Block Check:
     * - Auto-blocks 24 hours after the egg record was created or last updated.
     */
    fun isEggRecordLocked(record: EggRecord): Boolean {
        val lastUpdateTs = record.updatedAt ?: record.createdAt ?: return false
        val now = System.currentTimeMillis()
        val elapsed = now - lastUpdateTs.toDate().time
        return elapsed >= LOCK_DURATION_MS
    }

    /**
     * Calculates remaining time in milliseconds before the 24h auto-lock expires.
     * Returns 0 if already locked.
     */
    fun getRemainingTimeMs(timestamp: Timestamp?): Long {
        if (timestamp == null) return LOCK_DURATION_MS
        val now = System.currentTimeMillis()
        val elapsed = now - timestamp.toDate().time
        return (LOCK_DURATION_MS - elapsed).coerceAtLeast(0L)
    }

    /**
     * Returns a human-friendly status badge showing exact remaining time or auto-blocked status.
     */
    fun getLockStatusMessage(timestamp: Timestamp?, fallbackDateStr: String?, isBengali: Boolean): String {
        if (timestamp == null) {
            return if (isBengali)
                "🟢 সক্রিয় (ডাটা আপডেট করার পর থেকে ২৪ ঘণ্টার অটো-ব্লক কাউন্টডাউন শুরু হবে)"
            else
                "🟢 Active (24h auto-block countdown starts after update)"
        }

        val now = System.currentTimeMillis()
        val elapsed = now - timestamp.toDate().time

        if (elapsed >= LOCK_DURATION_MS) {
            return if (isBengali)
                "🔒 ২৪ ঘণ্টা অতিক্রান্ত (দুর্নীতি রোধে এই দিনের ডাটা স্থায়ীভাবে অটো-ব্লক)"
            else
                "🔒 24h Passed (Auto-blocked permanently against corruption)"
        }

        val remainingMs = LOCK_DURATION_MS - elapsed
        val remainingHours = remainingMs / (1000 * 60 * 60)
        val remainingMins = (remainingMs / (1000 * 60)) % 60

        return if (isBengali)
            "⏳ আপডেট পর ২৪ ঘণ্টার অটো-ব্লক হতে বাকি: ${remainingHours} ঘণ্টা ${remainingMins} মিনিট"
        else
            "⏳ Auto-blocks in ${remainingHours}h ${remainingMins}m (Anti-Corruption)"
    }
}
