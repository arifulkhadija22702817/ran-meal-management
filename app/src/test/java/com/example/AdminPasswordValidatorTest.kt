package com.example

import com.example.util.AdminPasswordValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminPasswordValidatorTest {

    @Test
    fun testValidPasswords() {
        val validList = listOf(
            "Admin@123",    // 9 chars, letter+digit+special
            "Pass#2026",    // 9 chars
            "A1!bcdef",     // 8 chars (min length)
            "Abcd1234#\$%^" // 12 chars (max length)
        )
        for (p in validList) {
            val (valid, err) = AdminPasswordValidator.validate(p)
            assertTrue("Expected '$p' to be valid, but got error: $err", valid)
        }
    }

    @Test
    fun testTooShortPassword() {
        val (valid, err) = AdminPasswordValidator.validate("A1!bcde") // 7 chars
        assertFalse(valid)
        assertTrue(err?.contains("৮") == true)
    }

    @Test
    fun testTooLongPassword() {
        val (valid, err) = AdminPasswordValidator.validate("A1!bcdef12345") // 13 chars
        assertFalse(valid)
        assertTrue(err?.contains("১২") == true)
    }

    @Test
    fun testMissingLetter() {
        val (valid, err) = AdminPasswordValidator.validate("12345678#@")
        assertFalse(valid)
        assertTrue(err?.contains("বর্ণ") == true || err?.contains("Alphabet") == true)
    }

    @Test
    fun testMissingDigit() {
        val (valid, err) = AdminPasswordValidator.validate("Abcdefgh@#")
        assertFalse(valid)
        assertTrue(err?.contains("সংখ্যা") == true || err?.contains("Number") == true)
    }

    @Test
    fun testMissingSpecialChar() {
        val (valid, err) = AdminPasswordValidator.validate("Abcdefgh12")
        assertFalse(valid)
        assertTrue(err?.contains("স্পেশাল") == true || err?.contains("Special") == true)
    }
}
