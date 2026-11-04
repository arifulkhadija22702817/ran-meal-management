package com.example

import com.example.data.service.AdminOtpService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdminOtpServiceTest {

    @Before
    fun setup() {
        AdminOtpService.reset()
    }

    @Test
    fun testOtpGenerationAndVerification() {
        val email = "admin@messmate.app"
        val code = AdminOtpService.generateOtp(email)

        assertEquals(6, code.length)
        assertTrue(code.all { it.isDigit() })

        // Successful verification
        val (success, msg) = AdminOtpService.verifyOtp(code, email)
        assertTrue(success)
        assertTrue(AdminOtpService.isCurrentlyVerified())

        // Single-use check: same code cannot be verified again
        val (secondTry, _) = AdminOtpService.verifyOtp(code, email)
        assertFalse(secondTry)
    }

    @Test
    fun testWrongCodeRejection() {
        val email = "manager@example.com"
        val code = AdminOtpService.generateOtp(email)
        val wrongCode = if (code == "123456") "654321" else "123456"

        val (success, errorMsg) = AdminOtpService.verifyOtp(wrongCode, email)
        assertFalse(success)
        assertTrue(errorMsg.contains("ভুল কোড"))
    }

    @Test
    fun testDifferentEmailRejection() {
        val email = "correct_admin@example.com"
        val code = AdminOtpService.generateOtp(email)

        val (success, errorMsg) = AdminOtpService.verifyOtp(code, "attacker@example.com")
        assertFalse(success)
        assertTrue(errorMsg.contains("ইমেইল মেলেনি"))
    }
}
