package com.jericx.trainr.presentation.workout.components

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UnitSystem
import org.junit.Test

class ExerciseSetTableTest {

    private fun set(reps: Int? = null, weightKg: Float? = null, seconds: Int? = null) =
        ExerciseSet(
            setNumber = 1,
            actualReps = reps,
            actualWeightKg = weightKg,
            actualSeconds = seconds
        )

    @Test
    fun aLoadedSetReadsAsWeightTimesReps() {
        assertThat(previousCellText(ExerciseMeasure.WEIGHT_AND_REPS, set(reps = 12, weightKg = 20f)))
            .isEqualTo("20kg × 12")
        assertThat(previousCellText(ExerciseMeasure.WEIGHT_AND_REPS, set(reps = 10, weightKg = 22.5f)))
            .isEqualTo("22.5kg × 10")
    }

    @Test
    fun aBodyweightSetReadsAsThePlainCount() {
        assertThat(previousCellText(ExerciseMeasure.REPS, set(reps = 15))).isEqualTo("15")
        assertThat(previousCellText(ExerciseMeasure.WEIGHT_AND_REPS, set(reps = 12)))
            .isEqualTo("12")
    }

    @Test
    fun aTimedSetReadsInTheTimersClockLanguage() {
        assertThat(previousCellText(ExerciseMeasure.DURATION, set(seconds = 45))).isEqualTo("0:45")
        assertThat(previousCellText(ExerciseMeasure.DURATION, set(seconds = 300))).isEqualTo("5:00")
    }

    @Test
    fun secondsFormatAsMinutesAndSeconds() {
        assertThat(formatSeconds(45)).isEqualTo("0:45")
        assertThat(formatSeconds(300)).isEqualTo("5:00")
        assertThat(formatSeconds(605)).isEqualTo("10:05")
    }

    // Digits fill in from the seconds end, the way a microwave timer is typed.
    @Test
    fun typedDigitsReadAsMinutesThenSeconds() {
        assertThat(secondsFromDigits("5")).isEqualTo(5)
        assertThat(secondsFromDigits("45")).isEqualTo(45)
        assertThat(secondsFromDigits("500")).isEqualTo(300)
        assertThat(secondsFromDigits("1230")).isEqualTo(750)
        assertThat(secondsFromDigits("")).isNull()
    }

    @Test
    fun storedSecondsRoundTripThroughTheirDigits() {
        for (seconds in listOf(5, 45, 90, 300, 750)) {
            assertThat(secondsFromDigits(durationDigits(seconds))).isEqualTo(seconds)
        }
    }

    // A slipped second dot used to clear the number that was already typed.
    @Test
    fun aSecondDecimalPointIsRefusedRatherThanWipingTheNumber() {
        assertThat(weightEntry("22.5", UnitSystem.METRIC)).isEqualTo(NumberEntry.ACCEPTED)
        assertThat(weightEntry("22.5.", UnitSystem.METRIC)).isEqualTo(NumberEntry.MALFORMED)
        assertThat(weightEntry("22.5.7", UnitSystem.METRIC)).isEqualTo(NumberEntry.MALFORMED)
        assertThat(weightEntry(".", UnitSystem.METRIC)).isEqualTo(NumberEntry.MALFORMED)
    }

    @Test
    fun aWeightIsDigitsWithAtMostOneDotAndTwoPlacesAfterIt() {
        assertThat(weightEntry("22", UnitSystem.METRIC)).isEqualTo(NumberEntry.ACCEPTED)
        assertThat(weightEntry("22.", UnitSystem.METRIC)).isEqualTo(NumberEntry.ACCEPTED)
        assertThat(weightEntry(".5", UnitSystem.METRIC)).isEqualTo(NumberEntry.ACCEPTED)
        assertThat(weightEntry("22.25", UnitSystem.METRIC)).isEqualTo(NumberEntry.ACCEPTED)
        assertThat(weightEntry("22.255", UnitSystem.METRIC)).isEqualTo(NumberEntry.MALFORMED)
    }

    // A hardware keyboard offers letters whatever the on-screen pad shows.
    @Test
    fun aCharacterThatCannotBelongToTheNumberIsRefused() {
        listOf("abc", "2a", "2e3", "-5", "+5", "2f", "NaN", "Infinity", "2 ", "1,5").forEach {
            assertThat(weightEntry(it, UnitSystem.METRIC)).isEqualTo(NumberEntry.MALFORMED)
            assertThat(repsEntry(it)).isEqualTo(NumberEntry.MALFORMED)
            assertThat(durationEntry(it)).isEqualTo(NumberEntry.MALFORMED)
        }
    }

    @Test
    fun repsAreWholeNumbersUpToNineHundredAndNinetyNine() {
        assertThat(repsEntry("12")).isEqualTo(NumberEntry.ACCEPTED)
        assertThat(repsEntry("0")).isEqualTo(NumberEntry.ACCEPTED)
        assertThat(repsEntry("999")).isEqualTo(NumberEntry.ACCEPTED)
        assertThat(repsEntry("1000")).isEqualTo(NumberEntry.OUT_OF_RANGE)
        assertThat(repsEntry("999999")).isEqualTo(NumberEntry.OUT_OF_RANGE)
        assertThat(repsEntry("12.5")).isEqualTo(NumberEntry.MALFORMED)
    }

    // The maximum is the one the person reads, so it follows the unit they type in.
    @Test
    fun aWeightStopsAtAThousandKilosOrTheSameInPounds() {
        assertThat(weightEntry("1000", UnitSystem.METRIC)).isEqualTo(NumberEntry.ACCEPTED)
        assertThat(weightEntry("1000.5", UnitSystem.METRIC)).isEqualTo(NumberEntry.OUT_OF_RANGE)
        assertThat(weightEntry("999999", UnitSystem.METRIC)).isEqualTo(NumberEntry.OUT_OF_RANGE)
        assertThat(weightEntry("2205", UnitSystem.IMPERIAL)).isEqualTo(NumberEntry.ACCEPTED)
        assertThat(weightEntry("2206", UnitSystem.IMPERIAL)).isEqualTo(NumberEntry.OUT_OF_RANGE)
        assertThat(weightEntry("1500", UnitSystem.METRIC)).isEqualTo(NumberEntry.OUT_OF_RANGE)
    }

    @Test
    fun aTimeStopsAtNinetyNineMinutesAndFiftyNineSeconds() {
        assertThat(durationEntry("9900")).isEqualTo(NumberEntry.ACCEPTED)
        assertThat(durationEntry("9959")).isEqualTo(NumberEntry.ACCEPTED)
        assertThat(durationEntry("10000")).isEqualTo(NumberEntry.OUT_OF_RANGE)
        assertThat(durationEntry("99999")).isEqualTo(NumberEntry.OUT_OF_RANGE)
    }

    // A finished face is written the way it is stored.
    @Test
    fun aTimeIsShownTheWayItIsStored() {
        listOf("5", "45", "100", "130", "1045", "9959").forEach {
            assertThat(durationEntry(it)).isEqualTo(NumberEntry.ACCEPTED)
            assertThat(durationDigits(secondsFromDigits(it)!!)).isEqualTo(it)
        }
    }

    @Test
    fun aSecondsFigureOfSixtyOrMoreIsRefusedOnceTheFaceIsFull() {
        assertThat(durationEntry("1060")).isEqualTo(NumberEntry.MALFORMED)
        assertThat(durationEntry("1094")).isEqualTo(NumberEntry.MALFORMED)
        assertThat(durationEntry("9960")).isEqualTo(NumberEntry.MALFORMED)
        assertThat(durationEntry("9999")).isEqualTo(NumberEntry.MALFORMED)
    }

    // 6-3-0 used to stall at 0:06 and 1-6-4-0 at 0:16: a buffer still short of
    // a full face was judged as though it were one. Backspacing walks the same
    // buffers back down, so this pins the delete key on every time too.
    @Test
    fun everyStorableTimeCanBeTypedAndBackspacedDigitByDigit() {
        val refused = (1..MAX_SECONDS)
            .flatMap { total -> durationDigits(total).let { d -> d.indices.map { d.take(it + 1) } } }
            .filter { durationEntry(it) != NumberEntry.ACCEPTED }

        assertThat(refused).isEmpty()
        assertThat(secondsFromDigits("63")).isEqualTo(63)
        assertThat(secondsFromDigits("630")).isEqualTo(390)
        assertThat(secondsFromDigits("1640")).isEqualTo(1000)
    }

    @Test
    fun aTimeThatStartsWithAZeroIsRefused() {
        assertThat(durationEntry("0")).isEqualTo(NumberEntry.MALFORMED)
        assertThat(durationEntry("0500")).isEqualTo(NumberEntry.MALFORMED)
    }

    // A number logged over the maximum has to be editable, and every step down
    // from it is over the maximum too.
    @Test
    fun aStoredNumberOverTheMaximumCanBeTypedBackIntoRange() {
        assertThat(movesTowardRange("999999", "99999", ::repsEntry)).isTrue()
        assertThat(movesTowardRange("1000", "100", ::repsEntry)).isTrue()
        assertThat(movesTowardRange("999999", "9999999", ::repsEntry)).isFalse()
        assertThat(movesTowardRange("12", "123", ::repsEntry)).isFalse()
        assertThat(movesTowardRange("1000.17", "1000.1") { weightEntry(it, UnitSystem.METRIC) })
            .isTrue()
    }

    // The fourth digit is dropped and the cell keeps 999: without the message
    // the keystroke is swallowed with nothing to show it ever happened.
    @Test
    fun aDigitRefusedForBeingTooBigSaysWhatTheRangeIs() {
        val held = holding("999", "9999", ::repsEntry)

        assertThat(held.text).isEqualTo("999")
        assertThat(held.saysRange).isTrue()
    }

    @Test
    fun theNextAcceptedDigitClearsTheMessage() {
        val refused = holding("99", "9999", ::repsEntry)
        val accepted = holding(refused.text, "999", ::repsEntry)

        assertThat(refused.saysRange).isTrue()
        assertThat(accepted.text).isEqualTo("999")
        assertThat(accepted.saysRange).isFalse()
    }

    @Test
    fun aMalformedKeystrokeLeavesNoMessageEither() {
        val held = holding("22.5", "22.5.") { weightEntry(it, UnitSystem.METRIC) }

        assertThat(held.text).isEqualTo("22.5")
        assertThat(held.saysRange).isFalse()
    }

    @Test
    fun aNumberStillOverTheMaximumKeepsTheMessage() {
        val held = holding("1500", "15000") { weightEntry(it, UnitSystem.METRIC) }

        assertThat(held.text).isEqualTo("1500")
        assertThat(held.saysRange).isTrue()
    }

    @Test
    fun typingAStoredNumberBackIntoRangeClearsTheMessage() {
        val held = holding("1500", "150") { weightEntry(it, UnitSystem.METRIC) }

        assertThat(held.text).isEqualTo("150")
        assertThat(held.saysRange).isFalse()
    }

    // Clearing the field is the person clearing it, and is always allowed.
    @Test
    fun anEmptyFieldIsAccepted() {
        assertThat(weightEntry("", UnitSystem.METRIC)).isEqualTo(NumberEntry.ACCEPTED)
        assertThat(repsEntry("")).isEqualTo(NumberEntry.ACCEPTED)
        assertThat(durationEntry("")).isEqualTo(NumberEntry.ACCEPTED)
    }

    // The column reports what happened, not what was asked, so an unlogged set shows a dash
    @Test
    fun anUnloggedSetShowsADash() {
        assertThat(previousCellText(ExerciseMeasure.WEIGHT_AND_REPS, null)).isEqualTo("—")
        assertThat(
            previousCellText(
                ExerciseMeasure.WEIGHT_AND_REPS,
                set().copy(targetReps = 12, targetWeightKg = 20f)
            )
        ).isEqualTo("—")
        assertThat(previousCellText(ExerciseMeasure.REPS, set())).isEqualTo("—")
        assertThat(previousCellText(ExerciseMeasure.DURATION, set())).isEqualTo("—")
    }
}
