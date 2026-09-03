package com.example

import android.app.Application
import com.example.data.AuraDeckDatabase
import com.example.data.AuraDeckRepository
import com.example.dsp.AudioEngineDsp
import com.example.engine.DualDeckAudioEngine
import com.example.visualizer.VisualizerEngine

class AuraDeckApp : Application() {

    lateinit var database: AuraDeckDatabase
        private set

    lateinit var repository: AuraDeckRepository
        private set

    lateinit var dspEngine: AudioEngineDsp
        private set

    lateinit var audioEngine: DualDeckAudioEngine
        private set

    lateinit var visualizerEngine: VisualizerEngine
        private set

    companion object {
        lateinit var instance: AuraDeckApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AuraDeckDatabase.getInstance(this)
        repository = AuraDeckRepository(database.auraDeckDao())
        dspEngine = AudioEngineDsp(this)
        audioEngine = DualDeckAudioEngine(this, dspEngine)
        visualizerEngine = VisualizerEngine()
    }

    override fun onTerminate() {
        audioEngine.release()
        dspEngine.releaseEffects()
        visualizerEngine.release()
        super.onTerminate()
    }
}
