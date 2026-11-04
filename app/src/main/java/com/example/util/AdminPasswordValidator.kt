package com.example.util

/**
 * Validates admin passwords according to strict requirements:
 * - Minimum 8 characters, maximum 12 characters
 * - At least one alphabet character (a-zA-Z)
 * - At least one numeric digit (0-9)
 * - At least one special character (!@#$%^&*...)
 */
object AdminPasswordValidator {
    const val MIN_LENGTH = 8
    const val MAX_LENGTH = 12
    const val SPECIAL_CHARS = "!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~`"

    fun validate(password: String): Pair<Boolean, String?> {
        val trimmed = password.trim()
        if (trimmed.length < MIN_LENGTH) {
            return false to "পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে (৮ থেকে ১২ অক্ষর)"
        }
        if (trimmed.length > MAX_LENGTH) {
            return false to "পাসওয়ার্ড সর্বোচ্চ ১২ অক্ষরের হতে পারবে"
        }
        if (!trimmed.any { it.isLetter() }) {
            return false to "পাসওয়ার্ডে কমপক্ষে একটি বর্ণ (Alphabet) থাকতে হবে"
        }
        if (!trimmed.any { it.isDigit() }) {
            return false to "পাসওয়ার্ডে কমপক্ষে একটি সংখ্যা (Number) থাকতে হবে"
        }
        val specialCharList = listOf('!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+', '-', '=', '[', ']', '{', '}', ';', ':', '\'', '"', '\\', '|', ',', '.', '<', '>', '/', '?', '~', '`')
        if (!trimmed.any { it in specialCharList }) {
            return false to "পাসওয়ার্ডে কমপক্ষে একটি স্পেশাল ক্যারেক্টার (!@#$%^&*...) থাকতে হবে"
        }
        return true to null
    }

    fun hasLetter(p: String): Boolean = p.any { it.isLetter() }
    fun hasDigit(p: String): Boolean = p.any { it.isDigit() }
    fun hasSpecial(p: String): Boolean {
        val specialCharList = listOf('!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+', '-', '=', '[', ']', '{', '}', ';', ':', '\'', '"', '\\', '|', ',', '.', '<', '>', '/', '?', '~', '`')
        return p.any { it in specialCharList }
    }
    fun isLengthValid(p: String): Boolean = p.trim().length in MIN_LENGTH..MAX_LENGTH
}
