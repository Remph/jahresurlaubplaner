package de.remph.urlaubsplaner.planner

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * A swipe sideways steps the calendar: swiping left shows the next month or year, swiping right
 * the previous one (mirrored in a right-to-left layout). [onStep] gets 1 or -1, the same values as
 * the arrow buttons, which stay for TalkBack and for anyone who does not swipe.
 *
 * Only a movement that is mostly horizontal counts: the vertical scroll of the page and the year
 * grid's vertical swipe claim their gesture first, and a tap on a day is not a drag at all.
 */
@Composable
fun Modifier.swipeToStep(onStep: (Long) -> Unit): Modifier {
    val latest by rememberUpdatedState(onStep)
    val mirrored = LocalLayoutDirection.current == LayoutDirection.Rtl
    return pointerInput(mirrored) {
        val threshold = SwipeDistance.toPx()
        var moved = 0f
        detectHorizontalDragGestures(
            onDragStart = { moved = 0f },
            onDragEnd = {
                if (abs(moved) >= threshold) {
                    val forward = (moved < 0) != mirrored
                    latest(if (forward) 1 else -1)
                }
            },
            onHorizontalDrag = { _, delta -> moved += delta },
        )
    }
}

// Far enough that a sloppy tap or a scroll that drifts sideways does not change the month.
private val SwipeDistance = 64.dp
