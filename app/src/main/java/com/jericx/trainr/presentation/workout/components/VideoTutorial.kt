package com.jericx.trainr.presentation.workout.components

import android.view.ViewGroup
import androidx.compose.foundation.Image
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
import com.jericx.trainr.presentation.common.theme.Gray100
import com.jericx.trainr.presentation.common.theme.Slate800
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.model.YouTubeVideo
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

private val ToggleHeight = 27.dp
private val PlayerCorner = RoundedCornerShape(5.dp)

// YouTube serves 16:9; the frame's 352x174 rectangle is 2.02:1, and matching it
// would letterbox the player. Held from the moment the section opens so the
// list does not jump as the player finds its own size.
private const val VIDEO_ASPECT = 16f / 9f

@Composable
fun VideoTutorial(
    video: YouTubeVideo,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.card)
    ) {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(Gray100)
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
                color = Slate800
            )
            Image(
                painter = painterResource(R.drawable.ic_keyboard_arrow_up),
                contentDescription = null,
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
                // Load-bearing for the Play Store, not just for tidiness.
                // Registering as an observer is what pauses playback when the
                // screen stops and releases the WebView when it goes; a player
                // that outlived the screen would keep playing with the app in
                // the background, which Google treats as Device and Network
                // Abuse — an attempt to substitute for a YouTube subscription —
                // and suspends for. Verified on device: backgrounding, locking
                // the screen and navigating away each stop the audio.
                lifecycleOwner.lifecycle.addObserver(this)
                addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        // Cue, not load: cueing shows YouTube's own poster
                        // frame and its own play button, and waits to be
                        // asked. Loading would start playing the moment the
                        // section opened.
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
