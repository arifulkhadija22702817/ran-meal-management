package com.example

import com.example.data.model.DailyMeal
import com.example.data.model.Expense
import com.example.data.model.Member
import com.example.data.model.Utility
import com.example.util.MessCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class MessCalculatorTest {

    @Test
    fun calculateSummary_correctlyComputesMealRateAndPotBalances() {
        val members = listOf(
            Member(id = "m1", name = "Arif", phone = "+8801700", potDeposits = 20.0, cashDeposits = 2000.0, active = true),
            Member(id = "m2", name = "Khadija", phone = "+8801800", potDeposits = 25.0, cashDeposits = 2000.0, active = true)
        )

        // Day 1: Arif eats 0.5 breakfast, 1.0 lunch, 1.0 dinner (2.5 meals)
        // Khadija eats 1.0 breakfast, 1.0 lunch, 1.0 dinner (3.0 meals)
        // Total meals = 5.5
        val meals = listOf(
            DailyMeal(
                id = "2026-10-01",
                messId = "main",
                date = "2026-10-01",
                memberMeals = mapOf(
                    "m1" to mapOf("b" to 0.5, "l" to 1.0, "d" to 1.0),
                    "m2" to mapOf("b" to 1.0, "l" to 1.0, "d" to 1.0)
                )
            )
        )

        // Total bazaar = 550 BDT
        val expenses = listOf(
            Expense(id = "e1", title = "Chicken & Veg", amount = 550.0, date = "2026-10-01")
        )

        // Shared Utility = 200 BDT (100 each for 2 members)
        val utilities = listOf(
            Utility(id = "u1", title = "Wi-Fi", amount = 200.0, month = "2026-10")
        )

        val summary = MessCalculator.calculateSummary(members, meals, expenses, utilities, "2026-10")

        // Assertions
        assertEquals(5.5, summary.totalMeals, 0.01)
        assertEquals(550.0, summary.totalBazaarCost, 0.01)
        assertEquals(100.0, summary.mealRate, 0.01) // 550 / 5.5 = 100 BDT/meal
        assertEquals(200.0, summary.totalUtilityCost, 0.01)
        assertEquals(100.0, summary.perMemberUtility, 0.01)

        val arif = summary.memberSummaries.first { it.member.id == "m1" }
        assertEquals(2.5, arif.totalMeals, 0.01)
        assertEquals(2.5, arif.totalPotsConsumed, 0.01)
        assertEquals(17.5, arif.remainingPots, 0.01) // 20 - 2.5 = 17.5
        assertEquals(250.0, arif.mealCost, 0.01) // 2.5 * 100 = 250
        assertEquals(100.0, arif.utilityShare, 0.01)
        assertEquals(350.0, arif.totalCost, 0.01) // 250 + 100 = 350
        assertEquals(1650.0, arif.netBalance, 0.01) // 2000 - 350 = 1650 refund
    }

    @Test
    fun calculateDailyMemberCost_matchesUserExactRules() {
        // 1. Full meal (সকাল + দুপুর + রাত) -> 40 Tk
        assertEquals(40.0, MessCalculator.calculateDailyMemberCost(1.0, 1.0, 1.0), 0.001)

        // 2. দুপুর + রাত -> 40 Tk
        assertEquals(40.0, MessCalculator.calculateDailyMemberCost(0.0, 1.0, 1.0), 0.001)

        // 3. সকাল + দুপুর -> 25 Tk
        assertEquals(25.0, MessCalculator.calculateDailyMemberCost(1.0, 1.0, 0.0), 0.001)

        // 4. রাত + সকাল -> 25 Tk
        assertEquals(25.0, MessCalculator.calculateDailyMemberCost(1.0, 0.0, 1.0), 0.001)

        // 5. শুধু দুপুর -> 25 Tk
        assertEquals(25.0, MessCalculator.calculateDailyMemberCost(0.0, 1.0, 0.0), 0.001)

        // 6. শুধু রাত -> 25 Tk
        assertEquals(25.0, MessCalculator.calculateDailyMemberCost(0.0, 0.0, 1.0), 0.001)

        // 7. শুধু সকাল -> 0 Tk (সকালের মিল অন করাতে টাকা বিয়োগ হবে না)
        assertEquals(0.0, MessCalculator.calculateDailyMemberCost(1.0, 0.0, 0.0), 0.001)

        // 8. সব বন্ধ (Absent) -> 0 Tk
        assertEquals(0.0, MessCalculator.calculateDailyMemberCost(0.0, 0.0, 0.0), 0.001)
    }

    @Test
    fun calculateSummary_fixedDailyModeCalculatesAccurately() {
        val members = listOf(
            Member(id = "m1", name = "Arif", potDeposits = 20.0, cashDeposits = 1000.0, active = true),
            Member(id = "m2", name = "Sabbir", potDeposits = 20.0, cashDeposits = 1000.0, active = true)
        )

        // Day 1: Arif takes full meal (B+L+D = 40 Tk), Sabbir takes Lunch only (L = 25 Tk)
        // Day 2: Arif takes Lunch+Dinner (L+D = 40 Tk), Sabbir takes Breakfast+Lunch (B+L = 25 Tk)
        val meals = listOf(
            DailyMeal(
                id = "2026-10-01",
                date = "2026-10-01",
                memberMeals = mapOf(
                    "m1" to mapOf("b" to 1.0, "l" to 1.0, "d" to 1.0),
                    "m2" to mapOf("b" to 0.0, "l" to 1.0, "d" to 0.0)
                )
            ),
            DailyMeal(
                id = "2026-10-02",
                date = "2026-10-02",
                memberMeals = mapOf(
                    "m1" to mapOf("b" to 0.0, "l" to 1.0, "d" to 1.0),
                    "m2" to mapOf("b" to 1.0, "l" to 1.0, "d" to 0.0)
                )
            )
        )

        val summary = MessCalculator.calculateSummary(
            members = members,
            dailyMeals = meals,
            expenses = emptyList(),
            utilities = emptyList(),
            monthPrefix = "2026-10",
            calculationMode = com.example.data.model.CALCULATION_MODE_FIXED_DAILY,
            selectedDate = "2026-10-01"
        )

        val arif = summary.memberSummaries.first { it.member.id == "m1" }
        val sabbir = summary.memberSummaries.first { it.member.id == "m2" }

        // Arif: Day 1 (40) + Day 2 (40) = 80 Tk
        assertEquals(80.0, arif.mealCost, 0.01)
        assertEquals(920.0, arif.netBalance, 0.01) // 1000 - 80 = 920 refund
        assertEquals(40.0, arif.todayCost, 0.01) // Day 1 cost

        // Sabbir: Day 1 (25) + Day 2 (25) = 50 Tk
        assertEquals(50.0, sabbir.mealCost, 0.01)
        assertEquals(950.0, sabbir.netBalance, 0.01) // 1000 - 50 = 950 refund
        assertEquals(25.0, sabbir.todayCost, 0.01) // Day 1 cost

        // Total mess today meal cost on Day 1: 40 + 25 = 65 Tk
        assertEquals(65.0, summary.todayMealCost, 0.01)
        // Total monthly billed meal cost: 80 + 50 = 130 Tk
        assertEquals(130.0, summary.totalMealCost, 0.01)
    }
}
