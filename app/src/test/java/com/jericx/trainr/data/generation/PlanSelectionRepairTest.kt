package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.generation.PlanSkeleton
import com.jericx.trainr.domain.generation.SessionFocus
import com.jericx.trainr.domain.generation.SkeletonDay
import com.jericx.trainr.domain.generation.SkeletonSlot
import com.jericx.trainr.domain.generation.SlotTier
import com.jericx.trainr.domain.model.UnitSystem
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Test

class PlanSelectionRepairTest {

    private val repair = PlanSelectionRepair()

    private fun slot(id: String, label: String, vararg candidates: String) = SkeletonSlot(
        id = id, label = label, tier = SlotTier.ACCESSORY, patterns = emptyList(), muscles = emptySet(),
        candidates = candidates.toList(), sets = 3, restSeconds = 60
    )

    private fun skeleton(vararg days: SkeletonDay) = PlanSkeleton(
        title = "Test Week", days = days.toList(), units = UnitSystem.METRIC, maxSetsPerSession = 20,
        sessionCeilingMinutes = 90, weeklySetsByRegion = emptyMap(), uncoveredPatterns = emptySet()
    )

    // Five open slots: the warm-up is settled and is not the model's to choose.
    private val week = skeleton(
        SkeletonDay(
            1, SessionFocus.FULL_BODY,
            listOf(
                slot("warm_up", "the warm-up", "arm_circles"),
                slot("primary", "the main lift", "goblet_squat", "dumbbell_squat", "split_squat"),
                slot("secondary", "the second lift", "push_up", "dumbbell_bench_press"),
                slot("isolation", "the isolation", "biceps_curl", "hammer_curl")
            )
        ),
        SkeletonDay(
            3, SessionFocus.UPPER,
            listOf(
                slot("primary", "the main lift", "dumbbell_row", "pull_up"),
                slot("accessory", "the accessory lift", "lateral_raise", "front_raise")
            )
        )
    )

    private val goodDay1 = mapOf(
        "primary" to "dumbbell_squat", "secondary" to "push_up", "isolation" to "hammer_curl", "title" to "Legs and Push"
    )
    private val goodDay3 = mapOf("primary" to "pull_up", "accessory" to "lateral_raise", "title" to "Upper Body Pull")

    private fun answer(day1: Map<String, String>? = goodDay1, day3: Map<String, String>? = goodDay3) =
        buildJsonObject {
            day1?.let { fields -> putJsonObject("day1") { fields.forEach { (name, value) -> put(name, value) } } }
            day3?.let { fields -> putJsonObject("day3") { fields.forEach { (name, value) -> put(name, value) } } }
        }.toString()

    private fun accepted(json: String) = repair.repair(json, week) as SelectionRepairResult.Accepted

    private fun problems(json: String) = (repair.repair(json, week) as SelectionRepairResult.Rejected).problems

    @Test
    fun aCleanAnswerIsTakenAsItIs() {
        val result = accepted(answer())

        assertThat(result.repairs).isEqualTo(0)
        assertThat(result.selection.days.getValue("day1")).isEqualTo(
            DaySelection(
                mapOf("primary" to "dumbbell_squat", "secondary" to "push_up", "isolation" to "hammer_curl"),
                "Legs and Push"
            )
        )
        assertThat(result.selection.days.getValue("day3").title).isEqualTo("Upper Body Pull")
    }

    // V0. The parser's own words are not passed on: they can quote the answer.
    @Test
    fun anAnswerThatIsNotAnObjectGoesBackWithOneFixedMessage() {
        listOf("not json at all", "[1, 2]", "\"day1\"", "").forEach {
            assertThat(problems(it)).containsExactly(PlanSelectionRepair.NOT_AN_OBJECT)
        }
    }

    // V1: a whole session missing is every one of its slots repaired.
    @Test
    fun aMissingSessionIsFilledFromTheTopOfEachList() {
        val result = accepted(answer(day3 = null))

        assertThat(result.repairs).isEqualTo(2)
        assertThat(result.selection.days.getValue("day3"))
            .isEqualTo(DaySelection(mapOf("primary" to "dumbbell_row", "accessory" to "lateral_raise"), "Upper Body"))
    }

    // V2
    @Test
    fun aMissingSlotTakesTheTopOfItsList() {
        val result = accepted(answer(day1 = goodDay1 - "isolation"))

        assertThat(result.repairs).isEqualTo(1)
        assertThat(result.selection.days.getValue("day1").slots["isolation"]).isEqualTo("biceps_curl")
    }

    // V3
    @Test
    fun aMovementTheSlotNeverOfferedIsReplacedByItsBest() {
        val result = accepted(answer(day1 = goodDay1 + ("isolation" to "barbell_curl")))

        assertThat(result.repairs).isEqualTo(1)
        assertThat(result.selection.days.getValue("day1").slots["isolation"]).isEqualTo("biceps_curl")
    }

    // V4
    @Test
    fun aMovementAlreadyUsedThatDayIsNotUsedTwice() {
        val result = accepted(answer(day1 = goodDay1 + ("secondary" to "dumbbell_squat")))

        assertThat(result.repairs).isEqualTo(1)
        assertThat(result.selection.days.getValue("day1").slots.values).containsNoDuplicates()
        assertThat(result.selection.days.getValue("day1").slots["secondary"]).isEqualTo("push_up")
    }

    // V5 to V8 are the app's to fix: a bad title costs no request.
    @Test
    fun aTitleThatNamesNothingFallsBackToTheSessionAndCostsNoRetry() {
        listOf(
            "Legs", "", "Five Words Is Too Many", "A".repeat(20) + " " + "B".repeat(21),
            "Full Body A", "Day 2 Legs", "Week 3 Push", "Good Form Training", "Upper Training Session"
        ).forEach { title ->
            val result = accepted(answer(day1 = goodDay1 + ("title" to title)))

            assertThat(result.repairs).isEqualTo(0)
            assertThat(result.selection.days.getValue("day1").title).isEqualTo("Full Body")
        }
    }

    @Test
    fun twoSessionsWithOneNameKeepItForTheFirstOnly() {
        val result = accepted(
            answer(day1 = goodDay1 + ("title" to "Upper Body Strength"), day3 = goodDay3 + ("title" to "upper body strength"))
        )

        assertThat(result.selection.days.getValue("day1").title).isEqualTo("Upper Body Strength")
        assertThat(result.selection.days.getValue("day3").title).isEqualTo("Upper Body")
    }

    // V9: mostly repaired is mostly the app's week, so the model is asked
    // again, told every problem at once.
    @Test
    fun anAnswerMostlyRepairedGoesBackWithEveryProblemNamed() {
        val sent = problems(
            answer(
                day1 = mapOf("secondary" to "goblet_squat", "isolation" to "barbell_curl", "title" to "Full Body A"),
                day3 = goodDay3 + ("title" to "Good Form Training")
            )
        )

        assertThat(sent).containsExactly(
            "In day1 (Full Body) you left out the main lift. Fill every slot with one movement from that slot's own list.",
            "In day1 (Full Body), 'goblet_squat' is already used earlier in that session. Each slot needs a different movement.",
            "In day1 (Full Body), the isolation was answered with 'barbell_curl'. That is not on that slot's list. " +
                "Choose only from the keys listed for the slot you are filling.",
            "The title for day1 (Full Body) is an index label. The app already shows which day and which week it is; " +
                "name what the session trains.",
            "The title for day3 (Upper Body) uses \"good form\", which says nothing about this session. " +
                "Name the region and the focus instead."
        )
    }

    @Test
    fun aSessionLeftOutAndATitleTooShortAreBothNamed() {
        val sent = problems(answer(day1 = mapOf("title" to "Legs"), day3 = null))

        assertThat(sent).contains(
            "You left out day3 (Upper Body). Answer every session in the schema, each with its title and each of its slots."
        )
        assertThat(sent).contains(
            "The title for day1 (Full Body) must be two to four words naming the body region and the focus, " +
                "like \"Upper Body Strength\". \"Legs\" is not."
        )
    }

    @Test
    fun anAnswerThatAnswersNoSessionGoesBack() {
        assertThat(problems("{}")).hasSize(2)
        assertThat(problems("""{"day2": {"primary": "push_up"}}""")).hasSize(2)
    }

    @Test
    fun aWeekWithNothingLeftToChooseNeedsNoAnswer() {
        val settled = skeleton(SkeletonDay(1, SessionFocus.FULL_BODY, listOf(slot("primary", "the main lift", "push_up"))))

        assertThat(repair.repair("not json at all", settled))
            .isEqualTo(SelectionRepairResult.Accepted(PlanSelection(), 0))
    }
}
