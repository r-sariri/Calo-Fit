package com.example

import com.example.calofit.domain.calendar.JalaliCalendarHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class JalaliCalendarTest {

    @Test
    fun testGregorianToJalaliConversion() {
        // 2026-03-21 is Farvardin 1, 1405 (Nowruz)
        val jalali = JalaliCalendarHelper.gregorianToJalali(2026, 3, 21)
        assertEquals(1405, jalali.year)
        assertEquals(1, jalali.month)
        assertEquals(1, jalali.day)
        assertEquals("فروردین", jalali.monthNameFa)
    }

    @Test
    fun testPersianDigitsConversion() {
        val converted = JalaliCalendarHelper.toPersianDigits("1234567890 Calories")
        assertEquals("۱۲۳۴۵۶۷۸۹۰ Calories", converted)
    }
}
