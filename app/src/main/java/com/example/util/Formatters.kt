package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Formatters {
    private val ptBr = Locale("pt", "BR")
    private val currencyFormat = NumberFormat.getCurrencyInstance(ptBr)

    fun formatCurrency(value: Double): String = currencyFormat.format(value)

    fun formatDateBr(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "—"
        val clean = isoDate.trim()
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in formats) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US)
                if (pattern.endsWith("'Z'")) parser.timeZone = TimeZone.getTimeZone("UTC")
                val date = parser.parse(clean)
                if (date != null) {
                    return SimpleDateFormat("dd/MM/yyyy", ptBr).format(date)
                }
            } catch (_: Exception) {
            }
        }
        return clean.take(10)
    }

    fun formatDateTimeBr(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "—"
        val clean = isoDate.trim()
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in formats) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US)
                if (pattern.endsWith("'Z'")) parser.timeZone = TimeZone.getTimeZone("UTC")
                val date = parser.parse(clean)
                if (date != null) {
                    return SimpleDateFormat("dd/MM/yyyy HH:mm", ptBr).format(date)
                }
            } catch (_: Exception) {
            }
        }
        return formatDateBr(clean)
    }

    fun todayIsoDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun nowIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    fun addDaysToDateBr(isoDate: String?, days: Int): String {
        val cal = java.util.Calendar.getInstance()
        if (!isoDate.isNullOrBlank()) {
            val clean = isoDate.trim()
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
            )
            for (pattern in formats) {
                try {
                    val parser = SimpleDateFormat(pattern, Locale.US)
                    if (pattern.endsWith("'Z'")) parser.timeZone = TimeZone.getTimeZone("UTC")
                    val parsed = parser.parse(clean)
                    if (parsed != null) {
                        cal.time = parsed
                        break
                    }
                } catch (_: Exception) {
                }
            }
        }
        cal.add(java.util.Calendar.DAY_OF_YEAR, days.coerceAtLeast(0))
        return SimpleDateFormat("dd/MM/yyyy", ptBr).format(cal.time)
    }

    fun parseIsoDayMillis(isoDate: String?): Long? {
        if (isoDate.isNullOrBlank()) return null
        val clean = isoDate.trim()
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in formats) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US)
                if (pattern.endsWith("'Z'")) parser.timeZone = TimeZone.getTimeZone("UTC")
                val parsed = parser.parse(clean)
                if (parsed != null) {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = parsed
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    return cal.timeInMillis
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun daysBetweenIsoDates(startIso: String?, endIso: String? = null): Long {
        val startMs = parseIsoDayMillis(startIso) ?: return 0L
        val endMs = parseIsoDayMillis(endIso ?: todayIsoDate()) ?: return 0L
        return ((endMs - startMs) / 86_400_000L)
    }

    fun parseDoubleInput(raw: String): Double {
        val normalized = raw.trim()
            .replace("R$", "")
            .replace(" ", "")
            .let { str ->
                if (str.contains(",") && str.contains(".")) {
                    str.replace(".", "").replace(",", ".")
                } else if (str.contains(",")) {
                    str.replace(",", ".")
                } else {
                    str
                }
            }
        return normalized.toDoubleOrNull() ?: 0.0
    }
}
