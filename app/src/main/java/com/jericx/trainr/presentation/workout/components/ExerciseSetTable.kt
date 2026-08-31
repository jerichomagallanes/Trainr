package com.jericx.trainr.presentation.workout.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.WeightUnit
import androidx.compose.ui.graphics.Color
import com.jericx.trainr.presentation.common.components.core.TrainrSwipeToDelete
import com.jericx.trainr.presentation.common.theme.OutlineGray
import com.jericx.trainr.presentation.common.theme.Slate800
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TextMuted
import com.jericx.trainr.presentation.common.theme.TrainrTheme

private val RowHeight = 34.dp
private val CheckSize = 24.dp
private val SetColumnWidth = 34.dp

@Composable
fun ExerciseSetTable(
    measure: ExerciseMeasure,
    sets: List<ExerciseSet>,
    onSetChanged: (ExerciseSet) -> Unit,
    onAddSet: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteSet: (ExerciseSet) -> Unit = {},
    previousSets: List<ExerciseSet> = emptyList(),
    units: UnitSystem = UnitSystem.Default
) {
    // No column at all without history: a week-one card looks exactly like the
    // design, which has no PREVIOUS.
    val showPrevious = previousSets.isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        // Column headings over nothing are noise, so an emptied table is just
        // its Add set button. The button itself is never conditional: deleting
        // the last set has to leave a way back.
        if (sets.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColumnLabel(stringResource(R.string.set_column), Modifier.width(SetColumnWidth))

                if (showPrevious) {
                    ColumnLabel(stringResource(R.string.previous_column), Modifier.weight(1f))
                }
                if (measure == ExerciseMeasure.WEIGHT_AND_REPS) {
                    ColumnLabel(
                        stringResource(
                            if (units == UnitSystem.IMPERIAL) R.string.weight_column_lbs
                            else R.string.weight_column
                        ),
                        Modifier.weight(1f)
                    )
                }
                ColumnLabel(
                    text = when (measure) {
                        ExerciseMeasure.DURATION -> stringResource(R.string.time_column)
                        else -> stringResource(R.string.reps_column)
                    },
                    modifier = Modifier.weight(1f)
                )

                Box(modifier = Modifier.size(CheckSize))
            }
        }

        sets.forEach { set ->
            key(set.id, set.setNumber) {
                val row = @Composable {
                    SetRow(
                        measure = measure,
                        set = set,
                        previousText = if (showPrevious) {
                            previousCellText(
                                measure,
                                previousSets.firstOrNull { it.setNumber == set.setNumber },
                                units
                            )
                        } else {
                            null
                        },
                        onSetChanged = onSetChanged,
                        units = units
                    )
                }

                DeletableRow(onDelete = { onDeleteSet(set) }) { row() }
            }
        }

        Text(
            text = stringResource(R.string.add_set),
            style = MaterialTheme.typography.labelLarge,
            color = Slate800,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .border(1.dp, OutlineGray, MaterialTheme.shapes.medium)
                .clickable(role = Role.Button, onClick = onAddSet)
                .padding(vertical = Spacing.small)
        )
    }
}

@Composable
private fun SetRow(
    measure: ExerciseMeasure,
    set: ExerciseSet,
    previousText: String?,
    onSetChanged: (ExerciseSet) -> Unit,
    units: UnitSystem
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = set.setNumber.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = Slate800,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(SetColumnWidth)
        )

        if (previousText != null) {
            Text(
                text = previousText,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }

        if (measure == ExerciseMeasure.WEIGHT_AND_REPS) {
            NumberCell(
                value = set.actualWeightKg?.let { formatWeight(it, units) },
                placeholder = set.targetWeightKg?.let { formatWeight(it, units) },
                decimal = true,
                onValueChange = { entered ->
                    onSetChanged(
                        set.copy(
                            actualWeightKg = entered?.toFloatOrNull()
                                ?.let { WeightUnit.toKilograms(it, units) }
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        when (measure) {
            ExerciseMeasure.DURATION -> DurationCell(
                seconds = set.actualSeconds,
                placeholderSeconds = set.targetSeconds,
                onSecondsChange = { onSetChanged(set.copy(actualSeconds = it)) },
                modifier = Modifier.weight(1f)
            )

            else -> NumberCell(
                value = set.actualReps?.toString(),
                placeholder = set.targetReps?.toString(),
                decimal = false,
                onValueChange = { onSetChanged(set.copy(actualReps = it?.toIntOrNull())) },
                modifier = Modifier.weight(1f)
            )
        }

        Image(
            painter = painterResource(
                if (set.isCompleted) R.drawable.ic_check_box else R.drawable.ic_check_box_blank
            ),
            contentDescription = stringResource(
                if (set.isCompleted) R.string.mark_set_incomplete else R.string.mark_set_complete
            ),
            modifier = Modifier
                .size(CheckSize)
                .clickable { onSetChanged(set.copy(isCompleted = !set.isCompleted)) }
        )
    }
}

@Composable
private fun ColumnLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

// The target is the placeholder rather than the value, so an untouched row shows
// what was asked for without claiming you did it.
@Composable
private fun NumberCell(
    value: String?,
    placeholder: String?,
    decimal: Boolean,
    onValueChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = Spacing.extraSmall),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value.orEmpty(),
            onValueChange = { onValueChange(it.take(6).ifBlank { null }) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = Slate800,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(Slate800),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(RowHeight)
                .clip(MaterialTheme.shapes.small)
                .border(1.dp, OutlineGray, MaterialTheme.shapes.small),
            decorationBox = { field ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (value.isNullOrEmpty()) {
                        Text(
                            text = placeholder.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                    field()
                }
            }
        )
    }
}

// Swiping a row away deletes its set, the way every logging app does it. The
// delete fires exactly once per completed dismissal — confirmValueChange can
// repeat within a drag — and rows are keyed to their set above, so a
// renumbered survivor can't inherit the dismissed state and fire again. The
// snap back only resets a row whose deletion the view model refused.
@Composable
private fun DeletableRow(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    TrainrSwipeToDelete(
        onDelete = onDelete,
        contentDescription = stringResource(R.string.delete_set)
    ) {
        content()
    }
}

// Time is typed like a microwave timer: digits fill in from the seconds end
// ("500" is 5:00), shown as m:ss to match the exercise timer, stored as seconds.
@Composable
private fun DurationCell(
    seconds: Int?,
    placeholderSeconds: Int?,
    onSecondsChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var digits by remember(seconds) {
        mutableStateOf(seconds?.let(::durationDigits) ?: "")
    }
    val shown = secondsFromDigits(digits)?.let(::formatSeconds).orEmpty()

    Box(
        modifier = modifier.padding(horizontal = Spacing.extraSmall),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = TextFieldValue(shown, selection = TextRange(shown.length)),
            onValueChange = { new ->
                digits = new.text.filter { it.isDigit() }.takeLast(4).trimStart('0')
                onSecondsChange(secondsFromDigits(digits))
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = Slate800,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(Slate800),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .height(RowHeight)
                .clip(MaterialTheme.shapes.small)
                .border(1.dp, OutlineGray, MaterialTheme.shapes.small),
            decorationBox = { field ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (shown.isEmpty()) {
                        Text(
                            text = placeholderSeconds?.let(::formatSeconds).orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                    field()
                }
            }
        )
    }
}

private fun formatWeight(kg: Float, units: UnitSystem): String {
    val shown = WeightUnit.forDisplay(kg, units)
    return if (shown % 1f == 0f) shown.toInt().toString() else shown.toString()
}

internal fun formatSeconds(totalSeconds: Int): String =
    "${totalSeconds / 60}:" + (totalSeconds % 60).toString().padStart(2, '0')

internal fun durationDigits(totalSeconds: Int): String =
    ("${totalSeconds / 60}" + (totalSeconds % 60).toString().padStart(2, '0'))
        .trimStart('0')

internal fun secondsFromDigits(digits: String): Int? {
    val cleaned = digits.filter { it.isDigit() }.takeLast(4).trimStart('0')
    if (cleaned.isEmpty()) return null

    val seconds = cleaned.takeLast(2).toInt()
    val minutes = cleaned.dropLast(2).ifEmpty { "0" }.toInt()
    return minutes * SECONDS_PER_MINUTE + seconds
}

private const val SECONDS_PER_MINUTE = 60

private const val NO_PREVIOUS = "—"

// What was actually done last time, in the shape of this row's own columns; a
// set that was prescribed but never logged shows a dash, not its target.
internal fun previousCellText(
    measure: ExerciseMeasure,
    previous: ExerciseSet?,
    units: UnitSystem = UnitSystem.Default
): String {
    if (previous == null) return NO_PREVIOUS

    return when (measure) {
        ExerciseMeasure.WEIGHT_AND_REPS -> {
            val reps = previous.actualReps ?: return NO_PREVIOUS
            val weight = previous.actualWeightKg
            if (weight == null) {
                "$reps"
            } else {
                val label = if (units == UnitSystem.IMPERIAL) "lbs" else "kg"
                "${formatWeight(weight, units)}$label × $reps"
            }
        }

        ExerciseMeasure.REPS -> previous.actualReps?.toString() ?: NO_PREVIOUS
        ExerciseMeasure.DURATION -> previous.actualSeconds?.let(::formatSeconds) ?: NO_PREVIOUS
    }
}

@Preview(showBackground = true)
@Composable
private fun ExerciseSetTablePreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.screen),
            modifier = Modifier.padding(Spacing.screen)
        ) {
            ExerciseSetTable(
                measure = ExerciseMeasure.WEIGHT_AND_REPS,
                sets = (1..3).map {
                    ExerciseSet(
                        setNumber = it,
                        targetReps = 12,
                        targetWeightKg = 20f,
                        isCompleted = it == 1
                    )
                },
                onSetChanged = {},
                onAddSet = {}
            )
            ExerciseSetTable(
                measure = ExerciseMeasure.REPS,
                sets = (1..3).map { ExerciseSet(setNumber = it, targetReps = 20) },
                onSetChanged = {},
                onAddSet = {}
            )
        }
    }
}
