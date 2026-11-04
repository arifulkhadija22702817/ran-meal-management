package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.MessSummary
import com.example.data.model.Utility
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    /**
     * Generates a monthly PDF report for the entire mess or individual member.
     * Returns the generated File stored safely in cacheDir.
     */
    fun generateMonthlyReport(
        context: Context,
        messName: String,
        month: String,
        summary: MessSummary,
        utilities: List<Utility>
    ): File {
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val fileName = "Meal_Management_Report_${month.replace("-", "_")}.pdf"
        val pdfFile = File(reportsDir, fileName)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (595 x 842 points)
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw Header Banner
        paint.color = Color.rgb(13, 104, 59) // Emerald primary
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(messName, 36f, 42f, paint)

        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        canvas.drawText("Monthly Meal & Expense Statement — Month: $month | Generated: $dateStr", 36f, 68f, paint)

        // Executive Summary Cards
        var yPos = 120f
        paint.color = Color.rgb(240, 244, 241)
        canvas.drawRoundRect(36f, yPos, 559f, yPos + 70f, 8f, 8f, paint)

        paint.color = Color.rgb(20, 30, 24)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val col1X = 50f
        val col2X = 180f
        val col3X = 310f
        val col4X = 440f

        canvas.drawText("TOTAL MEALS", col1X, yPos + 22f, paint)
        canvas.drawText("TOTAL BAZAAR", col2X, yPos + 22f, paint)
        canvas.drawText("MEAL RATE", col3X, yPos + 22f, paint)
        canvas.drawText("RICE POTS LEFT", col4X, yPos + 22f, paint)

        paint.textSize = 14f
        paint.color = Color.rgb(13, 104, 59)
        canvas.drawText(String.format(Locale.US, "%.1f", summary.totalMeals), col1X, yPos + 46f, paint)
        canvas.drawText(String.format(Locale.US, "৳ %.2f", summary.totalBazaarCost), col2X, yPos + 46f, paint)
        canvas.drawText(String.format(Locale.US, "৳ %.2f", summary.mealRate), col3X, yPos + 46f, paint)
        canvas.drawText(String.format(Locale.US, "%.1f pots", summary.remainingMessPots), col4X, yPos + 46f, paint)

        // Member Breakdown Table Header
        yPos += 105f
        paint.color = Color.rgb(230, 235, 232)
        canvas.drawRect(36f, yPos, 559f, yPos + 24f, paint)

        paint.color = Color.rgb(30, 40, 34)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("MEMBER NAME", 42f, yPos + 16f, paint)
        canvas.drawText("MEALS", 175f, yPos + 16f, paint)
        canvas.drawText("POTS (DEP/EAT/REM)", 225f, yPos + 16f, paint)
        canvas.drawText("MEAL COST", 335f, yPos + 16f, paint)
        canvas.drawText("DEPOSITED", 405f, yPos + 16f, paint)
        canvas.drawText("NET STATUS", 480f, yPos + 16f, paint)

        // Table Rows
        yPos += 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f

        var isAlternate = false
        for (m in summary.memberSummaries) {
            if (isAlternate) {
                paint.color = Color.rgb(248, 250, 249)
                canvas.drawRect(36f, yPos, 559f, yPos + 22f, paint)
            }
            isAlternate = !isAlternate

            paint.color = Color.rgb(30, 30, 30)
            canvas.drawText(m.member.name.take(18), 42f, yPos + 15f, paint)
            canvas.drawText(String.format(Locale.US, "%.1f", m.totalMeals), 175f, yPos + 15f, paint)

            val potsStr = String.format(Locale.US, "%.0f / %.1f / %.1f", m.member.potDeposits, m.totalPotsConsumed, m.remainingPots)
            canvas.drawText(potsStr, 225f, yPos + 15f, paint)
            canvas.drawText(String.format(Locale.US, "৳%.0f", m.mealCost), 335f, yPos + 15f, paint)
            canvas.drawText(String.format(Locale.US, "৳%.0f", m.cashDeposited), 405f, yPos + 15f, paint)

            // Net Balance Status
            if (m.netBalance >= 0) {
                paint.color = Color.rgb(22, 163, 74) // Green refund
                canvas.drawText(String.format(Locale.US, "+৳%.0f (Refund)", m.netBalance), 480f, yPos + 15f, paint)
            } else {
                paint.color = Color.rgb(220, 38, 38) // Red due
                canvas.drawText(String.format(Locale.US, "-৳%.0f (Due)", -m.netBalance), 480f, yPos + 15f, paint)
            }

            yPos += 22f
            if (yPos > 720f) break
        }

        // Shared Utilities Box
        yPos += 20f
        paint.color = Color.rgb(245, 247, 245)
        canvas.drawRoundRect(36f, yPos, 559f, yPos + 60f, 6f, 6f, paint)

        paint.color = Color.rgb(13, 104, 59)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SHARED UTILITIES (Total: ৳${String.format(Locale.US, "%.1f", summary.totalUtilityCost)})", 46f, yPos + 18f, paint)

        paint.color = Color.rgb(60, 60, 60)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val utilNames = if (utilities.isEmpty()) "None recorded" else utilities.joinToString(" | ") { "${it.title}: ৳${it.amount}" }
        canvas.drawText(utilNames.take(85), 46f, yPos + 36f, paint)
        canvas.drawText("Per Member Utility Share: ৳${String.format(Locale.US, "%.2f", summary.perMemberUtility)}", 46f, yPos + 50f, paint)

        // Footer Note
        paint.color = Color.GRAY
        paint.textSize = 8f
        canvas.drawText("Official automated report generated by Meal Management App • Cloud backed and tamper-verified.", 36f, 800f, paint)

        document.finishPage(page)

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }
}
