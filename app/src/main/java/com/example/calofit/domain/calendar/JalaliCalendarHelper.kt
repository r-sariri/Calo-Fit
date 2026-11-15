package com.example.calofit.domain.calendar

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Jalali (Solar Hijri) calendar and language-aware digit localization helper.
 * Provides Gregorian to Jalali conversion, date offset navigation, and digit normalization.
 */
object JalaliCalendarHelper {

    data class JalaliDate(
        val year: Int,
        val month: Int,
        val day: Int
    ) {
        val monthNameFa: String
            get() = when (month) {
                1 -> "فروردین"
                2 -> "اردیبهشت"
                3 -> "خرداد"
                4 -> "تیر"
                5 -> "مرداد"
                6 -> "شهریور"
                7 -> "مهر"
                8 -> "آبان"
                9 -> "آذر"
                10 -> "دی"
                11 -> "بهمن"
                12 -> "اسفند"
                else -> ""
            }

        val monthNameEn: String
            get() = when (month) {
                1 -> "Farvardin"
                2 -> "Ordibehesht"
                3 -> "Khordad"
                4 -> "Tir"
                5 -> "Mordad"
                6 -> "Shahrivar"
                7 -> "Mehr"
                8 -> "Aban"
                9 -> "Azar"
                10 -> "Dey"
                11 -> "Bahman"
                12 -> "Esfand"
                else -> ""
            }

        fun formatFa(usePersianDigits: Boolean = true): String {
            val raw = "$day $monthNameFa $year"
            return if (usePersianDigits) toPersianDigits(raw) else raw
        }

        fun formatShortFa(): String {
            return toPersianDigits("$day $monthNameFa")
        }

        fun formatIso(): String {
            return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
        }
    }

    /**
     * Converts a Gregorian date (year, month 1-12, day 1-31) to Jalali date.
     */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val gy2 = gy - 1600
        val gm2 = gm - 1
        val gd2 = gd - 1

        var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400

        for (i in 0 until gm2) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm2 > 1 && ((gy2 % 4 == 0 && gy2 % 100 != 0) || (gy2 % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd2

        var jDayNo = gDayNo - 79

        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        var jd = 0
        for (i in 0..11) {
            if (jDayNo < jDaysInMonth[i]) {
                jm = i + 1
                jd = jDayNo + 1
                break
            }
            jDayNo -= jDaysInMonth[i]
        }

        return JalaliDate(jy, jm, jd)
    }

    /**
     * Formats current date for display according to language.
     */
    fun getFormattedDate(isPersian: Boolean, usePersianDigits: Boolean = isPersian): String {
        return formatIsoForDisplay(getTodayIsoDate(), isPersian, usePersianDigits)
    }

    /**
     * Formats any ISO date string (YYYY-MM-DD) for display in Persian (Jalali + Persian digits)
     * or English (Gregorian + English digits).
     */
    fun formatIsoForDisplay(
        dateIso: String,
        isPersian: Boolean,
        usePersianDigits: Boolean = isPersian
    ): String {
        val parsed = parseIsoDate(dateIso) ?: return if (isPersian) toPersianDigits(dateIso) else dateIso
        val cal = Calendar.getInstance().apply { time = parsed }
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)

        return if (isPersian) {
            val jalali = gregorianToJalali(gy, gm, gd)
            jalali.formatFa(usePersianDigits)
        } else {
            val sdfEn = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
            sdfEn.format(parsed)
        }
    }

    fun formatIsoShort(dateIso: String, isPersian: Boolean): String {
        val parsed = parseIsoDate(dateIso) ?: return dateIso
        val cal = Calendar.getInstance().apply { time = parsed }
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)

        return if (isPersian) {
            val jalali = gregorianToJalali(gy, gm, gd)
            toPersianDigits("${jalali.month}/${jalali.day}")
        } else {
            SimpleDateFormat("M/d", Locale.US).format(parsed)
        }
    }

    /**
     * Converts ASCII digits 0-9 to Persian digits ۰-۹.
     */
    fun toPersianDigits(text: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder(text.length)
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Normalizes Persian/Arabic numerals and decimal separators back to ASCII for safe parsing.
     */
    fun toAsciiDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                '٫', '،' -> sb.append('.')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatDigitsForLanguage(text: String, isPersian: Boolean): String {
        return if (isPersian) toPersianDigits(text) else toAsciiDigits(text)
    }

    fun getTodayIsoDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun addDays(dateIso: String, days: Int): String {
        val parsed = parseIsoDate(dateIso) ?: return dateIso
        val cal = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.DAY_OF_YEAR, days)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun getLastNDaysIso(count: Int, endDateIso: String = getTodayIsoDate()): List<String> {
        if (count <= 0) return emptyList()
        return (count - 1 downTo 0).map { offset ->
            addDays(endDateIso, -offset)
        }
    }

    fun parseIsoDate(dateIso: String): Date? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
            }
            sdf.parse(toAsciiDigits(dateIso.trim()))
        } catch (_: Exception) {
            null
        }
    }

    fun getGreetingKeyForCurrentHour(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "greeting_morning"
            hour < 18 -> "greeting_afternoon"
            else -> "greeting_evening"
        }
    }
}
