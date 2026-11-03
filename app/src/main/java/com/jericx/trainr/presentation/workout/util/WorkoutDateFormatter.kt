package com.jericx.trainr.presentation.workout.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// SimpleDateFormat rather than java.time: minSdk is 24 and desugaring is off.
object WorkoutDateFormatter {

    private const val RANGE_SEPARATOR = " – "
    private const val DAYS_IN_WEEK = 7

    // A week date carries no time of day and no zone, so it is read back in the
    // one it was built in rather than wherever the phone has since moved to.
    private val DATE_ZONE: TimeZone = TimeZone.getTimeZone("UTC")

    fun formatFullDate(dateMillis: Long, locale: Locale): String =
        DateFormat.getDateInstance(DateFormat.FULL, locale)
            .apply { timeZone = DATE_ZONE }
            .format(Date(dateMillis))

    fun formatWeekday(dateMillis: Long, locale: Locale): String =
        SimpleDateFormat("EEEE", locale)
            .apply { timeZone = DATE_ZONE }
            .format(Date(dateMillis))

    fun formatMediumDate(dateMillis: Long, locale: Locale): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(dateMillis))

    // Takes the stored ISO number, Monday 1 to Sunday 7, not Calendar's own.
    fun formatWeekdayName(isoWeekday: Int, locale: Locale): String =
        Calendar.getInstance().run {
            set(Calendar.DAY_OF_WEEK, isoWeekday % DAYS_IN_WEEK + 1)
            SimpleDateFormat("EEEE", locale).format(time)
        }

    fun formatWeekRange(
        startMillis: Long,
        endMillis: Long,
        locale: Locale,
        abbreviated: Boolean = false
    ): String {
        val start = calendarOf(startMillis)
        val end = calendarOf(endMillis)

        val month = if (abbreviated) "MMM" else "MMMM"
        val sameYear = start.get(Calendar.YEAR) == end.get(Calendar.YEAR)
        val sameMonth = sameYear && start.get(Calendar.MONTH) == end.get(Calendar.MONTH)

        return when {
            sameMonth -> format("$month d", start, locale) +
                RANGE_SEPARATOR + format("d, yyyy", end, locale)

            sameYear -> format("$month d", start, locale) +
                RANGE_SEPARATOR + format("$month d, yyyy", end, locale)

            else -> format("$month d, yyyy", start, locale) +
                RANGE_SEPARATOR + format("$month d, yyyy", end, locale)
        }
    }

    private fun calendarOf(millis: Long): Calendar =
        Calendar.getInstance(DATE_ZONE).apply { timeInMillis = millis }

    private fun format(pattern: String, calendar: Calendar, locale: Locale): String =
        SimpleDateFormat(pattern, locale)
            .apply { timeZone = DATE_ZONE }
            .format(calendar.time)
}
