package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.DailyMeal
import com.example.data.model.EggRecord
import com.example.data.model.EggSummary
import com.example.data.model.Expense
import com.example.data.model.Member
import com.example.data.model.MemberSummary
import com.example.data.model.MessSummary
import com.example.data.model.Utility
import java.io.File
import java.net.URLEncoder
import java.util.Locale

object WhatsAppShareHelper {

    /**
     * Normalizes phone number into international WhatsApp format (e.g., 01712345678 -> 8801712345678).
     */
    fun normalizePhoneNumber(rawPhone: String): String {
        val digitsOnly = rawPhone.replace("+", "").replace("-", "").replace(" ", "").trim()
        return when {
            digitsOnly.startsWith("01") && digitsOnly.length == 11 -> "88$digitsOnly"
            digitsOnly.startsWith("8801") -> digitsOnly
            else -> digitsOnly
        }
    }

    /**
     * Sends an itemized PERSONAL meal & expense statement DIRECTLY to member's own WhatsApp number.
     * Starts with rice pot accounting ("chaul dewa theke suru kore sob hisab") as specifically requested.
     */
    fun shareMemberStatementToWhatsApp(
        context: Context,
        summary: MemberSummary,
        month: String,
        mealRate: Double,
        messName: String = "Meal Management",
        calculationMode: String = com.example.data.model.CALCULATION_MODE_FIXED_DAILY
    ) {
        val member = summary.member
        val normalizedPhone = normalizePhoneNumber(member.phone)
        val isFixedDaily = calculationMode == com.example.data.model.CALCULATION_MODE_FIXED_DAILY

        val balanceLine = if (summary.netBalance > 0) {
            "✅ *হিসাবের চূড়ান্ত ফলাফল (ফেরত পাবেন / Back Pabe):* +৳${String.format(Locale.US, "%.2f", summary.netBalance)}\n👉 আপনি মেস ম্যানেজার থেকে এই টাকা ফেরত পাবেন (Refund)।"
        } else if (summary.netBalance < 0) {
            "⚠️ *হিসাবের চূড়ান্ত ফলাফল (ম্যানেজারকে দিবেন / Manager k Dibe):* -৳${String.format(Locale.US, "%.2f", -summary.netBalance)}\n👉 এই টাকা আপনার বকেয়া, ম্যানেজারকে জমা দিতে হবে (Due)।"
        } else {
            "🤝 *হিসাবের চূড়ান্ত ফলাফল:* ৳০.০০ (আপনার খাওয়া খরচ ও জমা টাকা সম্পূর্ণ সমান, কোনো বকেয়া নেই)।"
        }

        val mealCalculationLine = if (isFixedDaily) {
            "• সক্রিয় পদ্ধতি: ফিক্সড দৈনিক প্যাকেজ (ফুল/দুপুর+রাত ৳৪০, আংশিক ৳২৫)\n• মিল বাবদ মোট খরচ (দৈনিক স্বয়ংক্রিয় কর্তন): ৳${String.format(Locale.US, "%.2f", summary.mealCost)}"
        } else {
            "• সক্রিয় পদ্ধতি: বাজারের হিসাব অনুযায়ী মিল রেট\n• মিল বাবদ খরচ (${String.format(Locale.US, "%.1f", summary.totalMeals)} মিল × ৳${String.format(Locale.US, "%.2f", mealRate)}): ৳${String.format(Locale.US, "%.2f", summary.mealCost)}"
        }

        val textMessage = """
            👤 *ব্যক্তিগত মেস হিসাব বিবরণী (Personal Statement)*
            🏠 *মেস:* $messName
            👤 *সদস্যের নাম:* ${member.name} ${if (member.roomNumber.isNotBlank()) "(রুম: ${member.roomNumber})" else ""}
            📱 *মোবাইল:* ${if (member.phone.isNotBlank()) member.phone else "সংরক্ষিত নেই"}
            📅 *বিলিং মাস:* $month
            ━━━━━━━━━━━━━━━━━━━━━
            🍚 *১. চাউল জমা ও ব্যবহারের হিসাব (Rice Pot Accounting)*:
            • জমা দিয়েছেন (Pots Deposited): ${String.format(Locale.US, "%.1f", member.potDeposits)} পট
            • খাওয়া হয়েছে (Pots Consumed): ${String.format(Locale.US, "%.1f", summary.totalPotsConsumed)} পট
            • চাউলের স্থিতি: ${
                if (summary.remainingPots >= 0) "+${String.format(Locale.US, "%.1f", summary.remainingPots)} পট (অবশিষ্ট)"
                else "-${String.format(Locale.US, "%.1f", -summary.remainingPots)} পট (ঘাটতি)"
            }

            🍲 *২. মিল গ্রহণের বিস্তারিত (Daily Meals)*:
            • 🌅 সকালের নাস্তা (Breakfast): ${String.format(Locale.US, "%.1f", summary.totalBreakfast)} টি
            • ☀️ দুপুরের খাবার (Lunch): ${String.format(Locale.US, "%.1f", summary.totalLunch)} টি
            • 🌙 রাতের খাবার (Dinner): ${String.format(Locale.US, "%.1f", summary.totalDinner)} টি
            • 🍲 সর্বমোট খাওয়া মিল: ${String.format(Locale.US, "%.1f", summary.totalMeals)} টি

            💵 *৩. জমা টাকা ও খরচের হিসাব (Joma Tk & Total Costs)*:
            • আপনার মোট জমা টাকা (Joma Tk): ৳${String.format(Locale.US, "%.2f", summary.cashDeposited)}
            • আপনি মোট খেয়েছেন/খরচ (Khaiche Tk): ৳${String.format(Locale.US, "%.2f", summary.totalCost)}
              $mealCalculationLine
              - মেসের শেয়ার্ড ইউটিলিটি বিলের অংশ: ৳${String.format(Locale.US, "%.2f", summary.utilityShare)}
            ━━━━━━━━━━━━━━━━━━━━━
            $balanceLine
            ━━━━━━━━━━━━━━━━━━━━━
            _Meal Management Cloud App থেকে সম্পূর্ণ নির্ভুল স্বয়ংক্রিয় হিসাব_
        """.trimIndent()

        openWhatsAppDirectOrChooser(context, normalizedPhone, textMessage, "সদস্যের ব্যক্তিগত হিসাব")
    }

    /**
     * Sends the DAILY mess & meal status directly to the Mess WhatsApp Group.
     */
    fun shareMessDailyStatusToGroup(
        context: Context,
        date: String,
        messName: String,
        dailyMeal: DailyMeal?,
        members: List<Member>,
        todayExpenses: List<Expense> = emptyList(),
        eggRecord: EggRecord? = null,
        eggSummary: EggSummary? = null,
        calculationMode: String = com.example.data.model.CALCULATION_MODE_FIXED_DAILY,
        fullMealRate: Double = 40.0,
        partialMealRate: Double = 25.0
    ) {
        val totalB = dailyMeal?.breakfastCount ?: 0.0
        val totalL = dailyMeal?.lunchCount ?: 0.0
        val totalD = dailyMeal?.dinnerCount ?: 0.0
        val totalDayMeals = totalB + totalL + totalD

        val isFixedDaily = calculationMode == com.example.data.model.CALCULATION_MODE_FIXED_DAILY
        val todayTotalMealExpense = members.sumOf { member ->
            val mMeals = dailyMeal?.memberMeals?.get(member.id) ?: emptyMap()
            val b = mMeals["b"] ?: 0.0
            val l = mMeals["l"] ?: 0.0
            val d = mMeals["d"] ?: 0.0
            MessCalculator.calculateDailyMemberCost(b, l, d, fullMealRate, partialMealRate)
        }

        val totalBazaarToday = todayExpenses.sumOf { it.amount }

        val bazaarText = if (todayExpenses.isEmpty()) {
            "• আজ কোনো বাজার এন্ট্রি করা হয়নি।"
        } else {
            todayExpenses.joinToString("\n") { "• ${it.title}: ৳${String.format(Locale.US, "%.0f", it.amount)}" }
        }

        val calculationModeText = if (isFixedDaily) {
            "• হিসাব পদ্ধতি: ফিক্সড দৈনিক প্যাকেজ (ফুল/দুপুর+রাত ৪০৳, সকাল+দুপুর/রাত+সকাল/শুধু দুপুর/রাত ২৫৳)\n• ⚡ আজকের মোট মেস মিল খরচ: ৳${todayTotalMealExpense.toInt()}"
        } else {
            "• হিসাব পদ্ধতি: বাজারের হিসাব অনুযায়ী মিল রেট"
        }

        val textMessage = """
            📢 *[$messName] আজকের দৈনিক মেস ও মিল রিপোর্ট*
            📅 *তারিখ:* $date
            ━━━━━━━━━━━━━━━━━━━━━
            🍽️ *আজকের মিল হিসাব*:
            • 🌅 সকাল (Breakfast): ${String.format(Locale.US, "%.1f", totalB)}
            • ☀️ দুপুর (Lunch): ${String.format(Locale.US, "%.1f", totalL)}
            • 🌙 রাত (Dinner): ${String.format(Locale.US, "%.1f", totalD)}
            • 🍲 আজকের মোট মিল: ${String.format(Locale.US, "%.1f", totalDayMeals)}
            • 👥 সক্রিয় সদস্য সংখ্যা: ${members.count { it.active }} জন
            $calculationModeText

            🛒 *আজকের বাজার খরচ*:
            $bazaarText
            • সর্বমোট দৈনিক বাজার: ৳${String.format(Locale.US, "%.0f", totalBazaarToday)}

            🥚 *ডিমের হিসাব ও স্টক*:
            • আজকের খাওয়া ডিম: ${eggRecord?.consumedCount ?: 0} টি
            • আজকের কেনা ডিম: ${eggRecord?.purchasedCount ?: 0} টি
            • বর্তমান অবশিষ্ট মজুদ ডিম: ${eggSummary?.currentStock ?: 0} টি
            ━━━━━━━━━━━━━━━━━━━━━
            _Meal Management App থেকে স্বয়ংক্রিয়ভাবে মেস গ্রুপে প্রেরিত_
        """.trimIndent()

        openWhatsAppDirectOrChooser(context, "", textMessage, "মেস গ্রুপে দৈনিক রিপোর্ট")
    }

    /**
     * Sends the MONTHLY overall mess report directly to the Mess WhatsApp Group.
     */
    fun shareMessMonthlyStatusToGroup(
        context: Context,
        month: String,
        messName: String,
        summary: MessSummary,
        utilities: List<Utility>,
        eggSummary: EggSummary? = null
    ) {
        val monthUtilities = utilities.filter { it.month == month }
        val totalUtilityCost = monthUtilities.sumOf { it.amount }

        val textMessage = """
            📢 *[$messName] সার্বিক মাসিক মেস রিপোর্ট ($month)*
            📅 *বিলিং মাস:* $month
            ━━━━━━━━━━━━━━━━━━━━━
            👥 *মেস সারসংক্ষেপ*:
            • মোট সক্রিয় সদস্য: ${summary.memberSummaries.size} জন
            • মোট পরিবেশিত মিল: ${String.format(Locale.US, "%.1f", summary.totalMeals)} টি
            • মোট বাজার খরচ: ৳${String.format(Locale.US, "%.2f", summary.totalBazaarCost)}
            • মোট শেয়ার্ড ইউটিলিটি বিল: ৳${String.format(Locale.US, "%.2f", totalUtilityCost)}
            • বর্তমান মিল রেট: ৳${String.format(Locale.US, "%.2f", summary.mealRate)} / মিল

            🍚 *মেসের চাউলের হিসাব*:
            • সদস্যদের মোট জমা চাউল: ${String.format(Locale.US, "%.1f", summary.totalPotsDeposited)} পট
            • সর্বমোট খাওয়া চাউল: ${String.format(Locale.US, "%.1f", summary.totalPotsConsumed)} পট
            • মেসের অবশিষ্ট চাউল: ${String.format(Locale.US, "%.1f", summary.remainingMessPots)} পট

            🥚 *ডিমের হিসাব*:
            • চলতি মাসের মোট কেনা: ${eggSummary?.totalPurchased ?: 0} টি
            • চলতি মাসের মোট খাওয়া: ${eggSummary?.totalConsumed ?: 0} টি
            • নষ্ট / ভাঙা ডিম: ${eggSummary?.totalDamaged ?: 0} টি
            • বর্তমান অবশিষ্ট মজুদ: ${eggSummary?.currentStock ?: 0} টি
            ━━━━━━━━━━━━━━━━━━━━━
            _Meal Management App থেকে স্বয়ংক্রিয়ভাবে মেস গ্রুপে প্রেরিত_
        """.trimIndent()

        openWhatsAppDirectOrChooser(context, "", textMessage, "মেস গ্রুপে মাসিক রিপোর্ট")
    }

    /**
     * Launches WhatsApp directly with target phone if available, or presents app chooser.
     */
    private fun openWhatsAppDirectOrChooser(
        context: Context,
        phone: String,
        textMessage: String,
        chooserTitle: String
    ) {
        try {
            val encodedMessage = URLEncoder.encode(textMessage, "UTF-8")
            if (phone.length >= 8) {
                val url = "https://api.whatsapp.com/send?phone=$phone&text=$encodedMessage"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                // If no direct phone (or for sending to a WhatsApp Group), open WhatsApp directly or standard chooser
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, textMessage)
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to chooser if specific package fails
                    val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, textMessage)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(fallbackIntent, chooserTitle))
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp পাঠানো যায়নি: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares a PDF file using Android's FileProvider.
     */
    fun sharePdfFile(context: Context, pdfFile: File, subject: String = "Meal Management Monthly Report") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "Here is the monthly Meal Management statement PDF.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Monthly PDF Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
