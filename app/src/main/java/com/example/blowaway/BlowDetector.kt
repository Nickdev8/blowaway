package com.example.blowaway

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.abs
import kotlin.math.max

class BlowDetector(
    private val sampleRateHz: Int = 16_000,
    private val amplitudeThreshold: Int = 12_000,
    private val spikeMultiplier: Float = 3.0f,
    private val warmUpMs: Long = 500L
) {

    fun detectBlow(timeoutMs: Long): Boolean {
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minBufferSize <= 0) {
            return false
        }

        val bufferSize = max(minBufferSize, 2_048)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            return false
        }

        val audioBuffer = ShortArray(bufferSize / 2)
        val startTime = System.currentTimeMillis()
        var ambientLevel = 0f

        try {
            audioRecord.startRecording()

            while (!Thread.currentThread().isInterrupted) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= timeoutMs) {
                    return false
                }

                val samplesRead = audioRecord.read(audioBuffer, 0, audioBuffer.size)
                if (samplesRead <= 0) {
                    continue
                }

                val peakAmplitude = peakAmplitude(audioBuffer, samplesRead)
                ambientLevel = if (ambientLevel == 0f) {
                    peakAmplitude.toFloat()
                } else {
                    (ambientLevel * 0.9f) + (peakAmplitude * 0.1f)
                }

                val crossedThreshold = peakAmplitude >= amplitudeThreshold
                val spikedAboveAmbient = peakAmplitude >= ambientLevel * spikeMultiplier

                if (elapsed >= warmUpMs && crossedThreshold && spikedAboveAmbient) {
                    return true
                }
            }
        } finally {
            if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop()
            }
            audioRecord.release()
        }

        return false
    }

    private fun peakAmplitude(buffer: ShortArray, samplesRead: Int): Int {
        var peak = 0
        for (index in 0 until samplesRead) {
            peak = max(peak, abs(buffer[index].toInt()))
        }
        return peak
    }
}
