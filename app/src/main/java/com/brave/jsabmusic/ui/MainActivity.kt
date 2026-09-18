package com.brave.jsabmusic.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.brave.jsabmusic.firebase.FirebaseSyncManager
import com.brave.jsabmusic.player.PlayerController
import com.brave.jsabmusic.service.PlaybackService
import com.brave.jsabmusic.ui.theme.JSABMusicTheme

/** Minimal functional entry point. UI enhancements can be restored after the build is stable. */
class MainActivity : ComponentActivity() {
    private lateinit var playerController: PlayerController
    private lateinit var firebaseSyncManager: FirebaseSyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        firebaseSyncManager = FirebaseSyncManager(applicationContext)
        playerController = PlaybackService.playerControllerInstance
            ?: PlayerController(applicationContext).also {
                PlaybackService.playerControllerInstance = it
            }

        setContent {
            JSABMusicTheme {
                val song by playerController.currentSong.collectAsState()
                val playing by playerController.isPlaying.collectAsState()

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(song?.title ?: "JSAB Music", style = MaterialTheme.typography.headlineSmall)
                    Text(song?.artist ?: "Ready to play")
                    Button(
                        onClick = playerController::togglePlay,
                        enabled = song != null
                    ) {
                        Text(if (playing) "Pause" else "Play")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        // PlaybackService owns the shared player instance.
        super.onDestroy()
    }
}
