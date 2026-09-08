package com.jericx.trainr.presentation.workout.components

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors
import com.jericx.trainr.presentation.workout.model.YouTubeVideo
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

private val ToggleHeight = 27.dp
private val PlayerCorner = RoundedCornerShape(5.dp)

// YouTube serves 16:9; the frame's 2.02:1 rectangle would letterbox the player.
// Held from the moment the section opens so the list does not jump.
private const val VIDEO_ASPECT = 16f / 9f

@Composable
fun VideoTutorial(
    video: YouTubeVideo,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.trainrColors

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.card)
    ) {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(colors.surfaceSunken)
                .clickable(role = Role.Button, onClick = onToggle)
                .height(ToggleHeight)
                .padding(horizontal = Spacing.tight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    if (isExpanded) R.string.hide_video_tutorial
                    else R.string.show_video_tutorial
                ),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurface
            )
            Icon(
                painter = painterResource(R.drawable.ic_keyboard_arrow_up),
                contentDescription = null,
                tint = colors.onSurface,
                modifier = Modifier
                    .padding(start = 5.dp)
                    .size(20.dp)
                    .rotate(if (isExpanded) 0f else 180f)
            )
        }

        if (!isExpanded) return@Column

        YouTubePlayer(videoId = video.id)
    }
}

@Composable
private fun YouTubePlayer(videoId: String, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(VIDEO_ASPECT)
            .clip(PlayerCorner),
        factory = { context ->
            YouTubePlayerView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                // Observing is what pauses playback and releases the WebView; a
                // player outliving the screen keeps playing in the background,
                // which Play treats as Device and Network Abuse.
                lifecycleOwner.lifecycle.addObserver(this)
                addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        // Cue, not load: loading autoplays the moment the
                        // section opens.
                        youTubePlayer.cueVideo(videoId, 0f)
                    }
                })
            }
        },
        onRelease = { view ->
            lifecycleOwner.lifecycle.removeObserver(view)
            view.release()
        }
    )
}

@Preview(showBackground = true, heightDp = 400)
@Composable
private fun VideoTutorialPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.screen),
            modifier = Modifier.padding(Spacing.screen)
        ) {
            VideoTutorial(
                video = YouTubeVideo("kDPxFoCmb-w"),
                isExpanded = false,
                onToggle = {}
            )
            VideoTutorial(
                video = YouTubeVideo("kDPxFoCmb-w"),
                isExpanded = true,
                onToggle = {}
            )
        }
    }
}
