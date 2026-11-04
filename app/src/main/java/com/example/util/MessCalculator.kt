package com.example.util

import com.example.data.model.CALCULATION_MODE_BAZAAR_RATE
import com.example.data.model.CALCULATION_MODE_FIXED_DAILY
import com.example.data.model.DailyMeal
import com.example.data.model.Expense
import com.example.data.model.Member
import com.example.data.model.MemberSummary
import com.example.data.model.MessSummary
import com.example.data.model.Utility
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MessCalculator {

    /**
     * Calculates the daily meal charge for an individual member based on active meals.
     * Rule:
     * - Full meal (সকাল + দুপুর + রাত) OR (দুপুর + রাত) -> ৳৪০
     * - (সকাল + দুপুর) OR (রাত + সকাল) OR (শুধু দুপুর) OR (শুধু রাত) -> ৳২৫
     * - (শুধু সকাল) -> ৳০ (টাকা বিয়োগ হবে না)
     * - Absent (সব বন্ধ) -> ৳০
     */
    fun calculateDailyMemberCost(
        breakfast: Double,
        lunch: Double,
        dinner: Double,
        fullRate: Double = 40.0,
        partialRate: Double = 25.0,
        breakfastOnlyRate: Double = 0.0
    ): Double {
        val hasB = breakfast > 0.0
        val hasL = lunch > 0.0
        val hasD = dinner > 0.0

        return when {
            // 1. Full meal (সকাল + দুপুর + রাত) or Lunch + Dinner (দুপুর + রাত) -> ৳৪০
            (hasB && hasL && hasD) || (!hasB && hasL && hasD) -> fullRate
            // 2. সকাল + দুপুর, রাত + সকাল, শুধু দুপুর, অথবা শুধু রাত -> ৳২৫
            (hasB && hasL && !hasD) || (hasB && !hasL && hasD) || (!hasB && hasL && !hasD) || (!hasB && !hasL && hasD) -> partialRate
            // 3. শুধু সকাল -> ৳০ (সকালের মিল অন করাতে টাকা বিয়োগ হবে না)
            (hasB && !hasL && !hasD) -> breakfastOnlyRate
            // 4. কোনো মিল অন নেই -> ৳০
            else -> 0.0
        }
    }

    /**
     * Calculates the entire mess balance sheet for a specified month.
     * Supports toggling between FIXED_DAILY (40 Tk / 25 Tk) and BAZAAR_RATE modes.
     */
    fun calculateSummary(
        members: List<Member>,
        dailyMeals: List<DailyMeal>,
        expenses: List<Expense>,
        utilities: List<Utility>,
        monthPrefix: String, // e.g. "2026-10"
        calculationMode: String = CALCULATION_MODE_BAZAAR_RATE, // default to maintain test compatibility
        fullMealRate: Double = 40.0,
        partialMealRate: Double = 25.0,
        selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    ): MessSummary {
        // Filter meals and expenses for the active month
        val monthMeals = dailyMeals.filter { it.date.startsWith(monthPrefix) }
        val monthExpenses = expenses.filter { it.date.startsWith(monthPrefix) }
        val monthUtilities = utilities.filter { it.month.startsWith(monthPrefix) }

        // Total shared bazaar money spent
        val totalBazaarCost = monthExpenses.sumOf { it.amount }

        // Calculate member-wise meal statistics across the month
        val memberMealMap = mutableMapOf<String, Triple<Double, Double, Double>>() // memberId -> (breakfast, lunch, dinner)
        for (m in members) {
            memberMealMap[m.id] = Triple(0.0, 0.0, 0.0)
        }

        for (meal in monthMeals) {
            meal.memberMeals.forEach { (memberId, mealCounts) ->
                val b = mealCounts["b"] ?: 0.0
                val l = mealCounts["l"] ?: 0.0
                val d = mealCounts["d"] ?: 0.0
                val current = memberMealMap[memberId] ?: Triple(0.0, 0.0, 0.0)
                memberMealMap[memberId] = Triple(current.first + b, current.second + l, current.third + d)
            }
        }

        // Total meals in the mess
        var totalMessMeals = 0.0
        members.forEach { m ->
            val triple = memberMealMap[m.id] ?: Triple(0.0, 0.0, 0.0)
            totalMessMeals += (triple.first + triple.second + triple.third)
        }

        // Meal rate (BDT per meal) under dynamic bazaar mode
        val dynamicBazaarMealRate = if (totalMessMeals > 0.0) totalBazaarCost / totalMessMeals else 0.0

        // Total utilities and per-member utility share
        val totalUtilityCost = monthUtilities.sumOf { it.amount }
        val activeMemberCount = members.count { it.active }.coerceAtLeast(1)
        val perMemberUtility = totalUtilityCost / activeMemberCount

        var totalPotsDeposited = 0.0
        var totalPotsConsumed = 0.0
        var totalMessMealCharges = 0.0
        var totalTodayMealCharges = 0.0

        // Get today's DailyMeal record for instant daily cost calculation
        val todayDailyMeal = dailyMeals.firstOrNull { it.date == selectedDate }

        val summaries = members.map { member ->
            val (b, l, d) = memberMealMap[member.id] ?: Triple(0.0, 0.0, 0.0)
            val totalMeals = b + l + d

            // Rice pot calculation
            val consumedPots = totalMeals
            val remainingPots = member.potDeposits - consumedPots

            totalPotsDeposited += member.potDeposits
            totalPotsConsumed += consumedPots

            // Calculate meal cost based on selected mode
            val mealCost: Double
            if (calculationMode == CALCULATION_MODE_FIXED_DAILY) {
                // FIXED_DAILY: Sum daily fixed charges (40 Tk / 25 Tk) across the month
                mealCost = monthMeals.sumOf { dailyMeal ->
                    val mealCounts = dailyMeal.memberMeals[member.id] ?: emptyMap()
                    val bCount = mealCounts["b"] ?: 0.0
                    val lCount = mealCounts["l"] ?: 0.0
                    val dCount = mealCounts["d"] ?: 0.0
                    calculateDailyMemberCost(bCount, lCount, dCount, fullMealRate, partialMealRate)
                }
            } else {
                // BAZAAR_RATE: Traditional dynamic rate
                mealCost = totalMeals * dynamicBazaarMealRate
            }

            // Calculate today's instant meal cost for this member
            val todayMealCounts = todayDailyMeal?.memberMeals?.get(member.id) ?: emptyMap()
            val todayB = todayMealCounts["b"] ?: 0.0
            val todayL = todayMealCounts["l"] ?: 0.0
            val todayD = todayMealCounts["d"] ?: 0.0
            val todayCost = if (calculationMode == CALCULATION_MODE_FIXED_DAILY) {
                calculateDailyMemberCost(todayB, todayL, todayD, fullMealRate, partialMealRate)
            } else {
                (todayB + todayL + todayD) * dynamicBazaarMealRate
            }

            totalMessMealCharges += mealCost
            totalTodayMealCharges += todayCost

            val utilityShare = if (member.active) perMemberUtility else 0.0
            val totalCost = mealCost + utilityShare
            val netBalance = member.cashDeposits - totalCost // positive = refund/credit, negative = due

            MemberSummary(
                member = member,
                totalBreakfast = b,
                totalLunch = l,
                totalDinner = d,
                totalMeals = totalMeals,
                totalPotsConsumed = consumedPots,
                remainingPots = remainingPots,
                mealCost = mealCost,
                utilityShare = utilityShare,
                totalCost = totalCost,
                cashDeposited = member.cashDeposits,
                netBalance = netBalance,
                todayCost = todayCost
            )
        }

        val remainingMessPots = totalPotsDeposited - totalPotsConsumed

        // Effective display meal rate
        val effectiveMealRate = if (calculationMode == CALCULATION_MODE_FIXED_DAILY) {
            if (totalMessMeals > 0.0) totalMessMealCharges / totalMessMeals else 0.0
        } else {
            dynamicBazaarMealRate
        }

        return MessSummary(
            totalMeals = totalMessMeals,
            totalBazaarCost = totalBazaarCost,
            mealRate = effectiveMealRate,
            totalUtilityCost = totalUtilityCost,
            perMemberUtility = perMemberUtility,
            totalPotsDeposited = totalPotsDeposited,
            totalPotsConsumed = totalPotsConsumed,
            remainingMessPots = remainingMessPots,
            memberSummaries = summaries,
            calculationMode = calculationMode,
            fullMealRate = fullMealRate,
            partialMealRate = partialMealRate,
            totalMealCost = totalMessMealCharges,
            todayMealCost = totalTodayMealCharges
        )
    }
}
