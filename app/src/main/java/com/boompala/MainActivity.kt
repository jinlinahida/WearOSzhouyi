package com.boompala

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.boompala.ui.BoompalaApp
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val videoFile = File(filesDir, "video_silk_background.mp4")
                if (!videoFile.exists() || videoFile.length() == 0L) {
                    resources.openRawResource(R.raw.video_silk_background).use { input ->
                        videoFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        setContent {
            BoompalaApp()
        }
    }
}
