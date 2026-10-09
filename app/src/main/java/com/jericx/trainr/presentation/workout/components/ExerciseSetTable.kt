package com.jericx.trainr.presentation.workout.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.WeightUnit
import com.jericx.trainr.presentation.common.components.core.TrainrFieldError
import com.jericx.trainr.presentation.common.components.core.TrainrSwipeToDelete
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.scaledWithText
import com.jericx.trainr.presentation.common.theme.trainrColors

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
    units: UnitSystem = UnitSystem.Default,
    isReadOnly: Boolean = false
) {
    val showPrevious = previousSets.isNotEmpty()
    val colors = MaterialTheme.trainrColors
    val checkSize = CheckSize.scaledWithText()
    val setColumnWidth = SetColumnWidth.scaledWithText()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        // Headings hide when there are no rows; the Add set button below is not
        // tied to them either, so deleting the last set leaves a way back.
        if (sets.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColumnLabel(stringResource(R.string.set_column), Modifier.width(setColumnWidth))

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

                Box(modifier = Modifier.size(checkSize))
            }
        }

        sets.forEach { set ->
            key(set.id, set.setNumber) {
                val row = @Composable {
                    SetRow(
                        measure = measure,
                        set = set,
                        checkSize = checkSize,
                        setColumnWidth = setColumnWidth,
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
                        units = units,
                        isReadOnly = isReadOnly
                    )
                }

                DeletableRow(onDelete = { onDeleteSet(set) }, enabled = !isReadOnly) { row() }
            }
        }

        if (!isReadOnly) {
            Text(
                text = stringResource(R.string.add_set),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .border(1.dp, colors.outlineControl, MaterialTheme.shapes.medium)
                    .clickable(role = Role.Button, onClick = onAddSet)
                    .padding(vertical = Spacing.small)
            )
        }
    }
}

@Composable
private fun SetRow(
    measure: ExerciseMeasure,
    set: ExerciseSet,
    checkSize: Dp,
    setColumnWidth: Dp,
    previousText: String?,
    onSetChanged: (ExerciseSet) -> Unit,
    units: UnitSystem,
    isReadOnly: Boolean
) {
    val colors = MaterialTheme.trainrColors
    var outOfRange by remember { mutableStateOf<String?>(null) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = set.setNumber.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(setColumnWidth)
            )

            if (previousText != null) {
                Text(
                    text = previousText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }

            if (measure == ExerciseMeasure.WEIGHT_AND_REPS) {
                NumberCell(
                    value = set.actualWeightKg?.let { formatWeight(it, units) },
                    placeholder = set.targetWeightKg?.let { formatWeight(it, units) },
                    keyboard = KeyboardType.Decimal,
                    rangeMessage = stringResource(
                        R.string.value_range_hint,
                        stringResource(R.string.weight_label),
                        LOWEST_WEIGHT.toString(),
                        "${maxWeightFor(units)} " + stringResource(
                            if (units == UnitSystem.IMPERIAL) R.string.weight_column_lbs
                            else R.string.weight_column
                        )
                    ),
                    check = { weightEntry(it, units) },
                    onValueChange = { entered ->
                        onSetChanged(
                            set.copy(
                                actualWeightKg = entered?.toFloatOrNull()
                                    ?.let { WeightUnit.toKilograms(it, units) }
                            )
                        )
                    },
                    onOutOfRange = { outOfRange = it },
                    isReadOnly = isReadOnly,
                    modifier = Modifier.weight(1f)
                )
            }

            when (measure) {
                ExerciseMeasure.DURATION -> DurationCell(
                    seconds = set.actualSeconds,
                    placeholderSeconds = set.targetSeconds,
                    rangeMessage = stringResource(
                        R.string.value_range_hint,
                        stringResource(R.string.time_column),
                        formatSeconds(LOWEST_SECONDS),
                        formatSeconds(MAX_SECONDS)
                    ),
                    onSecondsChange = { onSetChanged(set.copy(actualSeconds = it)) },
                    onOutOfRange = { outOfRange = it },
                    isReadOnly = isReadOnly,
                    modifier = Modifier.weight(1f)
                )

                else -> NumberCell(
                    value = set.actualReps?.toString(),
                    placeholder = set.targetReps?.toString(),
                    keyboard = KeyboardType.Number,
                    rangeMessage = stringResource(
                        R.string.value_range_hint,
                        stringResource(R.string.reps_column),
                        LOWEST_REPS.toString(),
                        MAX_REPS.toString()
                    ),
                    check = ::repsEntry,
                    onValueChange = { onSetChanged(set.copy(actualReps = it?.toIntOrNull())) },
                    onOutOfRange = { outOfRange = it },
                    isReadOnly = isReadOnly,
                    modifier = Modifier.weight(1f)
                )
            }

            Icon(
                painter = painterResource(
                    if (set.isCompleted) R.drawable.ic_check_box else R.drawable.ic_check_box_blank
                ),
                contentDescription = stringResource(
                    when {
                        isReadOnly && set.isCompleted -> R.string.completed
                        isReadOnly -> R.string.not_completed
                        set.isCompleted -> R.string.mark_set_incomplete
                        else -> R.string.mark_set_complete
                    }
                ),
                tint = if (set.isCompleted) colors.statusDoneInk else colors.outlineControl,
                modifier = Modifier
                    .size(checkSize)
                    .then(
                        if (isReadOnly) {
                            Modifier
                        } else {
                            Modifier.clickable {
                                onSetChanged(set.copy(isCompleted = !set.isCompleted))
                            }
                        }
                    )
            )
        }

        TrainrFieldError(message = outOfRange)
    }
}

@Composable
private fun ColumnLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.trainrColors.onSurfaceMuted,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

// A record shows what was logged and keeps the prescription muted behind it,
// the same way the field it replaces reads.
@Composable
private fun LoggedCell(
    value: String?,
    placeholder: String?,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.trainrColors
    Box(
        modifier = modifier
            .padding(horizontal = Spacing.extraSmall)
            .heightIn(min = RowHeight),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value ?: placeholder.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (value == null) colors.onSurfaceMuted else colors.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

// The target is the placeholder, not the value: an untouched row claims nothing.
@Composable
private fun NumberCell(
    value: String?,
    placeholder: String?,
    keyboard: KeyboardType,
    rangeMessage: String,
    check: (String) -> NumberEntry,
    onValueChange: (String?) -> Unit,
    onOutOfRange: (String?) -> Unit,
    isReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    if (isReadOnly) {
        LoggedCell(value = value, placeholder = placeholder, modifier = modifier)
        return
    }

    val state = rememberTextFieldState(value.orEmpty())
    val stored by rememberUpdatedState(value)
    val report by rememberUpdatedState(onValueChange)

    // The same number written another way is not a correction: "22." and "22.50"
    // both hold 22.5, and rewriting the field would eat the keystroke after them.
    LaunchedEffect(value) {
        if (value?.toFloatOrNull() != state.text.toString().toFloatOrNull()) {
            state.setTextAndPlaceCursorAtEnd(value.orEmpty())
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { typed ->
            if (typed != stored.orEmpty()) report(typed.ifBlank { null })
        }
    }

    Box(
        modifier = modifier.padding(horizontal = Spacing.extraSmall),
        contentAlignment = Alignment.Center
    ) {
        NumberField(
            state = state,
            transformation = refusing(check, rangeMessage, onOutOfRange),
            keyboard = keyboard,
            placeholder = placeholder
        )
    }
}

// Rows are keyed to their set above, so a renumbered survivor cannot inherit a
// dismissed state and delete again.
@Composable
private fun DeletableRow(
    onDelete: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    TrainrSwipeToDelete(
        onDelete = onDelete,
        contentDescription = stringResource(R.string.delete_set),
        enabled = enabled
    ) {
        content()
    }
}

// Digits fill in from the seconds end ("500" is 5:00), shown as m:ss, stored
// as seconds.
@Composable
private fun DurationCell(
    seconds: Int?,
    placeholderSeconds: Int?,
    rangeMessage: String,
    onSecondsChange: (Int?) -> Unit,
    onOutOfRange: (String?) -> Unit,
    isReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    if (isReadOnly) {
        LoggedCell(
            value = seconds?.let(::formatSeconds),
            placeholder = placeholderSeconds?.let(::formatSeconds),
            modifier = modifier
        )
        return
    }

    val state = rememberTextFieldState(seconds?.let(::durationDigits).orEmpty())
    val stored by rememberUpdatedState(seconds)
    val report by rememberUpdatedState(onSecondsChange)

    LaunchedEffect(seconds) {
        if (secondsFromDigits(state.text.toString()) != seconds) {
            state.setTextAndPlaceCursorAtEnd(seconds?.let(::durationDigits).orEmpty())
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { digits ->
            val typed = secondsFromDigits(digits)
            if (typed != stored) report(typed)
        }
    }

    Box(
        modifier = modifier.padding(horizontal = Spacing.extraSmall),
        contentAlignment = Alignment.Center
    ) {
        NumberField(
            state = state,
            transformation = refusing(::durationEntry, rangeMessage, onOutOfRange),
            keyboard = KeyboardType.Number,
            placeholder = placeholderSeconds?.let(::formatSeconds),
            output = ClockFace
        )
    }
}

@Composable
private fun NumberField(
    state: TextFieldState,
    transformation: InputTransformation,
    keyboard: KeyboardType,
    placeholder: String?,
    output: OutputTransformation? = null
) {
    val colors = MaterialTheme.trainrColors
    val focusManager = LocalFocusManager.current

    BasicTextField(
        state = state,
        inputTransformation = transformation,
        outputTransformation = output,
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = colors.onSurface,
            textAlign = TextAlign.Center
        ),
        cursorBrush = SolidColor(colors.onSurface),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Done),
        onKeyboardAction = { focusManager.clearFocus() },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowHeight)
            .clip(MaterialTheme.shapes.small)
            .border(1.dp, colors.outlineControl, MaterialTheme.shapes.small),
        decorator = TextFieldDecorator { field ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (state.text.isEmpty()) {
                    Text(
                        text = placeholder.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceMuted,
                        textAlign = TextAlign.Center
                    )
                }
                field()
            }
        }
    )
}

@Composable
private fun refusing(
    check: (String) -> NumberEntry,
    rangeMessage: String,
    onOutOfRange: (String?) -> Unit
): InputTransformation {
    val accepts by rememberUpdatedState(check)
    val message by rememberUpdatedState(rangeMessage)
    val report by rememberUpdatedState(onOutOfRange)

    return remember {
        InputTransformation.byValue { current, proposed ->
            val held = holding(current.toString(), proposed.toString(), accepts)
            report(message.takeIf { held.saysRange })
            held.text
        }
    }
}

internal data class Held(val text: String, val saysRange: Boolean)

// A keystroke turned away for its size has to say so or it is swallowed in
// silence, and a cell left holding too big a number keeps saying so until the
// number is back in range. Any keystroke that is taken has been answered.
internal fun holding(
    current: String,
    proposed: String,
    check: (String) -> NumberEntry
): Held {
    val entry = check(proposed)
    val text = when (entry) {
        NumberEntry.ACCEPTED -> proposed
        NumberEntry.OUT_OF_RANGE ->
            if (movesTowardRange(current, proposed, check)) proposed else current

        NumberEntry.MALFORMED -> current
    }
    val saysRange = entry == NumberEntry.OUT_OF_RANGE || check(text) == NumberEntry.OUT_OF_RANGE
    return Held(text, saysRange)
}

// A number stored above the maximum has to be correctable: every step down from
// it is out of range too, so a smaller one is let through.
internal fun movesTowardRange(
    current: String,
    proposed: String,
    check: (String) -> NumberEntry
): Boolean = check(current) == NumberEntry.OUT_OF_RANGE &&
    (proposed.toFloatOrNull() ?: 0f) < (current.toFloatOrNull() ?: 0f)

private val ClockFace = OutputTransformation {
    when (length) {
        0 -> Unit
        1 -> insert(0, "0:0")
        2 -> insert(0, "0:")
        else -> insert(length - 2, ":")
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

internal enum class NumberEntry { ACCEPTED, MALFORMED, OUT_OF_RANGE }

internal const val MAX_REPS = 999
internal const val MAX_WEIGHT_KG = 1000
internal const val MAX_WEIGHT_LB = 2205
internal const val MAX_SECONDS = 5999

private const val LOWEST_REPS = 0
private const val LOWEST_WEIGHT = 0
private const val LOWEST_SECONDS = 1
private const val MAX_DURATION_DIGITS = 4
private const val MAX_FACE_SECONDS = 59

private val REPS_SHAPE = Regex("""\d{1,6}""")
private val WEIGHT_SHAPE = Regex("""\d{0,6}(\.\d{0,2})?""")

internal fun maxWeightFor(units: UnitSystem): Int =
    if (units == UnitSystem.IMPERIAL) MAX_WEIGHT_LB else MAX_WEIGHT_KG

internal fun repsEntry(text: String): NumberEntry = when {
    text.isEmpty() -> NumberEntry.ACCEPTED
    !REPS_SHAPE.matches(text) -> NumberEntry.MALFORMED
    text.toInt() > MAX_REPS -> NumberEntry.OUT_OF_RANGE
    else -> NumberEntry.ACCEPTED
}

internal fun weightEntry(text: String, units: UnitSystem): NumberEntry {
    if (text.isEmpty()) return NumberEntry.ACCEPTED
    if (!WEIGHT_SHAPE.matches(text)) return NumberEntry.MALFORMED

    val typed = text.toFloatOrNull() ?: return NumberEntry.MALFORMED
    return if (typed > maxWeightFor(units)) NumberEntry.OUT_OF_RANGE else NumberEntry.ACCEPTED
}

// A buffer is only a finished face once it is full, and until then it is still
// taking digits: "63" is the 63 seconds the cell holds, and 16:40 is typed
// through a "164" that reads as 1:64 for one keystroke.
internal fun durationEntry(digits: String): NumberEntry = when {
    digits.isEmpty() -> NumberEntry.ACCEPTED
    !digits.all { it.isDigit() } -> NumberEntry.MALFORMED
    digits.length > MAX_DURATION_DIGITS -> NumberEntry.OUT_OF_RANGE
    digits.startsWith("0") -> NumberEntry.MALFORMED
    digits.length == MAX_DURATION_DIGITS &&
        digits.takeLast(2).toInt() > MAX_FACE_SECONDS -> NumberEntry.MALFORMED

    (secondsFromDigits(digits) ?: 0) > MAX_SECONDS -> NumberEntry.OUT_OF_RANGE
    else -> NumberEntry.ACCEPTED
}

private const val SECONDS_PER_MINUTE = 60

private const val NO_PREVIOUS = "—"

// Actual values only: a set prescribed but never logged shows a dash, not its
// target.
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
