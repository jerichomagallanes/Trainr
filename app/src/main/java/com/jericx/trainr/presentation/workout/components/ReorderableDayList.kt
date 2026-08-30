package com.jericx.trainr.presentation.workout.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.workout.WeeklyPlanDay
import com.jericx.trainr.presentation.workout.util.WorkoutDateFormatter
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

private const val LIFTED_SCALE = 1.02f
private const val LIFTED_ELEVATION = 12f

// Long-press lifts a session and drags it onto another weekday. The weekday
// belongs to the SLOT rather than the session, so the cards relabel themselves
// the moment they settle into a new one — the move is legible before the
// finger comes up. A finished session is the record of a date it was actually
// trained on, so it neither lifts nor lets anything cross it.
@Composable
fun ReorderableDayList(
    days: List<WeeklyPlanDay>,
    locale: Locale,
    onDayClick: (WorkoutDay) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val spacingPx = with(LocalDensity.current) { Spacing.medium.toPx() }
    val moveEarlier = stringResource(R.string.move_earlier)
    val moveLater = stringResource(R.string.move_later)

    // The draft permutation lives here while a card is in the air; the plan is
    // only told once the finger lifts.
    var order by remember(days) { mutableStateOf(days.indices.toList()) }
    var dragStartPosition by remember(days) { mutableIntStateOf(0) }
    var rawOffset by remember(days) { mutableFloatStateOf(0f) }
    val heights = remember(days) { mutableStateMapOf<Int, Float>() }

    // Kept across the reordered list that a drop produces, so the card can
    // spring into its new slot instead of appearing there.
    var draggedId by remember { mutableStateOf<Long?>(null) }
    val liftOffset = remember { Animatable(0f) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        order.forEachIndexed { position, source ->
            val planDay = days[source]
            val movable = planDay.day.status != WorkoutStatus.COMPLETED
            val isDragged = draggedId == planDay.day.id

            key(planDay.day.id) {
                WorkoutDayCard(
                    weekday = WorkoutDateFormatter.formatWeekday(days[position].dateMillis, locale),
                    day = planDay.day,
                    onClick = { onDayClick(planDay.day) },
                    modifier = Modifier
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            if (isDragged) {
                                translationY = liftOffset.value
                                scaleX = LIFTED_SCALE
                                scaleY = LIFTED_SCALE
                                shadowElevation = LIFTED_ELEVATION
                            }
                        }
                        .onSizeChanged { heights[source] = it.height.toFloat() }
                        .semantics {
                            if (movable) {
                                customActions = listOfNotNull(
                                    CustomAccessibilityAction(moveEarlier) {
                                        onMove(position, position - 1)
                                        true
                                    }.takeIf { position > 0 },
                                    CustomAccessibilityAction(moveLater) {
                                        onMove(position, position + 1)
                                        true
                                    }.takeIf { position < days.lastIndex }
                                )
                            }
                        }
                        .then(
                            if (!movable) {
                                Modifier
                            } else {
                                Modifier.pointerInput(days) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedId = planDay.day.id
                                            dragStartPosition = order.indexOf(source)
                                            rawOffset = 0f
                                            scope.launch { liftOffset.snapTo(0f) }
                                            haptics.performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            rawOffset += amount.y
                                            rawOffset = settleIntoSlots(
                                                source = source,
                                                days = days,
                                                heights = heights,
                                                spacingPx = spacingPx,
                                                offset = rawOffset,
                                                order = order,
                                                onReorder = { order = it },
                                                onCrossed = {
                                                    haptics.performHapticFeedback(
                                                        HapticFeedbackType.TextHandleMove
                                                    )
                                                }
                                            )
                                            scope.launch { liftOffset.snapTo(rawOffset) }
                                        },
                                        onDragEnd = {
                                            val to = order.indexOf(source)
                                            if (to != dragStartPosition) onMove(dragStartPosition, to)
                                            scope.launch {
                                                liftOffset.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.75f,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                                draggedId = null
                                            }
                                        },
                                        onDragCancel = {
                                            order = days.indices.toList()
                                            scope.launch {
                                                liftOffset.animateTo(0f)
                                                draggedId = null
                                            }
                                        }
                                    )
                                }
                            }
                        )
                )
            }
        }
    }
}

// Swaps the lifted card with each neighbour it has travelled half of, and
// returns the offset left over so the card stays under the finger. Runs in a
// loop because one drag event can cross more than one card.
private fun settleIntoSlots(
    source: Int,
    days: List<WeeklyPlanDay>,
    heights: Map<Int, Float>,
    spacingPx: Float,
    offset: Float,
    order: List<Int>,
    onReorder: (List<Int>) -> Unit,
    onCrossed: () -> Unit
): Float {
    var remaining = offset
    var current = order
    while (true) {
        val from = current.indexOf(source)
        val to = if (remaining > 0) from + 1 else from - 1
        if (to !in current.indices) break
        // A finished session holds its date; nothing crosses it.
        if (days[current[to]].day.status == WorkoutStatus.COMPLETED) break

        val step = (heights[current[to]] ?: return remaining) + spacingPx
        if (abs(remaining) < step / 2) break

        current = current.toMutableList().apply { add(to, removeAt(from)) }
        remaining -= if (remaining > 0) step else -step
        onReorder(current)
        onCrossed()
    }
    return remaining
}
