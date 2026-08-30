package com.jericx.trainr.presentation.workout.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.Color
import com.jericx.trainr.presentation.common.theme.OutlineGray
import com.jericx.trainr.presentation.common.theme.RedError
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
    previousSets: List<ExerciseSet> = emptyList()
) {
    // No column at all without history: a week-one card looks exactly like the
    // design, which has no PREVIOUS.
    val showPrevious = previousSets.isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColumnLabel(stringResource(R.string.set_column), Modifier.width(SetColumnWidth))

            if (showPrevious) {
                ColumnLabel(stringResource(R.string.previous_column), Modifier.weight(1f))
            }
            if (measure == ExerciseMeasure.WEIGHT_AND_REPS) {
                ColumnLabel(stringResource(R.string.weight_column), Modifier.weight(1f))
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

        sets.forEach { set ->
            val row = @Composable {
                SetRow(
                    measure = measure,
                    set = set,
                    previousText = if (showPrevious) {
                        previousCellText(
                            measure,
                            previousSets.firstOrNull { it.setNumber == set.setNumber }
                        )
                    } else {
                        null
                    },
                    onSetChanged = onSetChanged
                )
            }

            if (sets.size > 1) {
                DeletableRow(set = set, onDelete = { onDeleteSet(set) }) { row() }
            } else {
                row()
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
    onSetChanged: (ExerciseSet) -> Unit
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
                value = set.actualWeightKg?.let { formatWeight(it) },
                placeholder = set.targetWeightKg?.let { formatWeight(it) },
                decimal = true,
                onValueChange = { onSetChanged(set.copy(actualWeightKg = it?.toFloatOrNull())) },
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
// dismiss state never settles: deletion happens through recomposition, so a
// renumbered row can't inherit a dismissed state from the one it replaced.
@Composable
private fun DeletableRow(
    set: ExerciseSet,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    // One drag can confirm more than once; only the first report deletes. The
    // guard is keyed to the set INSTANCE: renumbering after a delete rebuilds
    // every remaining set, so a reused row slot cannot inherit a spent guard.
    val reported = remember(System.identityHashCode(set)) { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && !reported.value) {
                reported.value = true
                onDelete()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            // Drawn only mid-swipe: the row's own content is transparent, so a
            // permanent background would bleed red through every idle row.
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart &&
                dismissState.progress > 0f && dismissState.progress < 1f
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.extraSmall)
                        .clip(MaterialTheme.shapes.small)
                        .background(RedError),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_set),
                        tint = Color.White,
                        modifier = Modifier.padding(end = Spacing.tight)
                    )
                }
            }
        }
    ) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            content()
        }
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

private fun formatWeight(kg: Float): String =
    if (kg % 1f == 0f) kg.toInt().toString() else kg.toString()

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
internal fun previousCellText(measure: ExerciseMeasure, previous: ExerciseSet?): String {
    if (previous == null) return NO_PREVIOUS

    return when (measure) {
        ExerciseMeasure.WEIGHT_AND_REPS -> {
            val reps = previous.actualReps ?: return NO_PREVIOUS
            val weight = previous.actualWeightKg
            if (weight == null) "$reps" else "${formatWeight(weight)}kg × $reps"
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
