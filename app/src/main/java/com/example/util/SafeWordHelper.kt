package com.example.util

object SafeWordHelper {

    /**
     * Replaces instances of "cu" with "cool" when safe word mode is active.
     * Preserves sentence casing and handles special combinations.
     */
    fun formatSafeWord(text: String, isSafeWordMode: Boolean = true): String {
        if (!isSafeWordMode || text.isEmpty()) return text

        var result = text
            // Full phrase replacements
            .replace("Cara de Cu", "Cara de Cool", ignoreCase = false)
            .replace("cara de cu", "cara de cool", ignoreCase = false)
            .replace("CARA DE CU", "CARA DE COOL", ignoreCase = false)
            .replace("Modo Cara de Cu", "Modo Cara de Cool", ignoreCase = false)
            .replace("modo cara de cu", "modo cara de cool", ignoreCase = false)
            .replace("MODO CARA DE CU", "MODO CARA DE COOL", ignoreCase = false)
            .replace("Cara de Kool", "Cara de Cool", ignoreCase = false)
            .replace("cara de kool", "cara de cool", ignoreCase = false)
            .replace("CARA DE KOOL", "CARA DE COOL", ignoreCase = false)

        // Word boundary replacements for isolated "cu" or "de cu"
        val cuUpper = Regex("\\bCU\\b")
        val cuTitle = Regex("\\bCu\\b")
        val cuLower = Regex("\\bcu\\b")

        result = cuUpper.replace(result, "COOL")
        result = cuTitle.replace(result, "Cool")
        result = cuLower.replace(result, "cool")

        return result
    }

    fun getAppDisplayName(isCuMode: Boolean, isSafeWordMode: Boolean = true): String {
        return if (isCuMode) {
            if (isSafeWordMode) "Cara de Cool" else "Cara de Cu"
        } else {
            "Cara de Paçoca"
        }
    }

    fun getDefaultUserName(isCuMode: Boolean, isSafeWordMode: Boolean = true): String {
        return if (isCuMode) {
            if (isSafeWordMode) "Você (Cara de Cool)" else "Você (Cara de Cu)"
        } else {
            "Você (Cara de Paçoca)"
        }
    }
}
