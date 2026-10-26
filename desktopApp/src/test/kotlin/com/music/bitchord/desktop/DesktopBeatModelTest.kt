package com.music.bitchord.desktop

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.music.bitchord.playback.smart.BeatTracker
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Whether the Beat This! */
class DesktopBeatModelTest {

    @Test
    fun `the beat model loads in this build`() {
        DesktopAnalysisRuntime.ensureStarted()
        if (!DesktopAnalysisRuntime.available) {
            println("analysis library unavailable — skipping")
            return
        }
        val isMacX64 = System.getProperty("os.name").contains("Mac", ignoreCase = true) &&
            (System.getProperty("os.arch").lowercase() in setOf("x86_64", "amd64"))
        if (isMacX64) {
            println("ONNX Runtime does not provide osx-x86_64 native binaries in 1.29+ — skipping on macOS Intel")
            return
        }
        val model = File(DesktopAnalysisRuntime.modelPath(BeatTracker.MODEL_ASSET))
        assertTrue(model.isFile && model.length() > 0, "the model was not unpacked")

        val outcome = runCatching {
            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }
            OrtEnvironment.getEnvironment().createSession(model.absolutePath, options).use { session ->
                session.inputNames.first()
            }
        }
        outcome.exceptionOrNull()?.let {
            System.err.println("beat model load failed: ${it.message}")
            it.printStackTrace()
        }
        assertTrue(
            outcome.isSuccess,
            "the beat model would not load, so Automix has no meter: ${outcome.exceptionOrNull()?.message}",
        )
    }
}
